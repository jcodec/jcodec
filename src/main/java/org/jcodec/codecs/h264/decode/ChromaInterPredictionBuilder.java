package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.MVMatrix;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Builds inter prediction for chroma components
 * 
 * @author Jay Codec
 * 
 */
public class ChromaInterPredictionBuilder {

    private static int[] mapping = new int[] { 0, 1, 4, 5, 2, 3, 6, 7, 8, 9, 12, 13, 10, 11, 14, 15 };

    private BlockInterpolator interpolator;

    public ChromaInterPredictionBuilder() {
        this.interpolator = new BlockInterpolator();
    }

    public int[] predictCb(Picture[] reference, MVMatrix mvs, Point origin) {
        return predictChroma(reference, mvs, origin, true);
    }

    public int[] predictCr(Picture[] reference, MVMatrix mvs, Point origin) {
        return predictChroma(reference, mvs, origin, false);
    }

    private int[] predictChroma(Picture[] reference, MVMatrix mvs, Point origin, boolean cb) {

        int[] result = new int[64];

        for (int blkY = 0; blkY < 4; blkY++) {
            for (int blkX = 0; blkX < 4; blkX++) {

                Vector mv = mvs.getVectors()[mapping[blkY * 4 + blkX]];
                Picture ref = reference[mv.getRefId()];

                Rect rect = new Rect((origin.getX() << 2) + mv.getX() + (blkX << 4), (origin.getY() << 2) + mv.getY()
                        + (blkY << 4), 2, 2);

                PixelBuffer pixels = new PixelBuffer(result, (blkY << 4) + (blkX << 1), 3);

                if (cb)
                    interpolator.getBlockCb(ref, pixels, rect);
                else
                    interpolator.getBlockCr(ref, pixels, rect);
            }
        }

        return result;
    }

}
