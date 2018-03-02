package org.jcodec.scale.highbd;
import org.jcodec.api.NotSupportedException;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.PictureHiBD;

import java.lang.IllegalArgumentException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
@Deprecated
public class RgbToBgrHiBD implements TransformHiBD {

    @Override
    public void transform(PictureHiBD src, PictureHiBD dst) {
        if (src.getColor() != ColorSpace.RGB && src.getColor() != ColorSpace.BGR
                || dst.getColor() != ColorSpace.RGB && dst.getColor() != ColorSpace.BGR) {
            throw new IllegalArgumentException(
                    "Expected RGB or BGR inputs, was: " + src.getColor() + ", " + dst.getColor());
        }
        if (src.getCrop() != null || dst.getCrop() != null)
            throw new NotSupportedException("Cropped images not supported");

        int[] dataSrc = src.getPlaneData(0);
        int[] dataDst = dst.getPlaneData(0);
        for (int i = 0; i < dataSrc.length; i += 3) {
            // src and dst can actually be the same array
            int tmp = dataSrc[i + 2];
            dataDst[i + 2] = dataSrc[i];
            dataDst[i] = tmp;
        }
    }

}
