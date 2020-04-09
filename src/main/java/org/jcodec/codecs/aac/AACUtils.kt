package org.jcodec.codecs.aac

import org.jcodec.codecs.mpeg4.mp4.EsdsBox
import org.jcodec.common.AudioFormat
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.BitReader.Companion.createBitReader
import org.jcodec.common.model.ChannelLabel
import org.jcodec.containers.mp4.BoxUtil.`as`
import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.SampleEntry
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object AACUtils {
    private fun getObjectType(reader: BitReader): Int {
        var objectType = reader.readNBit(5)
        if (objectType == ObjectType.AOT_ESCAPE.ordinal) objectType = 32 + reader.readNBit(6)
        return objectType
    }

    private val AAC_DEFAULT_CONFIGS = arrayOf(
            null, arrayOf(ChannelLabel.MONO), arrayOf(ChannelLabel.STEREO_LEFT, ChannelLabel.STEREO_RIGHT), arrayOf(ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT), arrayOf(ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_CENTER), arrayOf(ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT,
            ChannelLabel.REAR_RIGHT), arrayOf(ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.REAR_LEFT,
            ChannelLabel.REAR_RIGHT, ChannelLabel.LFE), arrayOf(ChannelLabel.CENTER, ChannelLabel.FRONT_LEFT, ChannelLabel.FRONT_RIGHT, ChannelLabel.SIDE_LEFT,
            ChannelLabel.SIDE_RIGHT, ChannelLabel.REAR_LEFT, ChannelLabel.REAR_RIGHT, ChannelLabel.LFE))

    fun parseAudioInfo(privData: ByteBuffer?): AACMetadata? {
        val reader = createBitReader(privData!!)
        val objectType = getObjectType(reader)
        val index = reader.readNBit(4)
        val sampleRate = if (index == 0x0f) reader.readNBit(24) else AACConts.AAC_SAMPLE_RATES[index]
        val channelConfig = reader.readNBit(4)
        if (channelConfig == 0 || channelConfig >= AAC_DEFAULT_CONFIGS.size) return null
        val channels = AAC_DEFAULT_CONFIGS[channelConfig]
        return AACMetadata(AudioFormat(sampleRate, 16, channels!!.size, true, false), channels)
    }

    @JvmStatic
    fun getMetadata(mp4a: SampleEntry): AACMetadata? {
        require("mp4a" == mp4a.fourcc) { "Not mp4a sample entry" }
        val b = getCodecPrivate(mp4a) ?: return null
        return parseAudioInfo(b)
    }

    fun getCodecPrivate(mp4a: SampleEntry?): ByteBuffer? {
        var b = findFirst(mp4a, "esds") as LeafBox?
        if (b == null) {
            b = findFirstPath(mp4a, arrayOf(null, "esds")) as LeafBox?
        }
        if (b == null) return null
        val esds = `as`(EsdsBox::class.java, b)
        return esds.streamInfo
    }

    fun streamInfoToADTS(si: ByteBuffer, crcAbsent: Boolean,
                         numAACFrames: Int, frameSize: Int): ADTSParser.Header {
        val rd = createBitReader(si.duplicate())
        val objectType = rd.readNBit(5)
        val samplingIndex = rd.readNBit(4)
        val chanConfig = rd.readNBit(4)
        val frameLengthFlag = rd.read1Bit()
        rd.read1Bit()
        val extensionFlag = rd.read1Bit()
        return ADTSParser.Header(objectType, chanConfig, if (crcAbsent) 1 else 0, numAACFrames, samplingIndex, 7 + frameSize)
    }

    class AACMetadata(val format: AudioFormat, val labels: Array<ChannelLabel>?)
}