package org.jcodec.codecs.wav;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.AudioUtil;
import org.jcodec.common.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads integer samples from the wav file
 * 
 * @author The JCodec project
 */
public class WavInput {

    protected WavHeader header;
    protected byte[] prevBuf;
    protected ReadableByteChannel in;
    protected AudioFormat format;

    public WavInput(ReadableByteChannel in) throws IOException {
        this.header = WavHeader.read(in);
        this.format = header.getFormat();
        this.in = in;
    }

    public int read(ByteBuffer buf) throws IOException {
        return NIOUtils.read(in, NIOUtils.read(buf, format.samplesToBytes(format.bytesToSamples(buf.remaining()))));
    }

    public void close() throws IOException {
        in.close();
    }

    public WavHeader getHeader() {
        return header;
    }

    public AudioFormat getFormat() {
        return format;
    }

    /**
     * Manages file resource on top of WavInput
     */
    public static class File extends WavInput {

        public File(java.io.File f) throws IOException {
            super(NIOUtils.readableFileChannel(f));
        }

        @Override
        public void close() throws IOException {
            super.close();
            in.close();
        }
    }

    /**
     * Supports more high-level float and integer input on top of WavInput
     */
    public static class Adaptor {

        private WavInput src;
        private AudioFormat format;

        public Adaptor(WavInput src) {
            this.src = src;
            this.format = src.getFormat();
        }

        public Adaptor(ReadableByteChannel ch) throws IOException {
            this(new WavInput(ch));
        }

        public Adaptor(java.io.File file) throws IOException {
            this(new WavInput.File(file));
        }

        public AudioFormat getFormat() {
            return src.getFormat();
        }

        public void close() throws IOException {
            src.close();
        }

        public int read(int[] samples, int max) throws IOException {
            // Safety net
            max = Math.min(max, samples.length);

            ByteBuffer bb = ByteBuffer.allocate(format.samplesToBytes(max));
            int read = src.read(bb);
            bb.flip();
            AudioUtil.toInt(format, bb, samples);
            return format.bytesToSamples(read);
        }

        public int read(float[] samples, int max) throws IOException {
            // Safety net
            max = Math.min(max, samples.length);

            ByteBuffer bb = ByteBuffer.allocate(format.samplesToBytes(max));
            int read = src.read(bb);
            bb.flip();
            AudioUtil.toFloat(format, bb, samples);
            return format.bytesToSamples(read);
        }

    }
}
