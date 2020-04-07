package org.jcodec.containers.mp3

import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.containers.mp4.demuxer.DemuxerProbe
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.arrayOf
import kotlin.intArrayOf

/**
 * Demuxer for MPEG 1/2 audio layer 1,2,3 (MP3).
 *
 *
 * Extracts raw MPEG audio frames from the ES.
 *
 *
 * See http://mpgedit.org/mpgedit/mpeg_format/mpeghdr.htm for more detail.
 *
 * @author Stanislav Vitvitskiy
 */
class MPEGAudioDemuxer(private val ch: SeekableByteChannel) : Demuxer, DemuxerTrack {
    private val tracks: MutableList<DemuxerTrack>
    private var frameNo = 0
    private val readBuffer: ByteBuffer
    private var runningFour = 0
    private var eof = false
    private var meta: DemuxerTrackMeta? = null
    private var sampleRate = 0
    private fun extractMeta() {
        if (!validHeader(runningFour)) return
        val layer = 3 - getField(runningFour, LAYER)
        val channelCount = if (getField(runningFour, CHANNELS) == 3) 1 else 2
        val version = getField(runningFour, VERSION)
        sampleRate = freqTab[getField(runningFour, SAMPLE_RATE)] shr rateReductTab[version]
        val codecMeta = AudioCodecMeta.createAudioCodecMeta(".mp3", 16, channelCount, sampleRate,
                ByteOrder.LITTLE_ENDIAN, false, null, null)
        val codec = if (layer == 2) Codec.MP3 else if (layer == 1) Codec.MP2 else Codec.MP1
        meta = DemuxerTrackMeta(TrackType.AUDIO, codec, 0.0, null, 0, null, null, codecMeta)
    }

    @Throws(IOException::class)
    override fun close() {
        ch.close()
    }

    override fun getTracks(): List<DemuxerTrack> {
        return tracks
    }

    override fun getVideoTracks(): List<DemuxerTrack>? {
        return null
    }

    override fun getAudioTracks(): List<DemuxerTrack> {
        return tracks
    }

    @Throws(IOException::class)
    override fun nextFrame(): Packet? {
        if (eof) return null
        if (!validHeader(runningFour)) {
            eof = skipJunk()
        }
        val frameSize = calcFrameSize(runningFour)
        val frame = ByteBuffer.allocate(frameSize)
        eof = readFrame(frame)
        frame.flip()
        val pkt = Packet(frame, (frameNo * 1152).toLong(), sampleRate, 1152, frameNo.toLong(), FrameType.KEY, null, 0)
        ++frameNo
        return pkt
    }

    @Throws(IOException::class)
    private fun readMoreData() {
        readBuffer.clear()
        ch.read(readBuffer)
        readBuffer.flip()
    }

    @Throws(IOException::class)
    private fun readFrame(frame: ByteBuffer): Boolean {
        var eof = false
        while (frame.hasRemaining()) {
            frame.put((runningFour shr 24).toByte())
            runningFour = runningFour shl 8
            if (!readBuffer.hasRemaining()) readMoreData()
            if (readBuffer.hasRemaining()) runningFour = runningFour or (readBuffer.get() and 0xff) else eof = true
        }
        return eof
    }

    @Throws(IOException::class)
    private fun skipJunk(): Boolean {
        var eof = false
        var total = 0
        while (!validHeader(runningFour)) {
            if (!readBuffer.hasRemaining()) readMoreData()
            if (!readBuffer.hasRemaining()) {
                eof = true
                break
            }
            runningFour = runningFour shl 8
            runningFour = runningFour or (readBuffer.get() and 0xff)
            ++total
        }
        Logger.warn(String.format("[mp3demuxer] Skipped %d bytes of junk", total))
        return eof
    }

    override fun getMeta(): DemuxerTrackMeta {
        return meta!!
    }

    companion object {
        private fun field(off: Int, size: Int): Int {
            return (1 shl size) - 1 shl 16 or off
        }

        private const val MAX_FRAME_SIZE = 1728
        private const val MIN_FRAME_SIZE = 52
        private val CHANNELS = field(6, 2)
        private val PADDING = field(9, 1)
        private val SAMPLE_RATE = field(10, 2)
        private val BITRATE = field(12, 4)
        private val VERSION = field(19, 2)
        private val LAYER = field(17, 2)
        private val SYNC = field(21, 11)
        private const val MPEG1 = 0x3
        private const val MPEG2 = 0x2
        private const val MPEG25 = 0x0
        private val bitrateTable = arrayOf(arrayOf(intArrayOf(0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448), intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384), intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)), arrayOf(intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256), intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160), intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)))
        private val freqTab = intArrayOf(44100, 48000, 32000)
        private val rateReductTab = intArrayOf(2, 0, 1, 0)
        private fun getField(header: Int, field: Int): Int {
            return header shr (field and 0xffff) and (field shr 16)
        }

        private fun validHeader(four: Int): Boolean {
            if (getField(four, SYNC) != 0x7ff) return false
            if (getField(four, LAYER) == 0) return false
            if (getField(four, SAMPLE_RATE) == 3) return false
            return if (getField(four, BITRATE) == 0xf) false else true
        }

        private fun calcFrameSize(header: Int): Int {
            val bitrateIdx = getField(header, BITRATE)
            val layer = 3 - getField(header, LAYER)
            val version = getField(header, VERSION)
            val mpeg2 = if (version != 3) 1 else 0
            val bitRate = bitrateTable[mpeg2][layer][bitrateIdx] * 1000
            val sampleRate = freqTab[getField(header, SAMPLE_RATE)] shr rateReductTab[version]
            val padding = getField(header, PADDING)
            val lsf = if (version == MPEG25 || version == MPEG2) 1 else 0
            return when (layer) {
                0 -> (bitRate * 12 / sampleRate + padding) * 4
                1 -> bitRate * 144 / sampleRate + padding
                2 -> bitRate * 144 / (sampleRate shl lsf) + padding
                else -> bitRate * 144 / (sampleRate shl lsf) + padding
            }
        }

        /**
         * Used to auto-detect MPEG Audio (MP3) files
         *
         * @param b
         * Buffer containing a snippet of data
         * @return Score from 0 to 100
         */
        @JvmField
        val PROBE: DemuxerProbe = object : DemuxerProbe {
            override fun probe(b: ByteBuffer): Int {
                val fork = b.duplicate()
                var valid = 0
                var total = 0
                var header = fork.int
                do {
                    if (!validHeader(header)) header = skipJunkBB(header, fork)
                    val size = calcFrameSize(header)
                    if (fork.remaining() < size) break
                    ++total
                    if (size > 0) NIOUtils.skip(fork, size - 4) else header = skipJunkBB(header, fork)
                    if (fork.remaining() >= 4) {
                        header = fork.int
                        if (size >= MIN_FRAME_SIZE && size <= MAX_FRAME_SIZE && validHeader(header)) valid++
                    }
                } while (fork.remaining() >= 4)
                return 100 * valid / total
            }
        }

        private fun skipJunkBB(header: Int, fork: ByteBuffer): Int {
            var header = header
            while (!validHeader(header) && fork.hasRemaining()) {
                header = header shl 8
                header = header or (fork.get() and 0xff)
            }
            return header
        }
    }

    init {
        readBuffer = ByteBuffer.allocate(1 shl 18) // 256K
        readMoreData()
        if (readBuffer.remaining() < 4) {
            eof = true
        } else {
            runningFour = readBuffer.int
            if (!validHeader(runningFour)) {
                eof = skipJunk()
            }
            extractMeta()
        }
        tracks = ArrayList()
        tracks.add(this)
    }
}