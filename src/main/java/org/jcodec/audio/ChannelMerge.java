package org.jcodec.audio;

import java.nio.FloatBuffer;

import org.jcodec.common.Assert;
import org.jcodec.common.AudioFormat;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A funnel that merges channels into multi-channel stream
 * 
 * @author The JCodec project
 * 
 */
public class ChannelMerge implements AudioFilter {
    private AudioFormat format;

    public ChannelMerge(AudioFormat format) {
        this.format = format;
    }

    @Override
    public void filter(FloatBuffer[] in, long[] inPos, FloatBuffer[] out) {
        if (in.length != format.getChannels()) {
            throw new IllegalArgumentException("Channel merge must be supplied with " + format.getChannels()
                    + " input buffers to hold the channels.");
        }

        if (out.length != 1) {
            throw new IllegalArgumentException("Channel merget invoked on more then one output");
        }

        FloatBuffer out0 = out[0];

        int min = Integer.MAX_VALUE;
        for (FloatBuffer buf : in) {
            if (buf.remaining() < min)
                min = buf.remaining();
        }
        for (FloatBuffer buf : in) {
            Assert.assertEquals(buf.remaining(), min);
        }

        if (out0.remaining() < min * in.length)
            throw new IllegalArgumentException("Supplied output buffer is not big enough to hold " + min + " * "
                    + in.length + " = " + (min * in.length) + " output samples.");

        for (int i = 0; i < min; i++) {
            for (FloatBuffer buf : in)
                out0.put(buf.get());
        }
    }

    @Override
    public int getDelay() {
        return 0;
    }

    @Override
    public int getNInputs() {
        return format.getChannels();
    }

    @Override
    public int getNOutputs() {
        return 1;
    }
}
