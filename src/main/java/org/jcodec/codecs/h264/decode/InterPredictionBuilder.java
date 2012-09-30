package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.io.model.SubMBType.L0_4x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x4;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x8;

import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;
import org.jcodec.common.model.Rect;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A builder for inter prediction of the macroblock
 * 
 * 
 * @author Jay Codec
 * 
 */
public class InterPredictionBuilder {

    private BlockInterpolator interpolator;

    public InterPredictionBuilder() {
        this.interpolator = new BlockInterpolator();
    }

    public int[] predict16x16(Picture[] reference, Vector[] mVectors, Point origin) {
        int[] result = new int[256];

        PixelBuffer pixels = new PixelBuffer(result, 0, 4);

        Vector mv = mVectors[0];
        Rect blkRect = new Rect((origin.getX() << 2) + mv.getX(), (origin.getY() << 2) + mv.getY(), 16, 16);

        interpolator.getBlockLuma(reference[mv.getRefId()], pixels, blkRect);

        return result;
    }

    public int[] predict16x8(Picture[] reference, Vector[] mVectors, Point origin) {
        int[] result = new int[256];

        Vector mv0 = mVectors[0];
        Rect rect1 = new Rect((origin.getX() << 2) + mv0.getX(), (origin.getY() << 2) + mv0.getY(), 16, 8);
        interpolator.getBlockLuma(reference[mv0.getRefId()], PixelBuffer.wrap(result, 0, 4), rect1);

        Vector mv1 = mVectors[1];
        Rect rect2 = new Rect((origin.getX() << 2) + mv1.getX(), (origin.getY() << 2) + mv1.getY() + 32, 16, 8);
        interpolator.getBlockLuma(reference[mv1.getRefId()], PixelBuffer.wrap(result, 128, 4), rect2);

        return result;
    }

    public int[] predict8x16(Picture[] reference, Vector[] mVectors, Point origin) {

        int[] result = new int[256];

        Vector mv0 = mVectors[0];
        Rect rect0 = new Rect((origin.getX() << 2) + mv0.getX(), (origin.getY() << 2) + mv0.getY(), 8, 16);
        interpolator.getBlockLuma(reference[mv0.getRefId()], PixelBuffer.wrap(result, 0, 4), rect0);

        Vector mv1 = mVectors[1];
        Rect rect1 = new Rect((origin.getX() << 2) + mv1.getX() + 32, (origin.getY() << 2) + mv1.getY(), 8, 16);
        interpolator.getBlockLuma(reference[mv1.getRefId()], PixelBuffer.wrap(result, 8, 4), rect1);

        return result;
    }

    public int[] predict8x8(Picture[] Picture, SubMBType[] subPred, Vector[][] mVectors, Point origin) {

        int[] result = new int[256];

        for (int y8x8 = 0; y8x8 < 2; y8x8++) {
            for (int x8x8 = 0; x8x8 < 2; x8x8++) {

                int blkIdx = y8x8 * 2 + x8x8;
                if (subPred[blkIdx] == L0_8x8) {
                    predictSub8x8(Picture, mVectors[blkIdx], origin, result, x8x8, y8x8);

                } else if (subPred[blkIdx] == L0_8x4) {
                    predictSub8x4(Picture, mVectors[blkIdx], origin, result, x8x8, y8x8);

                } else if (subPred[blkIdx] == L0_4x8) {
                    predictSub4x8(Picture, mVectors[blkIdx], origin, result, x8x8, y8x8);

                } else {
                    predictSub4x4(Picture, mVectors[blkIdx], origin, result, x8x8, y8x8);

                }
            }
        }
        return result;
    }

    private void predictSub8x8(Picture[] reference, Vector[] mVectors, Point origin, int[] result, int x8x8, int y8x8) {
        Vector mv0 = mVectors[0];

        Rect rect = new Rect((origin.getX() << 2) + mv0.getX() + (x8x8 << 5), (origin.getY() << 2) + mv0.getY()
                + (y8x8 << 5), 8, 8);

        interpolator.getBlockLuma(reference[mv0.getRefId()], PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3), 4),
                rect);
    }

    private void predictSub8x4(Picture[] reference, Vector[] mVectors, Point origin, int[] result, int x8x8, int y8x8) {

        Vector mv0 = mVectors[0];
        Rect rect1 = new Rect((origin.getX() << 2) + mv0.getX() + (x8x8 << 5), (origin.getY() << 2) + mv0.getY()
                + (y8x8 << 5), 8, 4);
        interpolator.getBlockLuma(reference[mv0.getRefId()], PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3), 4),
                rect1);

        Vector mv1 = mVectors[1];
        Rect rect2 = new Rect((origin.getX() << 2) + mv1.getX() + (x8x8 << 5), (origin.getY() << 2) + mv1.getY() + 16
                + (y8x8 << 5), 8, 4);
        interpolator.getBlockLuma(reference[mv1.getRefId()],
                PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3) + 64, 4), rect2);
    }

    private void predictSub4x8(Picture[] reference, Vector[] mVectors, Point origin, int[] result, int x8x8, int y8x8) {

        Vector mv0 = mVectors[0];
        Rect rect0 = new Rect((origin.getX() << 2) + mv0.getX() + (x8x8 << 5), (origin.getY() << 2) + mv0.getY()
                + (y8x8 << 5), 4, 8);
        interpolator.getBlockLuma(reference[mv0.getRefId()], PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3), 4),
                rect0);

        Vector mv1 = mVectors[1];
        Rect rect1 = new Rect((origin.getX() << 2) + mv1.getX() + 16 + (x8x8 << 5), (origin.getY() << 2) + mv1.getY()
                + (y8x8 << 5), 4, 8);
        interpolator.getBlockLuma(reference[mv1.getRefId()],
                PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3) + 4, 4), rect1);
    }

    private void predictSub4x4(Picture[] reference, Vector[] mVectors, Point origin, int[] result, int x8x8, int y8x8) {

        Vector mv0 = mVectors[0];
        Rect rect0 = new Rect((origin.getX() << 2) + mv0.getX() + (x8x8 << 5), (origin.getY() << 2) + mv0.getY()
                + (y8x8 << 5), 4, 4);
        interpolator.getBlockLuma(reference[mv0.getRefId()], PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3), 4),
                rect0);

        Vector mv1 = mVectors[1];
        Rect rect1 = new Rect((origin.getX() << 2) + mv1.getX() + 16 + (x8x8 << 5), (origin.getY() << 2) + mv1.getY()
                + (y8x8 << 5), 4, 4);
        interpolator.getBlockLuma(reference[mv1.getRefId()],
                PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3) + 4, 4), rect1);

        Vector mv2 = mVectors[2];
        Rect rect2 = new Rect((origin.getX() << 2) + mv2.getX() + (x8x8 << 5), (origin.getY() << 2) + mv2.getY() + 16
                + (y8x8 << 5), 4, 4);
        interpolator.getBlockLuma(reference[mv2.getRefId()],
                PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3) + 64, 4), rect2);

        Vector mv3 = mVectors[3];
        Rect rect3 = new Rect((origin.getX() << 2) + mv3.getX() + 16 + (x8x8 << 5), (origin.getY() << 2) + mv3.getY()
                + 16 + (y8x8 << 5), 4, 4);
        interpolator.getBlockLuma(reference[mv3.getRefId()],
                PixelBuffer.wrap(result, (y8x8 << 7) + (x8x8 << 3) + 68, 4), rect3);
    }
}