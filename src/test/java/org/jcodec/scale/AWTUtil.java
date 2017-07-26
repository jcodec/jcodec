package org.jcodec.scale;

import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

public class AWTUtil {
    private static final int alphaR = 0xff;
    private static final int alphaG = 0xff;
    private static final int alphaB = 0xff;

    public static void toBufferedImage8Bit2(Picture8Bit src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        byte[] srcData = src.getPlaneData(0);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (srcData[i] + 128);
        }
    }

    public static BufferedImage toBufferedImage8Bit(Picture8Bit src) {
        if (src.getColor() != ColorSpace.RGB) {
            Transform8Bit transform = ColorUtil.getTransform8Bit(src.getColor(), ColorSpace.RGB);
            if (transform == null) {
                throw new IllegalArgumentException("Unsupported input colorspace: " + src.getColor());
            }
            Picture8Bit out = Picture8Bit.create(src.getWidth(), src.getHeight(), ColorSpace.RGB);
            transform.transform(src, out);
            new RgbToBgr8Bit().transform(out, out);
            src = out;
        }
        BufferedImage dst = new BufferedImage(src.getCroppedWidth(), src.getCroppedHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        if (src.getCrop() == null)
            toBufferedImage8Bit2(src, dst);
        else
            toBufferedImageCropped8Bit(src, dst);

        return dst;
    }

    private static void toBufferedImageCropped8Bit(Picture8Bit src, BufferedImage dst) {
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

    public static Picture8Bit fromBufferedImage8Bit(BufferedImage src, ColorSpace tgtColor) {
        Picture8Bit rgb = fromBufferedImageRGB8Bit(src);
        Transform8Bit tr = ColorUtil.getTransform8Bit(rgb.getColor(), tgtColor);
        Picture8Bit res = Picture8Bit.create(rgb.getWidth(), rgb.getHeight(), tgtColor);
        tr.transform(rgb, res);
        return res;
    }

    public static Picture8Bit fromBufferedImageRGB8Bit(BufferedImage src) {
        Picture8Bit dst = Picture8Bit.create(src.getWidth(), src.getHeight(), RGB);
        fromBufferedImage8Bit(src, dst);
        return dst;
    }

    public static void fromBufferedImage8Bit(BufferedImage src, Picture8Bit dst) {
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
