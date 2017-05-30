package org.jcodec.api.transcode;

import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
class VideoFrameWithPacket implements Comparable<VideoFrameWithPacket> {
    private Packet packet;
    private LoanerPicture frame;

    public VideoFrameWithPacket(Packet inFrame, LoanerPicture dec2) {
        this.packet = inFrame;
        this.frame = dec2;
    }

    @Override
    public int compareTo(VideoFrameWithPacket arg) {
        if (arg == null)
            return -1;
        else {
            long pts1 = packet.getPts();
            long pts2 = arg.packet.getPts();
            return pts1 > pts2 ? 1 : (pts1 == pts2 ? 0 : -1);
        }
    }

    public Packet getPacket() {
        return packet;
    }

    public LoanerPicture getFrame() {
        return frame;
    }
}