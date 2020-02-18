package net.sourceforge.jaad.aac.error;

import org.jcodec.common.io.BitReader;

import net.sourceforge.jaad.aac.AACException;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
public class BitsBuffer {

    int bufa, bufb, len;
    // bit-twiddling helpers
    static final int[] S = { 1, 2, 4, 8, 16 };
    static final int[] B = { 0x55555555, 0x33333333, 0x0F0F0F0F, 0x00FF00FF, 0x0000FFFF };

    public BitsBuffer() {
        len = 0;
    }

    public int getLength() {
        return len;
    }

    public int showBits(int bits) {
        if (bits == 0)
            return 0;
        if (len <= 32) {
            // huffman_spectral_data_2 needs to read more than may be available,
            // bits maybe > len, deliver 0 than
            if (len >= bits)
                return ((bufa >> (len - bits)) & (0xFFFFFFFF >> (32 - bits)));
            else
                return ((bufa << (bits - len)) & (0xFFFFFFFF >> (32 - bits)));
        } else {
            if ((len - bits) < 32)
                return ((bufb & (0xFFFFFFFF >> (64 - len))) << (bits - len + 32)) | (bufa >> (len - bits));
            else
                return ((bufb >> (len - bits - 32)) & (0xFFFFFFFF >> (32 - bits)));
        }
    }

    public boolean flushBits(int bits) {
        len -= bits;

        boolean b;
        if (len < 0) {
            len = 0;
            b = false;
        } else
            b = true;
        return b;
    }

    public int getBits(int n) {
        int i = showBits(n);
        if (!flushBits(n))
            i = -1;
        return i;
    }

    public int getBit() {
        int i = showBits(1);
        if (!flushBits(1))
            i = -1;
        return i;
    }

    public void rewindReverse() {
        if (len == 0)
            return;
        final int[] i = BitsBuffer.rewindReverse64(bufb, bufa, len);
        bufb = i[0];
        bufa = i[1];
    }

    // merge bits of a to b
    public void concatBits(BitsBuffer a) {
        if (a.len == 0)
            return;
        int al = a.bufa;
        int ah = a.bufb;

        int bl, bh;
        if (len > 32) {
            // mask off superfluous high b bits
            bl = bufa;
            bh = bufb & ((1 << (len - 32)) - 1);
            // left shift a len bits
            ah = al << (len - 32);
            al = 0;
        } else {
            bl = bufa & ((1 << (len)) - 1);
            bh = 0;
            ah = (ah << (len)) | (al >> (32 - len));
            al = al << len;
        }

        // merge
        bufa = bl | al;
        bufb = bh | ah;

        len += a.len;
    }

    public void readSegment(int segwidth, BitReader _in) throws AACException {
        len = segwidth;

        if (segwidth > 32) {
            bufb = _in.readNBit(segwidth - 32);
            bufa = _in.readNBit(32);
        } else {
            bufa = _in.readNBit(segwidth);
            bufb = 0;
        }
    }

    // 32 bit rewind and reverse
    static int rewindReverse32(int v, int len) {
        v = ((v >> S[0]) & B[0]) | ((v << S[0]) & ~B[0]);
        v = ((v >> S[1]) & B[1]) | ((v << S[1]) & ~B[1]);
        v = ((v >> S[2]) & B[2]) | ((v << S[2]) & ~B[2]);
        v = ((v >> S[3]) & B[3]) | ((v << S[3]) & ~B[3]);
        v = ((v >> S[4]) & B[4]) | ((v << S[4]) & ~B[4]);

        // shift off low bits
        v >>= (32 - len);

        return v;
    }

    // 64 bit rewind and reverse
    static int[] rewindReverse64(int hi, int lo, int len) {
        int[] i = new int[2];
        if (len <= 32) {
            i[0] = 0;
            i[1] = rewindReverse32(lo, len);
        } else {
            lo = ((lo >> S[0]) & B[0]) | ((lo << S[0]) & ~B[0]);
            hi = ((hi >> S[0]) & B[0]) | ((hi << S[0]) & ~B[0]);
            lo = ((lo >> S[1]) & B[1]) | ((lo << S[1]) & ~B[1]);
            hi = ((hi >> S[1]) & B[1]) | ((hi << S[1]) & ~B[1]);
            lo = ((lo >> S[2]) & B[2]) | ((lo << S[2]) & ~B[2]);
            hi = ((hi >> S[2]) & B[2]) | ((hi << S[2]) & ~B[2]);
            lo = ((lo >> S[3]) & B[3]) | ((lo << S[3]) & ~B[3]);
            hi = ((hi >> S[3]) & B[3]) | ((hi << S[3]) & ~B[3]);
            lo = ((lo >> S[4]) & B[4]) | ((lo << S[4]) & ~B[4]);
            hi = ((hi >> S[4]) & B[4]) | ((hi << S[4]) & ~B[4]);

            // shift off low bits
            i[1] = (hi >> (64 - len)) | (lo << (len - 32));
            i[1] = lo >> (64 - len);
        }
        return i;
    }

}
