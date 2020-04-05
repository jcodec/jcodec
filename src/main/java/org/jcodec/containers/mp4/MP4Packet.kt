package org.jcodec.containers.mp4

import org.jcodec.common.model.Packet
import org.jcodec.common.model.TapeTimecode
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MP4Packet(data: ByteBuffer?, pts: Long, timescale: Int, duration: Long, frameNo: Long, iframe: FrameType?,
                tapeTimecode: TapeTimecode?, displayOrder: Int, var mediaPts: Long,
                /**
                 * Zero-offset sample entry index
                 *
                 * @return
                 */
                val entryNo: Int, val fileOff: Long, val size: Int,
                val isPsync: Boolean) : Packet(data, pts, timescale, duration, frameNo, iframe, tapeTimecode, displayOrder) {

    companion object {
        fun createMP4PacketWithTimecode(other: MP4Packet, timecode: TapeTimecode?): MP4Packet {
            val frameNo = other.frameNo
            return createMP4Packet(other.data, other.pts, other.timescale, other.duration, frameNo, other.frameType,
                    timecode, other.displayOrder, other.mediaPts, other.entryNo)
        }

        fun createMP4PacketWithData(other: MP4Packet, frm: ByteBuffer?): MP4Packet {
            return createMP4Packet(frm, other.pts, other.timescale, other.duration, other.frameNo, other.frameType,
                    other.tapeTimecode, other.displayOrder, other.mediaPts, other.entryNo)
        }

        @JvmStatic
        fun createMP4Packet(data: ByteBuffer?, pts: Long, timescale: Int, duration: Long, frameNo: Long,
                            iframe: FrameType?, tapeTimecode: TapeTimecode?, displayOrder: Int, mediaPts: Long, entryNo: Int): MP4Packet {
            return MP4Packet(data, pts, timescale, duration, frameNo, iframe, tapeTimecode, displayOrder, mediaPts,
                    entryNo, 0, 0, false)
        }
    }

}