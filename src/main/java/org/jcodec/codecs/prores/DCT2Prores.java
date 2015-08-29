package org.jcodec.codecs.prores;

import static org.jcodec.common.model.ColorSpace.YUV422_10;

import java.nio.ByteBuffer;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class DCT2Prores extends ProresEncoder {

    public DCT2Prores(Profile profile) {
        super(profile);
    }

    @Override
    protected int encodeSlice(ByteBuffer out, int[][] scaledLuma, int[][] scaledChroma, int[] scan, int sliceMbCount,
            int mbX, int mbY, Picture source, int prevQp, int mbWidth, int mbHeight, boolean unsafe) {

        Picture striped = sliceData(source, mbX, mbY, mbWidth, sliceMbCount);

        int est = (sliceMbCount >> 2) * profile.bitrate;
        int low = est - (est >> 3); // 12% bitrate fluctuation
        int high = est + (est >> 3);

        int qp = prevQp;

        out.put((byte) (6 << 3)); // hdr size
        out.put((byte) qp); // qscale
        ByteBuffer fork = out.duplicate();
        out.putInt(0);
        int rem = out.position();
        int[] sizes = new int[3];
        encodeSliceData(out, scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp, sizes);
        if (bits(sizes) > high && qp < profile.lastQp) {
            do {
                ++qp;
                out.position(rem);
                encodeSliceData(out, scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp, sizes);
            } while (bits(sizes) > high && qp < profile.lastQp);
        } else if (bits(sizes) < low && qp > profile.firstQp) {
            do {
                --qp;
                out.position(rem);
                encodeSliceData(out, scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp, sizes);
            } while (bits(sizes) < low && qp > profile.firstQp);
        }

        fork.putShort((short) sizes[0]);
        fork.putShort((short) sizes[1]);

        return qp;
    }

    private Picture sliceData(Picture source, int mbX, int mbY, int mbWidth, int sliceMbCount) {

        Picture pic = Picture.create(sliceMbCount << 4, 16, YUV422_10);
        int[][] out = pic.getData();
        int[][] in = source.getData();

        System.arraycopy(in[0], (mbY * mbWidth + mbX) << 8, out[0], 0, out[0].length);
        System.arraycopy(in[1], (mbY * mbWidth + mbX) << 7, out[1], 0, out[1].length);
        System.arraycopy(in[2], (mbY * mbWidth + mbX) << 7, out[2], 0, out[2].length);

        return pic;
    }
}
