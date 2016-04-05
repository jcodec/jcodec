package org.jcodec.filters.color;
import org.jcodec.common.model.Picture8Bit;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class CVTColorFilter {

    public void yuv422BitTObgr24(Picture8Bit yuv, ByteBuffer rgb32) {
        ByteBuffer y = ByteBuffer.wrap(yuv.getPlaneData(0));
        ByteBuffer cb = ByteBuffer.wrap(yuv.getPlaneData(1));
        ByteBuffer cr = ByteBuffer.wrap(yuv.getPlaneData(2));

        while (y.hasRemaining()) {
            int c1 = (y.get() + 112) << 2;
            int c2 = (y.get() + 112) << 2;
            int d = cb.get() << 2;
            int e = cr.get() << 2;

            rgb32.put(blue(d, c1));
            rgb32.put(green(d, e, c1));
            rgb32.put(red(e, c1));

            rgb32.put(blue(d, c2));
            rgb32.put(green(d, e, c2));
            rgb32.put(red(e, c2));
        }
    }

    private static byte blue(int d, int c) {
        int blue = (1192 * c + 2064 * d + 512) >> 10;
        blue = blue < 0 ? 0 : (blue > 1023 ? 1023 : blue);
        return (byte)((blue >> 2) & 0xff);
    }

    private static byte green(int d, int e, int c) {
        int green = (1192 * c - 400 * d - 832 * e + 512) >> 10;
        green = green < 0 ? 0 : (green > 1023 ? 1023 : green);
        return (byte)((green >> 2) & 0xff);
    }

    private static byte red(int e, int c) {
        int red = (1192 * c + 1636 * e + 512) >> 10;
        red = red < 0 ? 0 : (red > 1023 ? 1023 : red);
        return (byte)((red >> 2) & 0xff);
    }

}
