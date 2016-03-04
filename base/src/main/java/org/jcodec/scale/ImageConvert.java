package org.jcodec.scale;
import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class ImageConvert {
    private static final int SCALEBITS = 10;
    private static final int ONE_HALF = (1 << (SCALEBITS - 1));

    private final static int FIX(double x) {
        return ((int) ((x) * (1 << SCALEBITS) + 0.5));
    }

    private static final int FIX_0_71414 = FIX(0.71414);
    private static final int FIX_1_772 = FIX(1.77200);
    private static final int _FIX_0_34414 = -FIX(0.34414);
    private static final int FIX_1_402 = FIX(1.40200);

    public final static int ycbcr_to_rgb24(int y, int cb, int cr) {
        y = y << SCALEBITS;
        cb = cb - 128;
        cr = cr - 128;
        int add_r = FIX_1_402 * cr + ONE_HALF;
        int add_g = _FIX_0_34414 * cb - FIX_0_71414 * cr + ONE_HALF;
        int add_b = FIX_1_772 * cb + ONE_HALF;

        int r = (y + add_r) >> SCALEBITS;
        int g = (y + add_g) >> SCALEBITS;
        int b = (y + add_b) >> SCALEBITS;
        r = crop(r);
        g = crop(g);
        b = crop(b);
        return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    private static final int CROP = 1024;

    final static int Y_JPEG_TO_CCIR(final int y) {
        return (((y) * FIX(219.0 / 255.0) + (ONE_HALF + (16 << SCALEBITS))) >> SCALEBITS);
    }

    final static int Y_CCIR_TO_JPEG(final int y) {
        return ((y) * FIX(255.0 / 219.0) + (ONE_HALF - 16 * FIX(255.0 / 219.0))) >> SCALEBITS;
    }

    private final static byte[] cropTable = new byte[CROP + 256 + CROP];
    private final static int[] intCropTable = new int[CROP + 256 + CROP];
    private final static byte[] _y_ccir_to_jpeg = new byte[256];
    private final static byte[] _y_jpeg_to_ccir = new byte[256];

    static {
        for (int i = -CROP; i < 0; i++) {
            cropTable[i + CROP] = 0;
            intCropTable[i + CROP] = 0;
        }
        for (int i = 0; i < 256; i++) {
            cropTable[i + CROP] = (byte) i;
            intCropTable[i + CROP] = i;
        }
        for (int i = 256; i < CROP; i++) {
            cropTable[i + CROP] = (byte) 255;
            intCropTable[i + CROP] = 255;
        }
        for (int i = 0; i < 256; i++) {
            _y_ccir_to_jpeg[i] = crop(Y_CCIR_TO_JPEG(i));
            _y_jpeg_to_ccir[i] = crop(Y_JPEG_TO_CCIR(i));
        }
    }

    public final static int icrop(final int i) {
        return intCropTable[i + CROP];
    }

    public final static byte crop(final int i) {
        return cropTable[i + CROP];
    }

    public final static byte y_ccir_to_jpeg(final byte y) {
        return _y_ccir_to_jpeg[(y & 0xff)];
    }

    public final static byte y_jpeg_to_ccir(final byte y) {
        return _y_jpeg_to_ccir[(y & 0xff)];
    }

    public static void YUV444toRGB888(final int y, final int u, final int v,
            final ByteBuffer rgb) {
        final int c = y - 16;
        final int d = u - 128;
        final int e = v - 128;

        final int r = (298 * c + 409 * e + 128) >> 8;
        final int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
        final int b = (298 * c + 516 * d + 128) >> 8;
        rgb.put(crop(r));
        rgb.put(crop(g));
        rgb.put(crop(b));
    }

    public static void RGB888toYUV444(final ByteBuffer rgb, final ByteBuffer Y,
            final ByteBuffer U, final ByteBuffer V) {
        final int r = rgb.get() & 0xff;
        final int g = rgb.get() & 0xff;
        final int b = rgb.get() & 0xff;
        int y = 66 * r + 129 * g + 25 * b;
        int u = -38 * r - 74 * g + 112 * b;
        int v = 112 * r - 94 * g - 18 * b;
        y = (y + 128) >> 8;
        u = (u + 128) >> 8;
        v = (v + 128) >> 8;
        Y.put(crop(y + 16));
        U.put(crop(u + 128));
        V.put(crop(v + 128));
    }

    public static byte RGB888toY4(final int r, final int g, final int b) {
        int y = 66 * r + 129 * g + 25 * b;
        y = (y + 128) >> 8;
        return crop(y + 16);
    }

    public static byte RGB888toU4(final int r, final int g, final int b) {
        int u = -38 * r - 74 * g + 112 * b;
        u = (u + 128) >> 8;
        return crop(u + 128);
    }

    public static byte RGB888toV4(final int r, final int g, final int b) {
        int v = 112 * r - 94 * g - 18 * b;
        v = (v + 128) >> 8;
        return crop(v + 128);
    }

}
