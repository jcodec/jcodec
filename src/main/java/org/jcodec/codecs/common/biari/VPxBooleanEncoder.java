package org.jcodec.codecs.common.biari;
import org.jcodec.codecs.vpx.VPXConst;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Containes boolean encoder from VPx codecs
 * 
 * @author The JCodec project
 * 
 */
public class VPxBooleanEncoder {
    private ByteBuffer out;
    private int lowvalue;
    private int range;
    private int count;

    public VPxBooleanEncoder(ByteBuffer out) {
        this.out = out;
        lowvalue = 0;
        range = 255;
        count = -24;
    }

    public void writeBit(int prob, int bb) {
        int split = 1 + (((range - 1) * prob) >> 8);

        if (bb != 0) {
            lowvalue += split;
            range -= split;
        } else {
            range = split;
        }

        int shift = VPXConst.vp8Norm[range];
        range <<= shift;
        count += shift;

        if (count >= 0) {
            int offset = shift - count;

            if (((lowvalue << (offset - 1)) & 0x80000000) != 0) {
                int x = out.position() - 1;

                while (x >= 0 && out.getAt(x) == -1) {
                    out.putAt(x, (byte) 0);
                    x--;
                }

                out.putAt(x, (byte) ((out.getAt(x) & 0xff) + 1));
            }

            out.put((byte) (lowvalue >> (24 - offset)));
            lowvalue <<= offset;
            shift = count;
            lowvalue &= 0xffffff;
            count -= 8;
        }

        lowvalue <<= shift;
    }

    public void stop() {
        int i;

        for (i = 0; i < 32; i++)
            writeBit(128, 0);
    }

    public int position() {
        return out.position() + ((count + 24) >> 3);
    }
}