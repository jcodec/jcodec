package org.jcodec.audio;

import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public interface AudioSink {
    
    void writeFloat(FloatBuffer buffer) throws IOException;

}