package org.jcodec.codecs.y4m;

import static org.apache.commons.lang.StringUtils.split;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
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

    private DataInputStream is;
    private int width;
    private int height;
    private String invalidFormat;
    private byte[] y;
    private byte[] cb;
    private byte[] cr;
    private Rational fps;

    public Y4MDecoder(InputStream is) throws IOException {
        this.is = new DataInputStream(is);
        String[] header = split(readLine(is), ' ');

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
            String[] numden = StringUtils.split(fpsStr, ':');
            fps = new Rational(Integer.parseInt(numden[0]), Integer.parseInt(numden[1]));
        }

        y = new byte[width * height];
        cb = new byte[(width * height) >> 2];
        cr = new byte[(width * height) >> 2];
    }

    public Picture nextFrame() throws IOException {
        if (invalidFormat != null)
            throw new RuntimeException("Invalid input: " + invalidFormat);
        String frame = readLine(is);
        if (frame == null || !frame.startsWith("FRAME"))
            return null;

        is.readFully(y);
        is.readFully(cb);
        is.readFully(cr);

        Picture create = Picture.create(width, height, ColorSpace.YUV420);
        copy(y, create.getPlaneData(0));
        copy(cb, create.getPlaneData(1));
        copy(cr, create.getPlaneData(2));

        return create;
    }

    void copy(byte[] b, int[] ii) {
        for (int i = 0; i < b.length; i++) {
            ii[i] = b[i];
        }
    }

    private static String find(String[] header, char c) {
        for (String string : header) {
            if (string.charAt(0) == c)
                return string.substring(1);
        }
        return null;
    }

    private static String readLine(InputStream y4m) throws IOException {
        byte[] buf = new byte[1024];
        int ch, i = 0;
        while ((ch = y4m.read()) != -1) {
            if (ch == '\n')
                break;
            buf[i++] = (byte) ch;
        }
        return ch == -1 ? null : new String(buf, 0, i);
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