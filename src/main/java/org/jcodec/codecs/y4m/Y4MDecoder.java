package org.jcodec.codecs.y4m;

import static org.jcodec.common.StringUtils.split;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Y4MDecoder {

    private FileChannel is;
    private int width;
    private int height;
    private String invalidFormat;
    private Rational fps;
    private int bufSize;

    public Y4MDecoder(SeekableByteChannel is) throws IOException {
        ByteBuffer buf = NIOUtils.fetchFrom(is, 2048);
        String[] header = split(readLine(buf), ' ');

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
            String[] numden = split(fpsStr, ':');
            fps = new Rational(Integer.parseInt(numden[0]), Integer.parseInt(numden[1]));
        }

        is.position(buf.position());
        bufSize = width * height * 2;
    }

    public Picture nextFrame(int[][] buffer) throws IOException {
        if (invalidFormat != null)
            throw new RuntimeException("Invalid input: " + invalidFormat);
        long pos = is.position();
        ByteBuffer buf = NIOUtils.fetchFrom(is, 2048);
        String frame = readLine(buf);
        if (frame == null || !frame.startsWith("FRAME"))
            return null;

        MappedByteBuffer pix = is.map(MapMode.READ_ONLY, pos + buf.position(), bufSize);
        is.position(pos + buf.position() + bufSize);

        Picture create = Picture.create(width, height, ColorSpace.YUV420);
        copy(pix, create.getPlaneData(0));
        copy(pix, create.getPlaneData(1));
        copy(pix, create.getPlaneData(2));

        return create;
    }

    void copy(ByteBuffer b, int[] ii) {
        for (int i = 0; b.hasRemaining(); i++) {
            ii[i] = b.get() & 0xff;
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
}