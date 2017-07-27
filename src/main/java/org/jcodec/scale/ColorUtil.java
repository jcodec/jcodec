package org.jcodec.scale;
import static java.lang.System.arraycopy;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ColorUtil {

    private static Map<ColorSpace, Map<ColorSpace, Transform8Bit>> map8Bit = new HashMap<ColorSpace, Map<ColorSpace, Transform8Bit>>();

    static {
        Map<ColorSpace, Transform8Bit> rgb8Bit = new HashMap<ColorSpace, Transform8Bit>();
        rgb8Bit.put(ColorSpace.RGB, new Idential8Bit());
        rgb8Bit.put(ColorSpace.YUV420J, new RgbToYuv420j8Bit());
        rgb8Bit.put(ColorSpace.YUV420, new RgbToYuv420p8Bit());
        rgb8Bit.put(ColorSpace.YUV422, new RgbToYuv422p8Bit());
        map8Bit.put(ColorSpace.RGB, rgb8Bit);

        Map<ColorSpace, Transform8Bit> yuv4208Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv4208Bit.put(ColorSpace.YUV420, new Idential8Bit());
        yuv4208Bit.put(ColorSpace.YUV422, new Yuv420pToYuv422p8Bit());
        yuv4208Bit.put(ColorSpace.RGB, new Yuv420pToRgb8Bit());
        yuv4208Bit.put(ColorSpace.YUV420J, new Idential8Bit());
        map8Bit.put(ColorSpace.YUV420, yuv4208Bit);

        Map<ColorSpace, Transform8Bit> yuv4228Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv4228Bit.put(ColorSpace.YUV422, new Idential8Bit());
        yuv4228Bit.put(ColorSpace.YUV420, new Yuv422pToYuv420p8Bit());
        yuv4228Bit.put(ColorSpace.YUV420J, new Yuv422pToYuv420p8Bit());
        yuv4228Bit.put(ColorSpace.RGB, new Yuv422pToRgb8Bit());
        map8Bit.put(ColorSpace.YUV422, yuv4228Bit);

        Map<ColorSpace, Transform8Bit> yuv4448Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv4448Bit.put(ColorSpace.YUV444, new Idential8Bit());
        map8Bit.put(ColorSpace.YUV444, yuv4448Bit);

        Map<ColorSpace, Transform8Bit> yuv420j8Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv420j8Bit.put(ColorSpace.YUV420J, new Idential8Bit());
        yuv420j8Bit.put(ColorSpace.YUV422, new Yuv420pToYuv422p8Bit());
        yuv420j8Bit.put(ColorSpace.RGB, new Yuv420jToRgb8Bit());
        yuv420j8Bit.put(ColorSpace.YUV420, new Idential8Bit());
        map8Bit.put(ColorSpace.YUV420J, yuv420j8Bit);
    }

    public static Transform8Bit getTransform8Bit(ColorSpace from, ColorSpace to) {
        Map<ColorSpace, Transform8Bit> map2 = map8Bit.get(from);

        return map2 == null ? null : map2.get(to);
    }

    public static class Idential8Bit implements Transform8Bit {
        @Override
        public void transform(Picture8Bit src, Picture8Bit dst) {
            for (int i = 0; i < Math.min(src.getData().length, dst.getData().length); i++)
                arraycopy(src.getPlaneData(i), 0, dst.getPlaneData(i), 0,
                        Math.min(src.getPlaneData(i).length, dst.getPlaneData(i).length));

        }
    }
}
