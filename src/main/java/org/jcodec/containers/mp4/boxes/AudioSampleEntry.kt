package org.jcodec.containers.mp4.boxes

import org.jcodec.api.NotSupportedException
import org.jcodec.common.AudioFormat
import org.jcodec.common.model.ChannelLabel
import org.jcodec.common.model.Label
import org.jcodec.containers.mp4.boxes.ChannelBox.ChannelDescription
import org.jcodec.containers.mp4.boxes.channel.ChannelLayout
import org.jcodec.platform.Platform
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Describes audio payload sample
 *
 * @author The JCodec project
 */
class AudioSampleEntry(atom: Header) : SampleEntry(atom) {
    var channelCount: Short = 0
        private set
    var sampleSize: Short = 0
        private set
    var sampleRate = 0f
        private set
    private var revision: Short = 0
    private var vendor = 0
    private var compressionId = 0
    private var pktSize = 0
    private var samplesPerPkt = 0
    private var bytesPerPkt = 0
    var bytesPerFrame = 0
        private set
    var bytesPerSample = 0
        private set
    var version: Short = 0
        private set
    private var lpcmFlags = 0
    override fun parse(input: ByteBuffer) {
        super.parse(input)
        version = input.short
        revision = input.short
        vendor = input.int
        channelCount = input.short
        sampleSize = input.short
        compressionId = input.short.toInt()
        pktSize = input.short.toInt()
        val sr = Platform.unsignedInt(input.int)
        sampleRate = sr.toFloat() / 65536f
        if (version.toInt() == 1) {
            samplesPerPkt = input.int
            bytesPerPkt = input.int
            bytesPerFrame = input.int
            bytesPerSample = input.int
        } else if (version.toInt() == 2) {
            input.int /* sizeof struct only */
            sampleRate = java.lang.Double.longBitsToDouble(input.long).toFloat()
            channelCount = input.int.toShort()
            input.int /* always 0x7F000000 */
            sampleSize = input.int.toShort()
            lpcmFlags = input.int
            bytesPerFrame = input.int
            samplesPerPkt = input.int
        }
        parseExtensions(input)
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        out.putShort(version)
        out.putShort(revision)
        out.putInt(vendor)
        if (version < 2) {
            out.putShort(channelCount)
            if (version.toInt() == 0) out.putShort(sampleSize) else out.putShort(16.toShort())
            out.putShort(compressionId.toShort())
            out.putShort(pktSize.toShort())
            out.putInt(Math.round(sampleRate * 65536.0).toInt())
            if (version.toInt() == 1) {
                out.putInt(samplesPerPkt)
                out.putInt(bytesPerPkt)
                out.putInt(bytesPerFrame)
                out.putInt(bytesPerSample)
            }
        } else if (version.toInt() == 2) {
            out.putShort(3.toShort())
            out.putShort(16.toShort())
            out.putShort((-2).toShort())
            out.putShort(0.toShort())
            out.putInt(65536)
            out.putInt(72)
            out.putLong(java.lang.Double.doubleToLongBits(sampleRate.toDouble()))
            out.putInt(channelCount.toInt())
            out.putInt(0x7F000000)
            out.putInt(sampleSize.toInt())
            out.putInt(lpcmFlags)
            out.putInt(bytesPerFrame)
            out.putInt(samplesPerPkt)
        }
        writeExtensions(out)
    }

    fun calcFrameSize(): Int {
        return if (version.toInt() == 0 || bytesPerFrame == 0) (sampleSize.toInt() shr 3) * channelCount else bytesPerFrame
    }

    fun calcSampleSize(): Int {
        return calcFrameSize() / channelCount
    }

    val endian: ByteOrder
        get() {
            val endianBox: EndianBox? = (NodeBox.findFirstPath(this, arrayOf(WaveExtension.fourcc(), EndianBox.fourcc()))
                    ?: return if ("twos" == header.fourcc) ByteOrder.BIG_ENDIAN else if ("lpcm" == header.fourcc) if (lpcmFlags and kAudioFormatFlagIsBigEndian != 0) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN else if ("sowt" == header.fourcc) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN) as EndianBox?
            return endianBox!!.endian!!
        }

    val isFloat: Boolean
        get() = "fl32" == header.fourcc || "fl64" == header.fourcc || "lpcm" == header.fourcc && lpcmFlags and kAudioFormatFlagIsFloat != 0

