package org.jcodec.audio;
import org.jcodec.common.AudioFormat;

import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public interface AudioSource {
    AudioFormat getFormat();
    
    int readFloat(FloatBuffer buffer) throws IOException;
}
