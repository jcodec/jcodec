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
public class Yuv444jToRgb implements Transform {

    public Yuv444jToRgb() {
    }

    public void transform(Picture src, Picture dst) {
        int[] y = src.getPlaneData(0);
        int[] u = src.getPlaneData(1);
        int[] v = src.getPlaneData(2);

        int[] data = dst.getPlaneData(0);

        for (int i = 0, srcOff = 0, dstOff = 0; i < dst.getHeight(); i++) {
            for (int j = 0; j < dst.getWidth(); j++, srcOff++, dstOff += 3) {
                YUVJtoRGB(y[srcOff], u[srcOff], v[srcOff], data, dstOff);
            }
        }
    }
}