    companion object {
        //@formatter:off
        var kAudioFormatFlagIsFloat = 0x1
        var kAudioFormatFlagIsBigEndian = 0x2
        var kAudioFormatFlagIsSignedInteger = 0x4
        var kAudioFormatFlagIsPacked = 0x8
        var kAudioFormatFlagIsAlignedHigh = 0x10
        var kAudioFormatFlagIsNonInterleaved = 0x20
        var kAudioFormatFlagIsNonMixable = 0x40

        //@formatter:on
        fun createAudioSampleEntry(header: Header, drefInd: Short, channelCount: Short,
                                   sampleSize: Short, sampleRate: Int, revision: Short, vendor: Int, compressionId: Int, pktSize: Int,
                                   samplesPerPkt: Int, bytesPerPkt: Int, bytesPerFrame: Int, bytesPerSample: Int, version: Short): AudioSampleEntry {
            val audio = AudioSampleEntry(header)
            audio.drefInd = drefInd
            audio.channelCount = channelCount
            audio.sampleSize = sampleSize
            audio.sampleRate = sampleRate.toFloat()
            audio.revision = revision
            audio.vendor = vendor
            audio.compressionId = compressionId
            audio.pktSize = pktSize
            audio.samplesPerPkt = samplesPerPkt
            audio.bytesPerPkt = bytesPerPkt
            audio.bytesPerFrame = bytesPerFrame
            audio.bytesPerSample = bytesPerSample
            audio.version = version
            return audio
        }

        private val MONO = Arrays.asList(Label.Mono)
        private val STEREO = Arrays.asList(Label.Left, Label.Right)
        private val MATRIX_STEREO = Arrays.asList(Label.LeftTotal, Label.RightTotal)
        val EMPTY = arrayOfNulls<Label>(0)
        var pcms: MutableSet<String> = HashSet()

        @JvmStatic
        fun compressedAudioSampleEntry(fourcc: String?, drefId: Int, sampleSize: Int, channels: Int,
                                       sampleRate: Int, samplesPerPacket: Int, bytesPerPacket: Int, bytesPerFrame: Int): AudioSampleEntry {
            return createAudioSampleEntry(Header.createHeader(fourcc, 0), drefId.toShort(),
                    channels.toShort(), 16.toShort(), sampleRate, 0.toShort(), 0, 65534, 0, samplesPerPacket, bytesPerPacket,
                    bytesPerFrame, 16 / 8, 0.toShort())
        }

        fun audioSampleEntry(fourcc: String?, drefId: Int, sampleSize: Int, channels: Int,
                             sampleRate: Int, endian: ByteOrder?): AudioSampleEntry {
            val ase = createAudioSampleEntry(Header.createHeader(fourcc, 0), drefId.toShort(),
                    channels.toShort(), 16.toShort(), sampleRate, 0.toShort(), 0, 65535, 0, 1, sampleSize, channels * sampleSize,
                    sampleSize, 1.toShort())
            val wave = NodeBox(Header("wave"))
            ase.add(wave)
            wave.add(FormatBox.createFormatBox(fourcc))
            wave.add(EndianBox.createEndianBox(endian))
            wave.add(Box.terminatorAtom())
            // ase.add(new ChannelBox(atom));
            return ase
        }

        fun lookupFourcc(format: AudioFormat): String {
            return if (format.sampleSizeInBits == 16 && !format.isBigEndian) "sowt" else if (format.sampleSizeInBits == 24) "in24" else throw NotSupportedException("Audio format $format is not supported.")
        }

        @JvmStatic
        fun audioSampleEntryPCM(format: AudioFormat): AudioSampleEntry {
            return audioSampleEntry(lookupFourcc(format), 1, format.sampleSizeInBits shr 3,
                    format.channels, format.sampleRate,
                    if (format.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
        }

        private val translationStereo: MutableMap<Label?, ChannelLabel> = HashMap()
        private val translationSurround: MutableMap<Label?, ChannelLabel> = HashMap()
        fun getLabelsFromSampleEntry(se: AudioSampleEntry): Array<Label?> {
            val channel = NodeBox.findFirst(se, "chan") as? ChannelBox?
            return if (channel != null) getLabelsFromChan(channel) else {
                val channelCount = se.channelCount.toInt()
                when (channelCount) {
                    1 -> arrayOf(Label.Mono)
                    2 -> arrayOf(Label.Left, Label.Right)
                    3 -> arrayOf(Label.Left, Label.Right, Label.Center)
                    4 -> arrayOf(Label.Left, Label.Right, Label.LeftSurround, Label.RightSurround)
                    5 -> arrayOf(Label.Left, Label.Right, Label.Center, Label.LeftSurround, Label.RightSurround)
                    6 -> arrayOf(Label.Left, Label.Right, Label.Center, Label.LFEScreen, Label.LeftSurround,
                            Label.RightSurround)
                    else -> {
                        val res = arrayOfNulls<Label>(channelCount.toInt())
                        Arrays.fill(res, Label.Mono)
                        res
                    }
                }
            }
        }

        fun getLabelsFromTrack(trakBox: TrakBox): Array<Label?> {
            return getLabelsFromSampleEntry(trakBox.sampleEntries[0] as AudioSampleEntry)
        }

        fun setLabel(trakBox: TrakBox, channel: Int, label: Label?) {
            val labels = getLabelsFromTrack(trakBox)
            labels[channel] = label
            _setLabels(trakBox, labels)
        }

        fun _setLabels(trakBox: TrakBox?, labels: Array<Label?>) {
            var channel = NodeBox.findFirstPath(trakBox, arrayOf("mdia", "minf", "stbl", "stsd", null, "chan")) as? ChannelBox?
            if (channel == null) {
                channel = ChannelBox.createChannelBox()
                (NodeBox.findFirstPath(trakBox, arrayOf("mdia", "minf", "stbl", "stsd", null))!! as AudioSampleEntry).add(channel)
            }
            setLabels(labels, channel)
        }

        fun setLabels(labels: Array<Label?>, channel: ChannelBox?) {
            channel!!.channelLayout = ChannelLayout.kCAFChannelLayoutTag_UseChannelDescriptions.code
            val list = arrayOfNulls<ChannelDescription>(labels.size)
            for (i in labels.indices) list[i] = ChannelDescription(labels[i]!!.getVal(), 0, floatArrayOf(0f, 0f, 0f))
            channel.descriptions = list
        }

        /**
         * `
         * enum
         * {
         * kCAFChannelBit_Left                 = (1<<0),
         * kCAFChannelBit_Right                = (1<<1),
         * kCAFChannelBit_Center               = (1<<2),
         * kCAFChannelBit_LFEScreen            = (1<<3),
         * kCAFChannelBit_LeftSurround         = (1<<4),   // WAVE: "Back Left"
         * kCAFChannelBit_RightSurround        = (1<<5),   // WAVE: "Back Right"
         * kCAFChannelBit_LeftCenter           = (1<<6),
         * kCAFChannelBit_RightCenter          = (1<<7),
         * kCAFChannelBit_CenterSurround       = (1<<8),   // WAVE: "Back Center"
         * kCAFChannelBit_LeftSurroundDirect   = (1<<9),   // WAVE: "Side Left"
         * kCAFChannelBit_RightSurroundDirect  = (1<<10), // WAVE: "Side Right"
         * kCAFChannelBit_TopCenterSurround    = (1<<11),
         * kCAFChannelBit_VerticalHeightLeft   = (1<<12), // WAVE: "Top Front Left"
         * kCAFChannelBit_VerticalHeightCenter = (1<<13), // WAVE: "Top Front Center"
         * kCAFChannelBit_VerticalHeightRight  = (1<<14), // WAVE: "Top Front Right"
         * kCAFChannelBit_TopBackLeft          = (1<<15),
         * kCAFChannelBit_TopBackCenter        = (1<<16),
         * kCAFChannelBit_TopBackRight         = (1<<17)
         * };
        ` *
         *
         * @param channelBitmap
         * @return
         */
        fun getLabelsByBitmap(channelBitmap: Long): Array<Label?> {
            val result: MutableList<Label> = ArrayList()
            val values = Label.values()
            for (i in values.indices) {
                val label = values[i]
                if (label.bitmapVal and channelBitmap != 0L) result.add(label)
            }
            return result.toTypedArray()
        }

        fun getLabelsFromChan(box: ChannelBox): Array<Label?> {
            val tag = box.channelLayout.toLong()
            if (tag shr 16 == 147L) {
                val n = tag.toInt() and 0xffff
                val res = arrayOfNulls<Label>(n)
                for (i in 0 until n) res[i] = Label.getByVal(1 shl 16 or i)
                return res
            }
            val values = ChannelLayout.values()
            for (i in values.indices) {
                val layout = values[i]
                if (layout.code.toLong() == tag) {
                    return if (layout == ChannelLayout.kCAFChannelLayoutTag_UseChannelDescriptions) {
                        box.descriptions.map { it?.label }.toTypedArray()
                    } else if (layout == ChannelLayout.kCAFChannelLayoutTag_UseChannelBitmap) {
                        getLabelsByBitmap(box.channelBitmap.toLong())
                    } else {
                        layout.labels
                    }
                }
            }
            return EMPTY
        }

        init {
            pcms.add("raw ")
            pcms.add("twos")
            pcms.add("sowt")
            pcms.add("fl32")
            pcms.add("fl64")
            pcms.add("in24")
            pcms.add("in32")
            pcms.add("lpcm")
        }

        init {
            translationStereo[Label.Left] = ChannelLabel.STEREO_LEFT
            translationStereo[Label.Right] = ChannelLabel.STEREO_RIGHT
            translationStereo[Label.HeadphonesLeft] = ChannelLabel.STEREO_LEFT
            translationStereo[Label.HeadphonesRight] = ChannelLabel.STEREO_RIGHT
            translationStereo[Label.LeftTotal] = ChannelLabel.STEREO_LEFT
            translationStereo[Label.RightTotal] = ChannelLabel.STEREO_RIGHT
            translationStereo[Label.LeftWide] = ChannelLabel.STEREO_LEFT
            translationStereo[Label.RightWide] = ChannelLabel.STEREO_RIGHT
            translationSurround[Label.Left] = ChannelLabel.FRONT_LEFT
            translationSurround[Label.Right] = ChannelLabel.FRONT_RIGHT
            translationSurround[Label.LeftCenter] = ChannelLabel.FRONT_CENTER_LEFT
            translationSurround[Label.RightCenter] = ChannelLabel.FRONT_CENTER_RIGHT
            translationSurround[Label.Center] = ChannelLabel.CENTER
            translationSurround[Label.CenterSurround] = ChannelLabel.REAR_CENTER
            translationSurround[Label.CenterSurroundDirect] = ChannelLabel.REAR_CENTER
            translationSurround[Label.LeftSurround] = ChannelLabel.REAR_LEFT
            translationSurround[Label.LeftSurroundDirect] = ChannelLabel.REAR_LEFT
            translationSurround[Label.RightSurround] = ChannelLabel.REAR_RIGHT
            translationSurround[Label.RightSurroundDirect] = ChannelLabel.REAR_RIGHT
            translationSurround[Label.RearSurroundLeft] = ChannelLabel.SIDE_LEFT
            translationSurround[Label.RearSurroundRight] = ChannelLabel.SIDE_RIGHT
            translationSurround[Label.LFE2] = ChannelLabel.LFE
            translationSurround[Label.LFEScreen] = ChannelLabel.LFE
            translationSurround[Label.LeftTotal] = ChannelLabel.STEREO_LEFT
            translationSurround[Label.RightTotal] = ChannelLabel.STEREO_RIGHT
            translationSurround[Label.LeftWide] = ChannelLabel.STEREO_LEFT
            translationSurround[Label.RightWide] = ChannelLabel.STEREO_RIGHT
        }
    }

    val isPCM: Boolean
        get() = pcms.contains(header.fourcc)

    val format: AudioFormat
        get() = AudioFormat(sampleRate.toInt(), calcSampleSize() shl 3, channelCount.toInt(), true,
                endian == ByteOrder.BIG_ENDIAN)

    val labels: Array<ChannelLabel?>
        get() {
            val channelBox = NodeBox.findFirst(this, "chan") as? ChannelBox?
            return if (channelBox != null) {
                val labels = getLabelsFromChan(channelBox)
                if (channelCount.toInt() == 2) translate(translationStereo, labels) else translate(translationSurround, labels)
            } else {
                when (channelCount.toInt()) {
                    1 -> arrayOf<ChannelLabel?>(ChannelLabel.MONO)
                    2 -> arrayOf<ChannelLabel?>(ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT)
                    6 -> arrayOf<ChannelLabel?>(ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.CENTER,
                            ChannelLabel.LFE, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT)
                    else -> {
                        val lbl = arrayOfNulls<ChannelLabel>(channelCount.toInt())
                        Arrays.fill(lbl, ChannelLabel.MONO)
                        lbl
                    }
                }
            }
        }

    private fun translate(translation: Map<Label?, ChannelLabel>, labels: Array<Label?>): Array<ChannelLabel?> {
        val result = arrayOfNulls<ChannelLabel>(labels.size)
        var i = 0
        for (j in labels.indices) {
            val label = labels[j]
            result[i++] = translation[label]
        }
        return result
    }
}