package org.jcodec.scale.highbd;
import static java.lang.System.arraycopy;

import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.PictureHiBD;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ColorUtilHiBD {

    private static Map<ColorSpace, Map<ColorSpace, TransformHiBD>> map = new HashMap<ColorSpace, Map<ColorSpace, TransformHiBD>>();

    static {
        Map<ColorSpace, TransformHiBD> rgb = new HashMap<ColorSpace, TransformHiBD>();
        rgb.put(ColorSpace.RGB, new Idential());
        rgb.put(ColorSpace.YUV420, new RgbToYuv420pHiBD(0, 0));
        rgb.put(ColorSpace.YUV420J, new RgbToYuv420jHiBD());
        rgb.put(ColorSpace.YUV422, new RgbToYuv422pHiBD(0, 0));
        rgb.put(ColorSpace.YUV422_10, new RgbToYuv422pHiBD(2, 0));
        map.put(ColorSpace.RGB, rgb);

        Map<ColorSpace, TransformHiBD> yuv420 = new HashMap<ColorSpace, TransformHiBD>();
        yuv420.put(ColorSpace.YUV420, new Idential());
        yuv420.put(ColorSpace.RGB, new Yuv420pToRgbHiBD(0, 0));
        yuv420.put(ColorSpace.YUV422, new Yuv420pToYuv422pHiBD(0, 0));
        yuv420.put(ColorSpace.YUV422_10, new Yuv420pToYuv422pHiBD(0, 2));
        map.put(ColorSpace.YUV420, yuv420);

        Map<ColorSpace, TransformHiBD> yuv422 = new HashMap<ColorSpace, TransformHiBD>();
        yuv422.put(ColorSpace.YUV422, new Idential());
        yuv422.put(ColorSpace.RGB, new Yuv422pToRgbHiBD(0, 0));
        yuv422.put(ColorSpace.YUV420, new Yuv422pToYuv420pHiBD(0, 0));
        yuv422.put(ColorSpace.YUV420J, new Yuv422pToYuv420jHiBD(0, 0));
        map.put(ColorSpace.YUV422, yuv422);

        Map<ColorSpace, TransformHiBD> yuv422_10 = new HashMap<ColorSpace, TransformHiBD>();
        yuv422_10.put(ColorSpace.YUV422_10, new Idential());
        yuv422_10.put(ColorSpace.RGB, new Yuv422pToRgbHiBD(2, 0));
        yuv422_10.put(ColorSpace.YUV420, new Yuv422pToYuv420pHiBD(0, 2));
        yuv422_10.put(ColorSpace.YUV420J, new Yuv422pToYuv420jHiBD(0, 2));
        map.put(ColorSpace.YUV422_10, yuv422_10);

        Map<ColorSpace, TransformHiBD> yuv444 = new HashMap<ColorSpace, TransformHiBD>();
        yuv444.put(ColorSpace.YUV444, new Idential());
        yuv444.put(ColorSpace.RGB, new Yuv444pToRgb(0, 0));
        yuv444.put(ColorSpace.YUV420, new Yuv444pToYuv420pHiBD(0, 0));
        map.put(ColorSpace.YUV444, yuv444);

        Map<ColorSpace, TransformHiBD> yuv444_10 = new HashMap<ColorSpace, TransformHiBD>();
        yuv444_10.put(ColorSpace.YUV444_10, new Idential());
        yuv444_10.put(ColorSpace.RGB, new Yuv444pToRgb(2, 0));
        yuv444_10.put(ColorSpace.YUV420, new Yuv444pToYuv420pHiBD(0, 2));
        map.put(ColorSpace.YUV444_10, yuv444_10);

        Map<ColorSpace, TransformHiBD> yuv420j = new HashMap<ColorSpace, TransformHiBD>();
        yuv420j.put(ColorSpace.YUV420J, new Idential());
        yuv420j.put(ColorSpace.RGB, new Yuv420jToRgbHiBD());
        yuv420j.put(ColorSpace.YUV420, new Yuv420jToYuv420HiBD());
        map.put(ColorSpace.YUV420J, yuv420j);

        Map<ColorSpace, TransformHiBD> yuv422j = new HashMap<ColorSpace, TransformHiBD>();
        yuv422j.put(ColorSpace.YUV422J, new Idential());
        yuv422j.put(ColorSpace.RGB, new Yuv422jToRgbHiBD());
        yuv422j.put(ColorSpace.YUV420, new Yuv422jToYuv420pHiBD());
        yuv422j.put(ColorSpace.YUV420J, new Yuv422pToYuv420pHiBD(0, 0));
        map.put(ColorSpace.YUV422J, yuv422j);

        Map<ColorSpace, TransformHiBD> yuv444j = new HashMap<ColorSpace, TransformHiBD>();
        yuv444j.put(ColorSpace.YUV444J, new Idential());
        yuv444j.put(ColorSpace.RGB, new Yuv444jToRgbHiBD());
        yuv444j.put(ColorSpace.YUV420, new Yuv444jToYuv420pHiBD());
        yuv444j.put(ColorSpace.YUV420J, new Yuv444pToYuv420pHiBD(0, 0));
        map.put(ColorSpace.YUV444J, yuv444j);
    }

    public static TransformHiBD getTransform(ColorSpace from, ColorSpace to) {
        Map<ColorSpace, TransformHiBD> map2 = map.get(from);

        return map2 == null ? null : map2.get(to);
    }

    public static class Idential implements TransformHiBD {
        @Override
        public void transform(PictureHiBD src, PictureHiBD dst) {
            for (int i = 0; i < 3; i++)
                arraycopy(src.getPlaneData(i), 0, dst.getPlaneData(i), 0, Math.min(
                        src.getPlaneWidth(i) * src.getPlaneHeight(i), dst.getPlaneWidth(i) * dst.getPlaneHeight(i)));

        }
    }
}
