package org.jcodec.containers.webp

import org.jcodec.common.Demuxer
import org.jcodec.common.DemuxerTrack
import org.jcodec.common.DemuxerTrackMeta
import org.jcodec.common.Fourcc.dwToFourCC
import org.jcodec.common.io.DataReader
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import org.jcodec.containers.mp4.demuxer.DemuxerProbe
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 *
 * Reads integer samples from the wav file
 *
 * @author Stanislav Vitvitskiy
 */
class WebpDemuxer(channel: SeekableByteChannel?) : Demuxer, DemuxerTrack {
    private val vt = listOf(this)
    private var headerRead = false
    private val raf: DataReader = DataReader.createDataReader(channel, ByteOrder.LITTLE_ENDIAN)
    private var done = false

    @Throws(IOException::class)
    override fun close() {
        raf.close()
    }

    @Throws(IOException::class)
    override fun nextFrame(): Packet? {
        if (done) return null
        if (!headerRead) {
            readHeader()
            headerRead = true
        }
        val fourCC = raf.readInt()
        val size = raf.readInt()
        done = true
        when (fourCC) {
            FOURCC_VP8 -> {
                val b = ByteArray(size)
                raf.readFully(b)
                return Packet(ByteBuffer.wrap(b), 0, 25, 1, 0, FrameType.KEY, null, 0)
            }
            FOURCC_ICCP, FOURCC_ANIM, FOURCC_ANMF, FOURCC_XMP, FOURCC_EXIF, FOURCC_ALPH, FOURCC_VP8L, FOURCC_VP8X -> {
                Logger.warn("Skipping unsupported chunk: " + dwToFourCC(fourCC) + ".")
                val b1 = ByteArray(size)
                raf.readFully(b1)
            }
            else -> {
                Logger.warn("Skipping unsupported chunk: " + dwToFourCC(fourCC) + ".")
                val b1 = ByteArray(size)
                raf.readFully(b1)
            }
        }
        return null
    }

    @Throws(IOException::class)
    private fun readHeader() {
        if (raf.readInt() != FOURCC_RIFF) throw IOException("Invalid RIFF file.")
        val size = raf.readInt() // Size must be sane
        if (raf.readInt() != FOURCC_WEBP) throw IOException("Not a WEBP file.")
    }

    override fun getMeta(): DemuxerTrackMeta? {
        // TODO Auto-generated method stub
        return null
    }

    override fun getTracks(): List<DemuxerTrack> = vt

    override fun getVideoTracks(): List<DemuxerTrack> = vt

    override fun getAudioTracks(): List<DemuxerTrack> = emptyList()

    companion object {
        const val FOURCC_RIFF = 0x46464952 // 'RIFF'
        const val FOURCC_WEBP = 0x50424557 // 'WEBP'
        const val FOURCC_VP8 = 0x20385056 // 'VP8 '
        const val FOURCC_ICCP = 0x50434349
        const val FOURCC_ANIM = 0x4d494e41
        const val FOURCC_ANMF = 0x464d4e41
        const val FOURCC_XMP = 0x20504d58
        const val FOURCC_EXIF = 0x46495845
        const val FOURCC_ALPH = 0x48504c41
        const val FOURCC_VP8L = 0x4c385056
        const val FOURCC_VP8X = 0x58385056

        @JvmField
        val PROBE: DemuxerProbe = object : DemuxerProbe {
            override fun probe(b: ByteBuffer): Int {
                val _b = b.duplicate()
                if (_b.remaining() < 12) return 0
                _b.order(ByteOrder.LITTLE_ENDIAN)
                if (_b.int != FOURCC_RIFF) return 0
                val size = _b.int // Size must be sane
                if (_b.int != FOURCC_WEBP) return 0
                return 100
            }
        }
    }

}