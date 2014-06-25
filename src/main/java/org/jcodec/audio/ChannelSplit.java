package org.jcodec.audio;

import java.nio.FloatBuffer;

import org.jcodec.common.AudioFormat;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A fork that splits the channels into different streams
 * 
 * @author The JCodec project
 * 
 */
public class ChannelSplit implements AudioFilter {

    private AudioFormat format;

    public ChannelSplit(AudioFormat format) {
        this.format = format;
    }

    @Override
    public void filter(FloatBuffer[] in, long[] inPos, FloatBuffer[] out) {
        if (in.length != 1) {
            throw new IllegalArgumentException("Channel split invoked on more then one input");
        }
        if (out.length != format.getChannels()) {
            throw new IllegalArgumentException("Channel split must be supplied with " + format.getChannels()
                    + " output buffers to hold the channels.");
        }

        FloatBuffer in0 = in[0];

        int outSampleCount = in0.remaining() / out.length;
        for (int i = 0; i < out.length; i++) {
            if (out[i].remaining() < outSampleCount)
                throw new IllegalArgumentException("Supplied buffer for " + i
                        + "th channel doesn't have sufficient space to put the samples ( required: " + outSampleCount
                        + ", actual: " + out[i].remaining() + ")");
        }

        while (in0.remaining() >= format.getChannels()) {
            for (int i = 0; i < out.length; i++) {
                out[i].put(in0.get());
            }
        }
    }

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public int getNInputs() {
        return 1;
    }

    @Override
    public int getNOutputs() {
        return format.getChannels();
    }
}
