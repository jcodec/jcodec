package org.jcodec.codecs.raw;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.jcodec.common.model.ColorSpace.YUV422_10;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * The decoder for yuv 10 bit 422
 * 
 * x|x|9876543210(cr0)|9876543210(y0) |9876543210(cb0)
 * x|x|9876543210(y2) |9876543210(cb1)|9876543210(y1)
 * x|x|9876543210(cb2)|9876543210(y3) |9876543210(cr1)
 * x|x|9876543210(y5) |9876543210(cr2)|9876543210(y4) 
 * 
 * @author Jay Codec
 * 
 */
public class V210Decoder {

    private int width;
    private int height;

    public V210Decoder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Picture decode(byte[] data) {
        IntBuffer dat = ByteBuffer.wrap(data).order(LITTLE_ENDIAN).asIntBuffer();
        IntBuffer y = IntBuffer.wrap(new int[width * height]);
        IntBuffer cb = IntBuffer.wrap(new int[width * height / 2]);
        IntBuffer cr = IntBuffer.wrap(new int[width * height / 2]);

        while (dat.hasRemaining()) {
            int i = dat.get();
            cr.put(i >> 20);
            y.put((i >> 10) & 0x3ff);
            cb.put(i & 0x3ff);

            i = dat.get();
            y.put(i & 0x3ff);
            y.put(i >> 20);
            cb.put((i >> 10) & 0x3ff);

            i = dat.get();
            cb.put(i >> 20);
            y.put((i >> 10) & 0x3ff);
            cr.put(i & 0x3ff);

            i = dat.get();
            y.put(i & 0x3ff);
            y.put(i >> 20);
            cr.put((i >> 10) & 0x3ff);
        }

        return new Picture(width, height, new int[][] {y.array(), cb.array(), cr.array()}, YUV422_10);
    }
}