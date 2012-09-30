package org.jcodec.algo;

import org.jcodec.common.model.Rational;

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
 * Example ( interpolates 48000 signal to 44100 ):
 * 
 * <blockquote><pre>
 * 
 * WavInput inp = null;
 * WavOutput out = null;
 * try {
 *     inp = new WavInput(new File(args[0]));
 *     out = new WavOutput(new File(args[1]), new WavHeader(inp.getHeader(), 44100));
 *     
 *     StreamInterpolator in = new CubicSplineStreamInterpolator(new Rational(44100, 48000));
 *     
 *     int[] samples;
 *     while ((samples = inp.read(1024)) != null) {
 *         int[] outSamples = in.interpolate(samples);
 *         out.write(outSamples);
 *     }
 *     
 * } finally {
 *     inp.close();
 *     out.close();
 * }
 * 
 * </blockquote></pre> 
 * 
 * 
 * @author The JCodec project
 * 
 */
public abstract class StreamInterpolator {

    protected Rational ratio;

    public StreamInterpolator(Rational ratio) {
        this.ratio = ratio;
    }

    public abstract int[] interpolate(int[] in);
}
