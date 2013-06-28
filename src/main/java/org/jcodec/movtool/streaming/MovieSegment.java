package org.jcodec.movtool.streaming;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A segment of a virtual movie file. Can contain a header of frame data.
 * 
 * @author The JCodec project
 * 
 */
public interface MovieSegment {
    ByteBuffer getData() throws IOException;

    int getNo();

    long getPos();

    int getDataLen() throws IOException;
}