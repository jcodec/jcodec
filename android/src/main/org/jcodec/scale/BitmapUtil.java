package org.jcodec.scale;

import android.graphics.Bitmap;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;

import java.nio.IntBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BitmapUtil {
    private static ThreadLocal<BitmapUtil> inst = new ThreadLocal<BitmapUtil>();
    private int[] buffer;

    private static BitmapUtil inst() {
        BitmapUtil i = inst.get();
        if (i == null) {
            i = new BitmapUtil();
            inst.set(i);
        }
        return i;
    }

    @Deprecated
    public static Picture fromBitmap(Bitmap src) {
        return inst().fromBitmapImpl(src);
    }

    public static Picture8Bit fromBitmap8Bit(Bitmap src) {
        return inst().fromBitmap8BitImpl(src);
    }

    @Deprecated
    public static void fromBitmap(Bitmap src, Picture dst) {
        inst().fromBitmapImpl(src, dst);
    }

    public static void fromBitmap8Bit(Bitmap src, Picture8Bit dst) {
        inst().fromBitmap8BitImpl(src, dst);
    }

    @Deprecated
    public static Bitmap toBitmap(Picture pic) {
        return inst().toBitmapImpl(pic);
    }

    public static Bitmap toBitmap8Bit(Picture8Bit pic) {
        return inst().toBitmap8BitImpl(pic);
    }

    @Deprecated
    public static void toBitmap(Picture src, Bitmap dst) {
        inst().toBitmapImpl(src, dst);
    }

    public static void toBitmap8Bit(Picture8Bit src, Bitmap dst) {
        inst().toBitmap8BitImpl(src, dst);
    }

    @Deprecated
    public Picture fromBitmapImpl(Bitmap src) {
        if (src == null)
            return null;
        Picture dst = Picture.create(src.getWidth(), src.getHeight(), ColorSpace.RGB);
        fromBitmapImpl(src, dst);
        return dst;
    }

    public Picture8Bit fromBitmap8BitImpl(Bitmap src) {
        if (src == null)
            return null;
        Picture8Bit dst = Picture8Bit.create(src.getWidth(), src.getHeight(), ColorSpace.RGB);
        fromBitmap8BitImpl(src, dst);
        return dst;
    }

    @Deprecated
    public void fromBitmapImpl(Bitmap src, Picture dst) {
        int[] dstData = dst.getPlaneData(0);
        int[] packed = new int[src.getWidth() * src.getHeight()];

        src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

        for (int i = 0, srcOff = 0, dstOff = 0; i < src.getHeight(); i++) {
            for (int j = 0; j < src.getWidth(); j++, srcOff++, dstOff += 3) {
                int rgb = packed[srcOff];
                dstData[dstOff] = (rgb >> 16) & 0xff;
                dstData[dstOff + 1] = (rgb >> 8) & 0xff;
                dstData[dstOff + 2] = rgb & 0xff;
            }
        }
    }

    public void fromBitmap8BitImpl(Bitmap src, Picture8Bit dst) {
        byte[] dstData = dst.getPlaneData(0);
        int[] packed = getBuffer(src.getWidth(), src.getHeight());

        src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

        for (int i = 0, srcOff = 0, dstOff = 0; i < src.getHeight(); i++) {
            for (int j = 0; j < src.getWidth(); j++, srcOff++, dstOff += 3) {
                int rgb = packed[srcOff];
                dstData[dstOff] = (byte) (((rgb >> 16) & 0xff) - 128);
                dstData[dstOff + 1] = (byte) (((rgb >> 8) & 0xff) - 128);
                dstData[dstOff + 2] = (byte) ((rgb & 0xff) - 128);
            }
        }
    }

    @Deprecated
    public Bitmap toBitmapImpl(Picture pic) {
        if (pic == null)
            return null;
        Bitmap dst = Bitmap.createBitmap(pic.getCroppedWidth(), pic.getCroppedHeight(), Bitmap.Config.ARGB_8888);
        toBitmapImpl(pic, dst);
        return dst;
    }

    public Bitmap toBitmap8BitImpl(Picture8Bit pic) {
        if (pic == null)
            return null;
        Bitmap dst = Bitmap.createBitmap(pic.getCroppedWidth(), pic.getCroppedHeight(), Bitmap.Config.ARGB_8888);
        toBitmap8BitImpl(pic, dst);
        return dst;
    }

    @Deprecated
    public void toBitmapImpl(Picture src, Bitmap dst) {
        int[] srcData = src.getPlaneData(0);
        int[] packed = getBuffer(src.getWidth(), src.getHeight());

        for (int i = 0, dstOff = 0, srcOff = 0; i < src.getCroppedHeight(); i++) {
            for (int j = 0; j < src.getCroppedWidth(); j++, dstOff++, srcOff += 3) {
                packed[dstOff] = (255 << 24) | (srcData[srcOff + 2] << 16) | (srcData[srcOff + 1] << 8)
                        | (srcData[srcOff]);
            }
            srcOff += src.getWidth() - src.getCroppedWidth();
        }
        dst.copyPixelsFromBuffer(IntBuffer.wrap(packed));
    }

    public void toBitmap8BitImpl(Picture8Bit src, Bitmap dst) {
        byte[] srcData = src.getPlaneData(0);
        int[] packed = getBuffer(src.getWidth(), src.getHeight());

        for (int i = 0, dstOff = 0, srcOff = 0; i < src.getCroppedHeight(); i++) {
            for (int j = 0; j < src.getCroppedWidth(); j++, dstOff++, srcOff += 3) {
                packed[dstOff] = (255 << 24) | ((srcData[srcOff + 2] + 128) << 16) | ((srcData[srcOff + 1] + 128) << 8)
                        | (srcData[srcOff] + 128);
            }
            srcOff += src.getWidth() - src.getCroppedWidth();
        }
        dst.copyPixelsFromBuffer(IntBuffer.wrap(packed));
    }

    private int[] getBuffer(int width, int height) {
        if (buffer == null || buffer.length < width * height) {
            buffer = new int[width * height];
        }
        return buffer;
    }
}