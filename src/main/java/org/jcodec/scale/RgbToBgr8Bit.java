package org.jcodec.scale;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;

import java.lang.IllegalArgumentException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class RgbToBgr8Bit implements Transform8Bit {

    @Override
    public void transform(Picture8Bit src, Picture8Bit dst) {
        if (src.getColor() != ColorSpace.RGB && src.getColor() != ColorSpace.BGR
                || dst.getColor() != ColorSpace.RGB && dst.getColor() != ColorSpace.BGR) {
            throw new IllegalArgumentException(
                    "Expected RGB or BGR inputs, was: " + src.getColor() + ", " + dst.getColor());
        }

        byte[] dataSrc = src.getPlaneData(0);
        byte[] dataDst = dst.getPlaneData(0);
        for (int i = 0; i < dataSrc.length; i += 3) {
            // src and dst can actually be the same array
            byte tmp = dataSrc[i + 2];
            dataDst[i + 2] = dataSrc[i];
            dataDst[i] = tmp;
        }
    }
}
