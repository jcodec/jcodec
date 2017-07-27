package org.jcodec.scale.highbd;

import static org.jcodec.scale.highbd.Yuv420jToRgbHiBD.YUVJtoRGB;

import org.jcodec.common.model.PictureHiBD;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv422jToRgbHiBD implements TransformHiBD {

    public Yuv422jToRgbHiBD() {
    }

    public void transform(PictureHiBD src, PictureHiBD dst) {
        int[] y = src.getPlaneData(0);
        int[] u = src.getPlaneData(1);
        int[] v = src.getPlaneData(2);

        int[] data = dst.getPlaneData(0);

        int offLuma = 0, offChroma = 0;
        for (int i = 0; i < dst.getHeight(); i++) {
            for (int j = 0; j < dst.getWidth(); j += 2) {
                YUVJtoRGB(y[offLuma], u[offChroma], v[offChroma], data, offLuma * 3);
                YUVJtoRGB(y[offLuma + 1], u[offChroma], v[offChroma], data, (offLuma + 1) * 3);
                offLuma += 2;
                ++offChroma;
            }
        }

    }
}
