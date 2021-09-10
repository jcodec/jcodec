package org.jcodec.codecs.vpx.vp8;

import java.util.Arrays;

import org.jcodec.codecs.vpx.vp8.data.VarianceFNs;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.PositionableIntArrPointer;
import org.jcodec.codecs.vpx.vp8.pointerhelper.ReadOnlyIntArrPointer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class CommonUtils {
    public static void vp8_zero(int[] dataToZero) {
        Arrays.fill(dataToZero, 0);
    }

    public static void vp8_zero(int[][] dataToZero) {
        for (int[] subArr : dataToZero) {
            vp8_zero(subArr);
        }
    }

    public static void vp8_zero(int[][][] dataToZero) {
        for (int[][] sub2DArr : dataToZero) {
            vp8_zero(sub2DArr);
        }
    }

    public static void vp8_zero(int[][][][] dataToZero) {
        for (int[][][] sub3DArr : dataToZero) {
            vp8_zero(sub3DArr);
        }
    }

    public static void vp8_zero(short[] dataToZero) {
        Arrays.fill(dataToZero, (short) 0);
    }

    public static void vp8_zero(short[][] dataToZero) {
        for (short[] subArr : dataToZero) {
            vp8_zero(subArr);
        }
    }

    public static void vp8_zero(short[][][] dataToZero) {
        for (short[][] sub2DArr : dataToZero) {
            vp8_zero(sub2DArr);
        }
    }

    public static void vp8_zero(short[][][][] dataToZero) {
        for (short[][][] sub3DArr : dataToZero) {
            vp8_zero(sub3DArr);
        }
    }

    public static void vp8_zero(FullAccessIntArrPointer dataToZero) {
        dataToZero.memset(0, (short) 0, dataToZero.getRemaining());
    }

    public static void vp8_copy(int[] source, int[] target) {
        System.arraycopy(source, 0, target, 0, source.length);
    }

    public static void vp8_copy(int[][] source, int[][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }

    public static void vp8_copy(int[][][] source, int[][][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }

    public static void vp8_copy(int[][][][] source, int[][][][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }

    public static void vp8_copy(short[] source, short[] target) {
        System.arraycopy(source, 0, target, 0, source.length);
    }

    public static void vp8_copy(short[][] source, short[][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }

    public static void vp8_copy(short[][][] source, short[][][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }

    public static void vp8_copy(short[][][][] source, short[][][][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }

    public static void vp8_copy(short[][][][][] source, short[][][][][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }

    public static void vp8_copy(short[][][][][][] source, short[][][][][][] target) {
        for (int i = 0; i < source.length; i++) {
            vp8_copy(source[i], target[i]);
        }
    }
    
    public static void vp8_copy(ReadOnlyIntArrPointer source, FullAccessIntArrPointer target) {
        target.memcopyin(0, source, 0, source.getRemaining());
    }

    public static short clipPixel(final short val) {
        return CommonUtils.clamp(val, (short) 0, (short) 255);
    }

    public static short byteClamp(final short val) {
        return CommonUtils.clamp(val, (short) Byte.MIN_VALUE, (short) Byte.MAX_VALUE);
    }

    public static short clamp(final short value, final short low, final short high) {
        return value < low ? low : (value > high ? high : value);
    }

    public static void genericCopy(final PositionableIntArrPointer src_ptr, final int src_stride,
            final FullAccessIntArrPointer dst_ptr, final int dst_stride, final int height, final int width) {
        for (int r = 0; r < height; ++r) {
            dst_ptr.memcopyin(r * dst_stride, src_ptr, r * src_stride, width);
        }
    }

    static void vp8_copy_mem8x4(PositionableIntArrPointer src, int src_stride, FullAccessIntArrPointer dst,
            int dst_stride) {
        genericCopy(src, src_stride, dst, dst_stride, 8, 4);
    }

    static void vp8_copy_mem8x8(PositionableIntArrPointer src, int src_stride, FullAccessIntArrPointer dst,
            int dst_stride) {
        genericCopy(src, src_stride, dst, dst_stride, 8, 8);
    }

    static void vp8_copy_mem16x16(PositionableIntArrPointer src, int src_stride, FullAccessIntArrPointer dst,
            int dst_stride) {
        genericCopy(src, src_stride, dst, dst_stride, 16, 16);
    }

    public static VarianceFNs.COPY vp8_copy32xn = new VarianceFNs.COPY() {
        @Override
        public void call(PositionableIntArrPointer src_ptr, int src_stride, FullAccessIntArrPointer ref_ptr,
                int ref_stride, int n) {
            genericCopy(src_ptr, src_stride, ref_ptr, ref_stride, n, 32);
        }
    };

    static int roundPowerOfTwo(int value, int n) {
        return (value + (1 << (n - 1))) >> n;
    }

    static long roundPowerOfTwo(long value, int n) {
        return (value + (1 << (n - 1))) >> n;
    }

}
