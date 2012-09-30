package org.jcodec.common.io;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface InBits {
    int read1Bit() throws IOException;

    int readNBit(int n) throws IOException;

    int checkNBit(int n) throws IOException;

    boolean moreData() throws IOException;

    int skip(int bits) throws IOException;

    int align() throws IOException;

    int curBit();

    boolean lastByte() throws IOException;
}
