package org.jcodec.common;
import org.jcodec.common.model.AudioBuffer;

import js.io.IOException;
import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface AudioDecoder {
    AudioBuffer decodeFrame(ByteBuffer frame, ByteBuffer dst) throws IOException;
}
