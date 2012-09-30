package org.jcodec.common.io;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public interface OutBits {

    void write1Bit(int bit) throws IOException;

    void writeNBit(int val, int nBits) throws IOException;

    void flush() throws IOException;
    
    int curBit();
}
