package org.jcodec.scale;

import static org.jcodec.scale.Yuv422pToRgb.YUV444toRGB888;

import org.jcodec.common.model.Picture;

public class Yuv420pToRgb implements Transform {

    private final int downShift;
    private final int upShift;

    public Yuv420pToRgb(int upShift, int downShift) {
        this.upShift = upShift;
        this.downShift = downShift;
    }

    public void transform(Picture src, Picture dst) {
        int[] y = src.getPlaneData(0);
        int[] u = src.getPlaneData(1);
        int[] v = src.getPlaneData(2);
        int[] data = dst.getPlaneData(0);

        int offLuma = 0, offChroma = 0;
        int stride = dst.getWidth();
        for (int i = 0; i < dst.getHeight(); i +=2 ) {
            for (int j = 0; j < stride; j += 2) {
                YUV444toRGB888((y[offLuma + j] << upShift) >> downShift, (u[offChroma] << upShift) >> downShift,
                        (v[offChroma] << upShift) >> downShift, data, (offLuma + j) * 3);
                YUV444toRGB888((y[offLuma + j + 1] << upShift) >> downShift, (u[offChroma] << upShift) >> downShift,
                        (v[offChroma] << upShift) >> downShift, data, (offLuma + j + 1) * 3);

                YUV444toRGB888((y[offLuma + j + stride] << upShift) >> downShift,
                        (u[offChroma] << upShift) >> downShift, (v[offChroma] << upShift) >> downShift, data,
                        (offLuma + j + stride) * 3);
                YUV444toRGB888((y[offLuma + j + stride + 1] << upShift) >> downShift,
                        (u[offChroma] << upShift) >> downShift, (v[offChroma] << upShift) >> downShift, data,
                        (offLuma + j + stride + 1) * 3);

                ++offChroma;
            }
            offLuma += 2 * stride;
        }
    }
}