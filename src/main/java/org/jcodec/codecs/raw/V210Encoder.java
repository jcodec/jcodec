package org.jcodec.codecs.raw;

import static java.lang.System.arraycopy;

import java.io.IOException;
import java.io.OutputStream;

import org.jcodec.common.io.LittleEndianDataOutputStream;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * The encoder for yuv 10 bit 422
 * 
 * x|x|9876543210(cr0)|9876543210(y0) |9876543210(cb0) x|x|9876543210(y2)
 * |9876543210(cb1)|9876543210(y1) x|x|9876543210(cb2)|9876543210(y3)
 * |9876543210(cr1) x|x|9876543210(y5) |9876543210(cr2)|9876543210(y4)
 * 
 * @author The JCodec project
 * 
 */
public class V210Encoder {
    public void encodeFrame(OutputStream out, Picture frame) throws IOException {
        int tgtStride = ((frame.getPlaneWidth(0) + 47) / 48) * 48;
        int[][] data = frame.getData();

        int[] tmpY = new int[tgtStride];
        int[] tmpCb = new int[tgtStride >> 1];
        int[] tmpCr = new int[tgtStride >> 1];

        LittleEndianDataOutputStream daos = new LittleEndianDataOutputStream(out);

        int yOff = 0, cbOff = 0, crOff = 0;
        for (int yy = 0; yy < frame.getHeight(); yy++) {
            arraycopy(data[0], yOff, tmpY, 0, frame.getPlaneWidth(0));
            arraycopy(data[1], cbOff, tmpCb, 0, frame.getPlaneWidth(1));
            arraycopy(data[2], crOff, tmpCr, 0, frame.getPlaneWidth(2));

            for (int yi = 0, cbi = 0, cri = 0; yi < tgtStride;) {
                int i = 0;
                i |= clip(tmpCr[cri++]) << 20;
                i |= clip(tmpY[yi++]) << 10;
                i |= clip(tmpCb[cbi++]);
                daos.writeInt(i);

                i = 0;
                i |= clip(tmpY[yi++]);
                i |= clip(tmpY[yi++]) << 20;
                i |= clip(tmpCb[cbi++]) << 10;
                daos.writeInt(i);

                i = 0;
                i |= clip(tmpCb[cbi++]) << 20;
                i |= clip(tmpY[yi++]) << 10;
                i |= clip(tmpCr[cri++]);
                daos.writeInt(i);

                i = 0;
                i |= clip(tmpY[yi++]);
                i |= clip(tmpY[yi++]) << 20;
                i |= clip(tmpCr[cri++]) << 10;
                daos.writeInt(i);
            }
            yOff += frame.getPlaneWidth(0);
            cbOff += frame.getPlaneWidth(1);
            crOff += frame.getPlaneWidth(2);
        }
    }

    static final int clip(int val) {
        return MathUtil.clip(val, 8, 1019);
    }
}