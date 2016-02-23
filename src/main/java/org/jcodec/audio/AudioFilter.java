package org.jcodec.audio;

import java.nio.FloatBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface AudioFilter {

    void filter(FloatBuffer[] _in, long[] inPos, FloatBuffer[] out);

    int getDelay();

    int getNInputs();

    int getNOutputs();
}