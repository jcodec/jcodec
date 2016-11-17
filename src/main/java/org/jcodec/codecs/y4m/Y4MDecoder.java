package org.jcodec.codecs.y4m;

import static org.jcodec.common.StringUtils.splitC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Y4MDecoder extends VideoDecoder implements Demuxer {

    private SeekableByteChannel is;
    private int width;
    private int height;
    private String invalidFormat;
    private Rational fps;
    private int bufSize;

    public Y4MDecoder(SeekableByteChannel _is) throws IOException {
        this.is = _is;
        ByteBuffer buf = NIOUtils.fetchFromChannel(is, 2048);
        String[] header = splitC(readLine(buf), ' ');

        if (!"YUV4MPEG2".equals(header[0])) {
            invalidFormat = "Not yuv4mpeg stream";
            return;
        }
        String chroma = find(header, 'C');
        if (chroma != null && !chroma.startsWith("420")) {
            invalidFormat = "Only yuv420p is supported";
            return;
        }

        width = Integer.parseInt(find(header, 'W'));
        height = Integer.parseInt(find(header, 'H'));

        String fpsStr = find(header, 'F');
        if (fpsStr != null) {
            String[] numden = splitC(fpsStr, ':');
            fps = new Rational(Integer.parseInt(numden[0]), Integer.parseInt(numden[1]));
        }

        is.setPosition(buf.position());
        bufSize = width * height;
        bufSize += bufSize / 2;
    }

    public Picture8Bit nextFrame8Bit(byte[][] buffer) throws IOException {
        if (invalidFormat != null)
            throw new RuntimeException("Invalid input: " + invalidFormat);
        ByteBuffer buf = NIOUtils.fetchFromChannel(is, 2048);
        String frame = readLine(buf);
        if (frame == null || !frame.startsWith("FRAME"))
            return null;

        is.setPosition(is.position() - buf.remaining());
        ByteBuffer pix = NIOUtils.fetchFromChannel(is, bufSize);

        Picture8Bit create = Picture8Bit.createPicture8Bit(width, height, buffer, ColorSpace.YUV420);
        copy(pix, create.getPlaneData(0), width*height);
        copy(pix, create.getPlaneData(1), width*height / 4);
        copy(pix, create.getPlaneData(2), width*height / 4);

        return create;
    }

    void copy(ByteBuffer b, byte[] ii, int size) {
        for (int i = 0; b.hasRemaining() && i < size; i++) {
            ii[i] = (byte) ((b.get() & 0xff) - 128);
        }
    }

    private static String find(String[] header, char c) {
        for (String string : header) {
            if (string.charAt(0) == c)
                return string.substring(1);
        }
        return null;
    }

    private static String readLine(ByteBuffer y4m) {
        ByteBuffer duplicate = y4m.duplicate();
        while (y4m.hasRemaining() && y4m.get() != '\n')
            ;
        if (y4m.hasRemaining())
            duplicate.limit(y4m.position() - 1);
        return new String(NIOUtils.toArray(duplicate));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Rational getFps() {
        return fps;
    }

    public Size getSize() {
        return new Size(width, height);
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Picture8Bit decodeFrame8Bit(ByteBuffer data, byte[][] buffer) {
        // TODO Auto-generated method stub
        return null;
    }
}