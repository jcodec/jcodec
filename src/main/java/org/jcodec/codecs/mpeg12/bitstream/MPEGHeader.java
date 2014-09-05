package org.jcodec.codecs.mpeg12.bitstream;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface MPEGHeader {

    void write(ByteBuffer bb);

}
