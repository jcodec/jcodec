package org.jcodec.common;

import android.graphics.Bitmap;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.scale.BitmapUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AndroidUtil {

    private static AndroidUtil inst;
    private BitmapUtil bitmapUtil;

    public AndroidUtil(BitmapUtil bitmapUtil) {
        this.bitmapUtil = bitmapUtil;
    }

    private static AndroidUtil inst() {
        if (inst == null) {
            inst = new AndroidUtil(new BitmapUtil());
        }
        return inst;
    }

    public static Bitmap toBitmap8Bit(Picture8Bit pic) {
        return inst().toBitmap8BitImpl(pic);
    }

    public static void toBitmap8Bit(Picture8Bit pic, Bitmap out) {
        inst().toBitmap8BitImpl(pic, out);
    }

    public static Picture8Bit fromBitmap8Bit(Bitmap bitmap, ColorSpace colorSpace) {
        return inst().fromBitmap8BitImpl(bitmap, colorSpace);
    }

    public static Picture8Bit fromBitmap8Bit(Bitmap bitmap, VideoEncoder encoder) {
        return inst().fromBitmap8BitImpl(bitmap, encoder);
    }

    public static void fromBitmap8Bit(Bitmap bitmap, Picture8Bit out) {
        inst().fromBitmap8BitImpl(bitmap, out);
    }

    public Bitmap toBitmap8BitImpl(Picture8Bit pic) {
        if (pic == null)
            return null;

        Transform8Bit transform = ColorUtil.getTransform8Bit(pic.getColor(), ColorSpace.RGB);
        Picture8Bit rgb = Picture8Bit.createCropped(pic.getWidth(), pic.getHeight(), ColorSpace.RGB, pic.getCrop());
        transform.transform(pic, rgb);
        return bitmapUtil.toBitmap8BitImpl(rgb);
    }

    public void toBitmap8BitImpl(Picture8Bit pic, Bitmap out) {
        if (pic == null)
            throw new IllegalArgumentException("Input pic is null");
        if (out == null)
            throw new IllegalArgumentException("Out bitmap is null");

        Transform8Bit transform = ColorUtil.getTransform8Bit(pic.getColor(), ColorSpace.RGB);
        Picture8Bit rgb = Picture8Bit.createCropped(pic.getWidth(), pic.getHeight(), ColorSpace.RGB, pic.getCrop());
        transform.transform(pic, rgb);
        bitmapUtil.toBitmap8BitImpl(rgb, out);
    }

    public Picture8Bit fromBitmap8BitImpl(Bitmap bitmap, ColorSpace colorSpace) {
        if (bitmap == null)
            return null;
        Picture8Bit out = Picture8Bit.create(bitmap.getWidth(), bitmap.getHeight(), colorSpace);
        fromBitmap8BitImpl(bitmap, out);
        return out;
    }

    public Picture8Bit fromBitmap8BitImpl(Bitmap bitmap, VideoEncoder encoder) {
        if (bitmap == null)
            return null;

        ColorSpace selectedColorSpace = null;
        for (ColorSpace colorSpace : encoder.getSupportedColorSpaces()) {
            if (ColorUtil.getTransform8Bit(ColorSpace.RGB, colorSpace) != null) {
                selectedColorSpace = colorSpace;
                break;
            }
        }
        if (selectedColorSpace == null) {
            throw new RuntimeException("Could not find a transform to convert to a codec-supported colorspace.");
        }

        Picture8Bit out = Picture8Bit.create(bitmap.getWidth(), bitmap.getHeight(), selectedColorSpace);
        fromBitmap8BitImpl(bitmap, out);
        return out;
    }
    

    public void fromBitmap8BitImpl(Bitmap bitmap, Picture8Bit out) {
        if (bitmap == null)
            throw new IllegalArgumentException("Input pic is null");
        if (out == null)
            throw new IllegalArgumentException("Out bitmap is null");

        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            throw new RuntimeException("Unsupported bitmap config: " + bitmap.getConfig());
        }
        Picture8Bit rgb = bitmapUtil.fromBitmap8BitImpl(bitmap);

        Transform8Bit transform = ColorUtil.getTransform8Bit(ColorSpace.RGB, out.getColor());
        transform.transform(rgb, out);
    }
}