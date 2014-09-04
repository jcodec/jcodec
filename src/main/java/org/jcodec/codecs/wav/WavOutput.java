package org.jcodec.codecs.wav;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.jcodec.audio.AudioSink;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.AudioUtil;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Outputs integer samples into wav file
 * 
 * @author The JCodec project
 */
public class WavOutput implements Closeable {

    protected SeekableByteChannel out;
    protected WavHeader header;
    protected int written;
    protected AudioFormat format;

    public WavOutput(SeekableByteChannel out, AudioFormat format) throws IOException {
        this.out = out;
        this.format = format;
        header = new WavHeader(format, 0);
        header.write(out);
    }

    public void write(ByteBuffer samples) throws IOException {
        written += out.write(samples);
    }

    @Override
    public void close() throws IOException {
        out.position(0);
        new WavHeader(format, format.bytesToFrames(written)).write(out);
        NIOUtils.closeQuietly(out);
    }

    /**
     * Manages the file resource on top of WavOutput
     */
    public static class File extends WavOutput {

        public File(java.io.File f, AudioFormat format) throws IOException {
            super(NIOUtils.writableFileChannel(f), format);
        }

        @Override
        public void close() throws IOException {
            super.close();
            NIOUtils.closeQuietly(out);
        }
    }

    /**
     * Supports more high-level float and int array output on top of WavOutput
     */
    public static class Sink implements AudioSink, Closeable {
        private WavOutput out;

        public Sink(WavOutput out) {
            this.out = out;
        }

        public Sink(java.io.File f, AudioFormat format) throws IOException {
            this(new File(f, format));
        }

        public Sink(SeekableByteChannel ch, AudioFormat format) throws IOException {
            this(new WavOutput(ch, format));
        }

        @Override
        public void write(FloatBuffer data) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(out.format.samplesToBytes(data.remaining()));
            AudioUtil.fromFloat(data, out.format, buf);
            buf.flip();
            out.write(buf);
        }

        public void write(int[] data, int len) throws IOException {
            // Safety net
            len = Math.min(data.length, len);

            ByteBuffer buf = ByteBuffer.allocate(out.format.samplesToBytes(len));
            AudioUtil.fromInt(data, len, out.format, buf);
            buf.flip();
            out.write(buf);
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }
}