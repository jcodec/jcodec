package org.jcodec.common;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface AudioEncoder {

    /**
     * Encodes audio sample data in from the provided 'audioPkt'. The encoder is
     * expected to know the bytes per sample, channel count and endian of the
     * provided sample data to be able to correctly decode the bytes of provided
     * samples.
     * 
     * @param audioPkt Raw bytes containing sample data.
     * @param buf Buffer to use as a storage for the output audio frame.
     * @return Encoded audio frame.
     */
    ByteBuffer encode(ByteBuffer audioPkt, ByteBuffer buf);

}
