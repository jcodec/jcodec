package org.jcodec.common;

import android.graphics.Bitmap;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.BitmapUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

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

    public static Bitmap toBitmap(Picture pic) {
        return inst().toBitmapImpl(pic);
    }

    public static void toBitmap(Picture pic, Bitmap out) {
        inst().toBitmapImpl(pic, out);
    }

    public static Picture fromBitmap(Bitmap bitmap, ColorSpace colorSpace) {
        return inst().fromBitmapImpl(bitmap, colorSpace);
    }

    public static Picture fromBitmap(Bitmap bitmap, VideoEncoder encoder) {
        return inst().fromBitmapImpl(bitmap, encoder);
    }

    public static void fromBitmap(Bitmap bitmap, Picture out) {
        inst().fromBitmapImpl(bitmap, out);
    }

    public Bitmap toBitmapImpl(Picture pic) {
        if (pic == null)
            return null;

        Transform transform = ColorUtil.getTransform(pic.getColor(), ColorSpace.RGB);
        Picture rgb = Picture.createCropped(pic.getWidth(), pic.getHeight(), 0, ColorSpace.RGB, pic.getCrop());
        transform.transform(pic, rgb);
        return bitmapUtil.toBitmapImpl(rgb);
    }

    public void toBitmapImpl(Picture pic, Bitmap out) {
        if (pic == null)
            throw new IllegalArgumentException("Input pic is null");
        if (out == null)
            throw new IllegalArgumentException("Out bitmap is null");

        Transform transform = ColorUtil.getTransform(pic.getColor(), ColorSpace.RGB);
        Picture rgb = Picture.createCropped(pic.getWidth(), pic.getHeight(), 0, ColorSpace.RGB, pic.getCrop());
        transform.transform(pic, rgb);
        bitmapUtil.toBitmapImpl(rgb, out);
    }

    public Picture fromBitmapImpl(Bitmap bitmap, ColorSpace colorSpace) {
        if (bitmap == null)
            return null;
        Picture out = Picture.create(bitmap.getWidth(), bitmap.getHeight(), colorSpace);
        fromBitmapImpl(bitmap, out);
        return out;
    }

    public Picture fromBitmapImpl(Bitmap bitmap, VideoEncoder encoder) {
        if (bitmap == null)
            return null;

        ColorSpace selectedColorSpace = null;
        for (ColorSpace colorSpace : encoder.getSupportedColorSpaces()) {
            if (ColorUtil.getTransform(ColorSpace.RGB, colorSpace) != null) {
                selectedColorSpace = colorSpace;
                break;
            }
        }
        if (selectedColorSpace == null) {
            throw new RuntimeException("Could not find a transform to convert to a codec-supported colorspace.");
        }

        Picture out = Picture.create(bitmap.getWidth(), bitmap.getHeight(), selectedColorSpace);
        fromBitmapImpl(bitmap, out);
        return out;
    }
    

    public void fromBitmapImpl(Bitmap bitmap, Picture out) {
        if (bitmap == null)
            throw new IllegalArgumentException("Input pic is null");
        if (out == null)
            throw new IllegalArgumentException("Out bitmap is null");

        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            throw new RuntimeException("Unsupported bitmap config: " + bitmap.getConfig());
        }
        Picture rgb = bitmapUtil.fromBitmapImpl(bitmap);

        Transform transform = ColorUtil.getTransform(ColorSpace.RGB, out.getColor());
        transform.transform(rgb, out);
    }
}