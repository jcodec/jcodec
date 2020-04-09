package org.jcodec.api.transcode

import org.jcodec.api.transcode.PixelStore.LoanerPicture
import org.jcodec.common.model.Packet

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class VideoFrameWithPacket(val packet: Packet, val frame: LoanerPicture?) : Comparable<VideoFrameWithPacket?> {
    override fun compareTo(other: VideoFrameWithPacket?): Int {
        return if (other == null) -1 else {
            val pts1 = packet.getPts()
            val pts2 = other.packet.getPts()
            when {
                pts1 > pts2 -> 1
                pts1 == pts2 -> 0
                else -> -1
            }
        }
    }

}