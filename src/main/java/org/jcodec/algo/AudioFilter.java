package org.jcodec.algo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Interpolates a sequence of signal samples into another sequence of samples
 * taken at a different frequency
 * 
 * Interpolation algorithm depends on a specific implementation
 * 
 * The input stream is supplied in chunks represented as integer arrays. The
 * output is produced in chunks as well.
 * 
 * The first sample of each chunk is the next sample to the last sample of a
 * previous chunk in the sequence.
 * 
 * The interpolator is stateful and each stream requires a separate instance.
 * 
 * @author The JCodec project
 * 
 */
public interface AudioFilter {

    int filter(float[] in, int max, float[] out);
}
