package org.jcodec.movtool.streaming;
import java.lang.IllegalStateException;
import java.lang.System;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Virtual movie frame or PCM raw packet
 * 
 * @author The JCodec project
 * 
 */
public interface VirtualPacket {
    ByteBuffer getData() throws IOException;

    int getDataLen() throws IOException;

    double getPts();

    double getDuration();

    boolean isKeyframe();

    int getFrameNo();

    Comparator<? super VirtualPacket> byPts = new Comparator<VirtualPacket>() {
        @Override
        public int compare(VirtualPacket o1, VirtualPacket o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return -1;
            else if (o2 == null)
                return 1;
            else
                return o1.getPts() < o2.getPts() ? -1 : (o1.getPts() == o2.getPts() ? 0 : 1);
        }
    };
}