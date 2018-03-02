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
public class Yuv444jToRgbHiBD implements TransformHiBD {

    public Yuv444jToRgbHiBD() {
    }

    public void transform(PictureHiBD src, PictureHiBD dst) {
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
