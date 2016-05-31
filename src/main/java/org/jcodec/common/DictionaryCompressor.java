package org.jcodec.common;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.io.VLC;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.platform.Platform;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Compresses a set of long's creating a dictionary and then outputting it using
 * a huffman table
 * 
 * @author The JCodec project
 * 
 */
public class DictionaryCompressor {
    protected VLC buildCodes(int[] counts, int esc) {
        int[] codes = new int[counts.length];
        int[] codeSizes = new int[counts.length];
        int code = 0;
        for (; code < Math.min(codes.length, esc); code++) {
            int max = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > counts[max])
                    max = i;
            }

            codes[max] = code;
            codeSizes[max] = Math.max(1, MathUtil.log2(code));
            counts[max] = Integer.MIN_VALUE;
        }
        int escSize = MathUtil.log2(esc);
        for (int i = 0; i < counts.length; i++)
            if (counts[i] >= 0) {
                codes[i] = esc;
                codeSizes[i] = escSize;
            }

        return new VLC(codes, codeSizes);
    }

    public static class Long extends DictionaryCompressor {
        public void compress(long[] values, ByteBuffer bb) {
            RunLength.Long rl = getValueStats(values);
            int[] counts = rl.getCounts();
            long[] keys = rl.getValues();
            VLC vlc = buildCodes(counts, values.length / 10);
            int[] codes = vlc.getCodes();
            int[] codeSizes = vlc.getCodeSizes();
            bb.putInt(codes.length);
            for (int i = 0; i < codes.length; i++) {
                bb.put((byte) codeSizes[i]);
                bb.putShort((short) (codes[i] >>> 16));
                bb.putLong(keys[i]);
            }

            BitWriter br = new BitWriter(bb);
            for (int j = 0; j < values.length; j++) {
                long l = values[j];
                for (int i = 0; i < keys.length; i++)
                    if (keys[i] == l) {
                        vlc.writeVLC(br, i);
                        if (codes[i] == 0xf)
                            br.writeNBit(16, i);
                    }
            }
            br.flush();
        }

        private RunLength.Long getValueStats(long[] values) {
            long[] copy = Platform.copyOfLong(values, values.length);
            Arrays.sort(copy);
            RunLength.Long rl = new RunLength.Long();
            for (int i = 0; i < copy.length; i++) {
                long l = copy[i];
                rl.add(l);
            }
            return rl;
        }
    }

    public static class Int extends DictionaryCompressor {
        public void compress(int[] values, ByteBuffer bb) {
            RunLength.Integer rl = getValueStats(values);
            int[] counts = rl.getCounts();
            int[] keys = rl.getValues();
            int esc = Math.max(1, (1 << (MathUtil.log2(counts.length) - 2)) - 1);
            VLC vlc = buildCodes(counts, esc);
            int[] codes = vlc.getCodes();
            int[] codeSizes = vlc.getCodeSizes();
            bb.putInt(codes.length);
            for (int i = 0; i < codes.length; i++) {
                bb.put((byte) codeSizes[i]);
                bb.putShort((short) (codes[i] >>> 16));
                bb.putInt(keys[i]);
            }

            BitWriter br = new BitWriter(bb);
            for (int j = 0; j < values.length; j++) {
                int l = values[j];
                for (int i = 0; i < keys.length; i++)
                    if (keys[i] == l) {
                        vlc.writeVLC(br, i);
                        if (codes[i] == esc)
                            br.writeNBit(i, 16);
                    }
            }
            br.flush();
        }

        private RunLength.Integer getValueStats(int[] values) {
            int[] copy = Platform.copyOfInt(values, values.length);
            Arrays.sort(copy);
            RunLength.Integer rl = new RunLength.Integer();
            for (int i = 0; i < copy.length; i++) {
                int l = copy[i];
                rl.add(l);
            }
            return rl;
        }
    }
}
