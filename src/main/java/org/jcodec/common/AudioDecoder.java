package org.jcodec.common;

import java.io.IOException;

import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface AudioDecoder {
    AudioBuffer decodeFrame(Buffer frame, byte[] dst) throws IOException;
}
