package org.jcodec.scale;

import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.PictureHiBD;

public class AWTUtil {
    private static final int alphaR = 0xff;
    private static final int alphaG = 0xff;
    private static final int alphaB = 0xff;

    public static void toBufferedImage2(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        byte[] srcData = src.getPlaneData(0);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (srcData[i] + 128);
        }
    }

    public static BufferedImage toBufferedImage(Picture src) {
        if (src.getColor() == ColorSpace.RGB) {
            new RgbToBgr().transform(src, src);
        } else if (src.getColor() != ColorSpace.BGR) {
            Transform transform = ColorUtil.getTransform(src.getColor(), ColorSpace.RGB);
            if (transform == null) {
                throw new IllegalArgumentException("Unsupported input colorspace: " + src.getColor());
            }
            Picture out = Picture.create(src.getWidth(), src.getHeight(), ColorSpace.RGB);
            transform.transform(src, out);
            new RgbToBgr().transform(out, out);
            src = out;
        }
        BufferedImage dst = new BufferedImage(src.getCroppedWidth(), src.getCroppedHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        if (src.getCrop() == null)
            toBufferedImage2(src, dst);
        else
            toBufferedImageCropped(src, dst);

        return dst;
    }

    private static void toBufferedImageCropped(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        byte[] srcData = src.getPlaneData(0);
        int dstStride = dst.getWidth() * 3;
        int srcStride = src.getWidth() * 3;
        for (int line = 0, srcOff = 0, dstOff = 0; line < dst.getHeight(); line++) {
            for (int id = dstOff, is = srcOff; id < dstOff + dstStride; id += 3, is += 3) {
                data[id] = (byte) (srcData[is] + 128);
                data[id + 1] = (byte) (srcData[is + 1] + 128);
                data[id + 2] = (byte) (srcData[is + 2] + 128);
            }
            srcOff += srcStride;
            dstOff += dstStride;
        }
    }

    public static Picture fromBufferedImage(BufferedImage src, ColorSpace tgtColor) {
        Picture rgb = fromBufferedImageRGB(src);
        Transform tr = ColorUtil.getTransform(rgb.getColor(), tgtColor);
        Picture res = Picture.create(rgb.getWidth(), rgb.getHeight(), tgtColor);
        tr.transform(rgb, res);
        return res;
    }

    public static Picture fromBufferedImageRGB(BufferedImage src) {
        Picture dst = Picture.create(src.getWidth(), src.getHeight(), RGB);
        fromBufferedImage(src, dst);
        return dst;
    }

    public static void fromBufferedImage(BufferedImage src, Picture dst) {
        byte[] dstData = dst.getPlaneData(0);

        int off = 0;
        for (int i = 0; i < src.getHeight(); i++) {
            for (int j = 0; j < src.getWidth(); j++) {
                int rgb1 = src.getRGB(j, i);
                int alpha = (rgb1 >> 24) & 0xff;
                if (alpha == 0xff) {
                    dstData[off++] = (byte) (((rgb1 >> 16) & 0xff) - 128);
                    dstData[off++] = (byte) (((rgb1 >> 8) & 0xff) - 128);
                    dstData[off++] = (byte) ((rgb1 & 0xff) - 128);
                } else {
                    int nalpha = 255 - alpha;
                    dstData[off++] = (byte) (((((rgb1 >> 16) & 0xff) * alpha + alphaR * nalpha) >> 8) - 128);
                    dstData[off++] = (byte) (((((rgb1 >> 8) & 0xff) * alpha + alphaG * nalpha) >> 8) - 128);
                    dstData[off++] = (byte) ((((rgb1 & 0xff) * alpha + alphaB * nalpha) >> 8) - 128);
                }
            }
        }
    }
}
