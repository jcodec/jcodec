package org.jcodec.audio;
import js.io.IOException;
import js.nio.FloatBuffer;

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