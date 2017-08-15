package org.jcodec.scale;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import static org.jcodec.common.model.ColorSpace.RGB;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class AWTUtil {
    public static BufferedImage toBufferedImage(Picture src) {
		if (src.getColor() != ColorSpace.BGR) {
			Picture bgr = Picture.createCropped(src.getWidth(), src.getHeight(), ColorSpace.BGR, src.getCrop());
			if (src.getColor() == ColorSpace.RGB) {
				new RgbToBgr().transform(src, bgr);
			} else {
				Transform transform = ColorUtil.getTransform(src.getColor(), ColorSpace.RGB);
				transform.transform(src, bgr);
				new RgbToBgr().transform(bgr, bgr);				
			}
			src = bgr;
		}

        BufferedImage dst = new BufferedImage(src.getCroppedWidth(), src.getCroppedHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        if (src.getCrop() == null)
            toBufferedImage(src, dst);
        else
            toBufferedImageCropped(src, dst);

        return dst;
    }

    public static BufferedImage toBufferedImage(Picture src, DemuxerTrackMeta.Orientation orientation) {
    	if (src.getColor() != ColorSpace.BGR) {
			Picture bgr = Picture.createCropped(src.getWidth(), src.getHeight(), ColorSpace.BGR, src.getCrop());
			if (src.getColor() == ColorSpace.RGB) {
				new RgbToBgr().transform(src, bgr);
			} else {
				Transform transform = ColorUtil.getTransform(src.getColor(), ColorSpace.RGB);
				transform.transform(src, bgr);
				new RgbToBgr().transform(bgr, bgr);				
			}
			src = bgr;
		}

        BufferedImage dst = new BufferedImage(src.getCroppedWidth(), src.getCroppedHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        if (src.getCrop() == null)
            toBufferedImage(src, dst);
        else
            toBufferedImageCropped(src, dst);

        if (orientation.equals(DemuxerTrackMeta.Orientation.D_90))
            return rotate90ToRight(dst);
        else if (orientation.equals(DemuxerTrackMeta.Orientation.D_180))
            return rotate180(dst);
        else if (orientation.equals(DemuxerTrackMeta.Orientation.D_270))
            return rotate90ToLeft(dst);

        return dst;
    }

    public static BufferedImage rotate90ToRight( BufferedImage inputImage ) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage returnImage = new BufferedImage( height, width , inputImage.getType() );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                returnImage.setRGB( height - y - 1, x, inputImage.getRGB(x, y) );
            }
        }
        return returnImage;
    }

    public static BufferedImage rotate180( BufferedImage inputImage ) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage returnImage = new BufferedImage( width, height, inputImage.getType() );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                returnImage.setRGB( width - x - 1, height - y - 1, inputImage.getRGB(x, y) );
            }
        }
        return returnImage;
    }

    public static BufferedImage rotate90ToLeft( BufferedImage inputImage ) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage returnImage = new BufferedImage( height, width , inputImage.getType() );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                returnImage.setRGB( y, width - x - 1, inputImage.getRGB( x, y ) );
            }
        }
        return returnImage;
    }

    private static void toBufferedImageCropped(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        byte[] srcData = src.getPlaneData(0);
        int dstStride = dst.getWidth() * 3;
        int srcStride = src.getWidth() * 3;
        for (int line = 0, srcOff = 0, dstOff = 0; line < dst.getHeight(); line++) {
            for (int id = dstOff, is = srcOff; id < dstOff + dstStride; id += 3, is += 3) {
                // Unshifting, since JCodec stores [0..255] -> [-128, 127]
                data[id] = (byte) (srcData[is] + 128);
                data[id + 1] = (byte) (srcData[is + 1] + 128);
                data[id + 2] = (byte) (srcData[is + 2] + 128);
            }
            srcOff += srcStride;
            dstOff += dstStride;
        }
    }

    public static void toBufferedImage(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        byte[] srcData = src.getPlaneData(0);
        for (int i = 0; i < data.length; i++) {
            // Unshifting, since JCodec stores [0..255] -> [-128, 127]
            data[i] = (byte) (srcData[i] + 128);
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
                dstData[off++] = (byte) (((rgb1 >> 16) & 0xff) - 128);
                dstData[off++] = (byte) (((rgb1 >> 8) & 0xff) - 128);
                dstData[off++] = (byte) ((rgb1 & 0xff) - 128);
            }
        }
    }

    public static void savePicture(Picture pic, String format, File file) throws IOException {
        ImageIO.write(toBufferedImage(pic), format, file);
    }
}
