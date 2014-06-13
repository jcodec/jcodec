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
 * <blockquote>
 * 
 * <pre>
 * 
 * WavInput.Adaptor inp = null;
 * WavOutput.Adaptor out = null;
 * try {
 *     inp = new WavInput.Adaptor(new File(args[0]));
 *     out = new WavOutput.Adaptor(new File(args[1]), new AudioFormat(inp.getFormat(), 44100));
 * 
 *     BiliearStreamInterpolator in = new BiliearStreamInterpolator(new Rational(44100, 48000));
 * 
 *     int[] samples = new int[1024];
 *     int[] outSamples = new int[2048];
 * 
 *     while (inp.read(samples) &gt; 0) {
 *         int outSize = in.interpolate(samples, outSamples);
 *         out.write(outSamples, outSize);
 *     }
 * 
 * } finally {
 *     inp.close();
 *     out.close();
 * }
 * </pre>
 * 
 * </blockquote>
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

    public abstract int interpolate(int[] in, int[] out);
}
