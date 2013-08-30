package org.jcodec.scale;

import static org.jcodec.scale.Yuv420jToRgb.YUVJtoRGB;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Yuv422jToRgb implements Transform {

    public Yuv422jToRgb() {
    }

    public void transform(Picture src, Picture dst) {
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
