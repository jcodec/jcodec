package org.jcodec.movtool.streaming;

import java.io.IOException;
import java.nio.ByteBuffer;

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
}