package org.jcodec.codecs.wav;
import org.jcodec.audio.AudioSource;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.AudioUtil;
import org.jcodec.common.io.NIOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads integer samples from the wav file
 * 
 * @author The JCodec project
 */
public class WavInput implements Closeable {

    protected WavHeader header;
    protected byte[] prevBuf;
    protected ReadableByteChannel _in;
    protected AudioFormat format;

    public WavInput(ReadableByteChannel _in) throws IOException {
        this.header = WavHeader.readChannel(_in);
        this.format = header.getFormat();
        this._in = _in;
    }

    public int read(ByteBuffer buf) throws IOException {
        int maxRead = format.framesToBytes(format.bytesToFrames(buf.remaining()));
        return NIOUtils.readL(_in, buf, maxRead);
    }

    public void close() throws IOException {
        _in.close();
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
    public static class WavFile extends WavInput {

        public WavFile(File f) throws IOException {
            super(NIOUtils.readableChannel(f));
        }

        @Override
        public void close() throws IOException {
            super.close();
            _in.close();
        }
    }

    /**
     * Supports more high-level float and integer input on top of WavInput
     */
    public static class Source implements AudioSource, Closeable {

        private WavInput src;
        private AudioFormat format;
        private int pos;

        public Source(WavInput src) {
            this.src = src;
            this.format = src.getFormat();
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
            return format.bytesToFrames(read);
        }

        public int readFloat(FloatBuffer samples) throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(format.samplesToBytes(samples.remaining()));
            int i = src.read(bb);
            if (i == -1)
                return -1;
            bb.flip();
            AudioUtil.toFloat(format, bb, samples);
            int read = format.bytesToFrames(i);
            pos += read;

            return read;
        }
    }
}
