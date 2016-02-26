package org.jcodec.audio;

import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A class with many audio helper functions
 * 
 * @author The JCodec project
 * 
 */
public class Audio {

    public static void transfer(AudioSource src, AudioSink sink) throws IOException {
        filterTransfer(src, new DummyFilter(1), sink);
    }

    public static void filterTransfer(AudioSource src, AudioFilter filter, AudioSink sink) throws IOException {
        if (filter.getNInputs() != 1)
            throw new IllegalArgumentException("Audio filter has # inputs != 1");
        if (filter.getNOutputs() != 1)
            throw new IllegalArgumentException("Audio filter has # outputs != 1");
        if (filter.getDelay() != 0)
            throw new IllegalArgumentException("Audio filter has delay");

        FloatBuffer[] ins = new FloatBuffer[] { FloatBuffer.allocate(4096) };
        FloatBuffer[] outs = new FloatBuffer[] { FloatBuffer.allocate(8192) };
        long[] pos = new long[1];

        while (src.read(ins[0]) != -1) {
            ins[0].flip();
            filter.filter(ins, pos, outs);
            pos[0] += ins[0].position();
            rotate(ins[0]);
            outs[0].flip();
            sink.writeFloat(outs[0]);
            outs[0].clear();
        }
    }

    public static void print(FloatBuffer buf) {
        FloatBuffer dup = buf.duplicate();
        while (dup.hasRemaining())
            System.out.print(String.format("%.3f,", dup.get()));
        System.out.println();
    }

    public static void rotate(FloatBuffer buf) {
        int pos;
        for (pos = 0; buf.hasRemaining(); pos++)
            buf.put(pos, buf.get());
        buf.position(pos);
        buf.limit(buf.capacity());
    }

    public static class DummyFilter implements AudioFilter {

        private int nInputs;

        public DummyFilter(int nInputs) {
            this.nInputs = nInputs;
        }

        @Override
        public void filter(FloatBuffer[] _in, long[] inPos, FloatBuffer[] out) {
            for (int i = 0; i < _in.length; i++) {
                if (out[i].remaining() >= _in[i].remaining())
                    out[i].put(_in[i]);
                else {
                    FloatBuffer duplicate = _in[i].duplicate();
                    duplicate.limit(_in[i].position() + out[i].remaining());
                    out[i].put(duplicate);
                }

            }
        }

        @Override
        public int getDelay() {
            return 0;
        }

        @Override
        public int getNInputs() {
            return nInputs;
        }

        @Override
        public int getNOutputs() {
            return nInputs;
        }
    }
}