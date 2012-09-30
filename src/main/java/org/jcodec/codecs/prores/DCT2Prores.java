package org.jcodec.codecs.prores;

import static org.jcodec.common.model.ColorSpace.YUV422_10;

import java.io.DataOutput;
import java.io.IOException;

import org.jcodec.common.model.Picture;

public class DCT2Prores extends ProresEncoder {

    public DCT2Prores(Profile profile) {
        super(profile);
    }

    @Override
    protected int encodeSlice(DataOutput out, int[][] scaledLuma, int[][] scaledChroma, int[] scan, int sliceMbCount,
            int mbX, int mbY, Picture source, int prevQp, int mbWidth, int mbHeight, boolean unsafe) throws IOException {

        Picture striped = sliceData(source, mbX, mbY, mbWidth, sliceMbCount);

        int est = (sliceMbCount >> 2) * profile.bitrate;
        int low = est - (est >> 3); // 12% bitrate fluctuation
        int high = est + (est >> 3);

        int qp = prevQp;
        byte[][] data = encodeSliceData(scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp);
        if (bits(data) > high && qp < profile.lastQp) {
            do {
                ++qp;
                data = encodeSliceData(scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp);
            } while (bits(data) > high && qp < profile.lastQp);
        } else if (bits(data) < low && qp > profile.firstQp) {
            do {
                --qp;
                data = encodeSliceData(scaledLuma[qp - 1], scaledChroma[qp - 1], scan, sliceMbCount, striped, qp);
            } while (bits(data) < low && qp > profile.firstQp);
        }

        out.write(6 << 3); // hdr size
        out.write(qp); // qscale
        out.writeShort(data[0].length);
        out.writeShort(data[1].length);

        out.write(data[0]);
        out.write(data[1]);
        out.write(data[2]);

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
