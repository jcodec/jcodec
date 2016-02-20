package org.jcodec.scale;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ColorUtil {

    private static Map<ColorSpace, Map<ColorSpace, Transform>> map = new HashMap<ColorSpace, Map<ColorSpace, Transform>>();
    private static Map<ColorSpace, Map<ColorSpace, Transform8Bit>> map8Bit = new HashMap<ColorSpace, Map<ColorSpace, Transform8Bit>>();

    static {
        Map<ColorSpace, Transform> rgb = new HashMap<ColorSpace, Transform>();
        rgb.put(ColorSpace.RGB, new Idential());
        rgb.put(ColorSpace.YUV420, new RgbToYuv420p(0, 0));
        rgb.put(ColorSpace.YUV420J, new RgbToYuv420j());
        rgb.put(ColorSpace.YUV422, new RgbToYuv422p(0, 0));
        rgb.put(ColorSpace.YUV422_10, new RgbToYuv422p(2, 0));
        map.put(ColorSpace.RGB, rgb);

        Map<ColorSpace, Transform> yuv420 = new HashMap<ColorSpace, Transform>();
        yuv420.put(ColorSpace.YUV420, new Idential());
        yuv420.put(ColorSpace.RGB, new Yuv420pToRgb(0, 0));
        yuv420.put(ColorSpace.YUV422, new Yuv420pToYuv422p(0, 0));
        yuv420.put(ColorSpace.YUV422_10, new Yuv420pToYuv422p(0, 2));
        map.put(ColorSpace.YUV420, yuv420);

        Map<ColorSpace, Transform> yuv422 = new HashMap<ColorSpace, Transform>();
        yuv422.put(ColorSpace.YUV422, new Idential());
        yuv422.put(ColorSpace.RGB, new Yuv422pToRgb(0, 0));
        yuv422.put(ColorSpace.YUV420, new Yuv422pToYuv420p(0, 0));
        yuv422.put(ColorSpace.YUV420J, new Yuv422pToYuv420j(0, 0));
        map.put(ColorSpace.YUV422, yuv422);

        Map<ColorSpace, Transform> yuv422_10 = new HashMap<ColorSpace, Transform>();
        yuv422_10.put(ColorSpace.YUV422_10, new Idential());
        yuv422_10.put(ColorSpace.RGB, new Yuv422pToRgb(2, 0));
        yuv422_10.put(ColorSpace.YUV420, new Yuv422pToYuv420p(0, 2));
        yuv422_10.put(ColorSpace.YUV420J, new Yuv422pToYuv420j(0, 2));
        map.put(ColorSpace.YUV422_10, yuv422_10);

        Map<ColorSpace, Transform> yuv444 = new HashMap<ColorSpace, Transform>();
        yuv444.put(ColorSpace.YUV444, new Idential());
        yuv444.put(ColorSpace.RGB, new Yuv444pToRgb(0, 0));
        yuv444.put(ColorSpace.YUV420, new Yuv444pToYuv420p(0, 0));
        map.put(ColorSpace.YUV444, yuv444);

        Map<ColorSpace, Transform> yuv444_10 = new HashMap<ColorSpace, Transform>();
        yuv444_10.put(ColorSpace.YUV444_10, new Idential());
        yuv444_10.put(ColorSpace.RGB, new Yuv444pToRgb(2, 0));
        yuv444_10.put(ColorSpace.YUV420, new Yuv444pToYuv420p(0, 2));
        map.put(ColorSpace.YUV444_10, yuv444_10);

        Map<ColorSpace, Transform> yuv420j = new HashMap<ColorSpace, Transform>();
        yuv420j.put(ColorSpace.YUV420J, new Idential());
        yuv420j.put(ColorSpace.RGB, new Yuv420jToRgb());
        yuv420j.put(ColorSpace.YUV420, new Yuv420jToYuv420());
        map.put(ColorSpace.YUV420J, yuv420j);

        Map<ColorSpace, Transform> yuv422j = new HashMap<ColorSpace, Transform>();
        yuv422j.put(ColorSpace.YUV422J, new Idential());
        yuv422j.put(ColorSpace.RGB, new Yuv422jToRgb());
        yuv422j.put(ColorSpace.YUV420, new Yuv422jToYuv420p());
        yuv422j.put(ColorSpace.YUV420J, new Yuv422pToYuv420p(0, 0));
        map.put(ColorSpace.YUV422J, yuv422j);

        Map<ColorSpace, Transform> yuv444j = new HashMap<ColorSpace, Transform>();
        yuv444j.put(ColorSpace.YUV444J, new Idential());
        yuv444j.put(ColorSpace.RGB, new Yuv444jToRgb());
        yuv444j.put(ColorSpace.YUV420, new Yuv444jToYuv420p());
        yuv444j.put(ColorSpace.YUV420J, new Yuv444pToYuv420p(0, 0));
        map.put(ColorSpace.YUV444J, yuv444j);

        Map<ColorSpace, Transform8Bit> rgb8Bit = new HashMap<ColorSpace, Transform8Bit>();
        rgb8Bit.put(ColorSpace.RGB, new Idential8Bit());
        rgb8Bit.put(ColorSpace.YUV420J, new RgbToYuv420j8Bit());
        rgb8Bit.put(ColorSpace.YUV420, new RgbToYuv420p8Bit());
        map8Bit.put(ColorSpace.RGB, rgb8Bit);

        Map<ColorSpace, Transform8Bit> yuv4208Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv4208Bit.put(ColorSpace.YUV420, new Idential8Bit());
        yuv4208Bit.put(ColorSpace.YUV422, new Yuv420pToYuv422p8Bit());
        yuv4208Bit.put(ColorSpace.RGB, new Yuv420pToRgb8Bit());
        map8Bit.put(ColorSpace.YUV420, yuv4208Bit);

        Map<ColorSpace, Transform8Bit> yuv4228Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv4228Bit.put(ColorSpace.YUV422, new Idential8Bit());
        map8Bit.put(ColorSpace.YUV422, yuv4228Bit);

        Map<ColorSpace, Transform8Bit> yuv4448Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv4448Bit.put(ColorSpace.YUV444, new Idential8Bit());
        map8Bit.put(ColorSpace.YUV444, yuv4448Bit);

        Map<ColorSpace, Transform8Bit> yuv420j8Bit = new HashMap<ColorSpace, Transform8Bit>();
        yuv420j8Bit.put(ColorSpace.YUV420J, new Idential8Bit());
        yuv420j8Bit.put(ColorSpace.RGB, new Yuv420jToRgb8Bit());
        map8Bit.put(ColorSpace.YUV420J, yuv420j8Bit);
    }

    @Deprecated
    public static Transform getTransform(ColorSpace from, ColorSpace to) {
        Map<ColorSpace, Transform> map2 = map.get(from);

        return map2 == null ? null : map2.get(to);
    }

    public static Transform8Bit getTransform8Bit(ColorSpace from, ColorSpace to) {
        Map<ColorSpace, Transform8Bit> map2 = map8Bit.get(from);

        return map2 == null ? null : map2.get(to);
    }

    @Deprecated
    public static class Idential implements Transform {
        @Override
        public void transform(Picture src, Picture dst) {
            for (int i = 0; i < 3; i++)
                System.arraycopy(
                        src.getPlaneData(i),
                        0,
                        dst.getPlaneData(i),
                        0,
                        Math.min(src.getPlaneWidth(i) * src.getPlaneHeight(i),
                                dst.getPlaneWidth(i) * dst.getPlaneHeight(i)));

        }
    }

    public static class Idential8Bit implements Transform8Bit {
        @Override
        public void transform(Picture8Bit src, Picture8Bit dst) {
            for (int i = 0; i < 3; i++)
                System.arraycopy(
                        src.getPlaneData(i),
                        0,
                        dst.getPlaneData(i),
                        0,
                        Math.min(src.getPlaneWidth(i) * src.getPlaneHeight(i),
                                dst.getPlaneWidth(i) * dst.getPlaneHeight(i)));

        }
    }
}
