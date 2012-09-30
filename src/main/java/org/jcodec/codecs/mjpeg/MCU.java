package org.jcodec.codecs.mjpeg;

import java.nio.IntBuffer;

import org.jcodec.codecs.mjpeg.FrameHeader.Component;
import org.jcodec.codecs.mjpeg.tools.Asserts;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * 3.1.88 minimum coded unit; MCU: The smallest group of data units that is
 * coded.
 */
public class MCU {
    public static class Comp {
        final int[][] data;
        final int h;
        final int v;

        public Comp(int h, int v) {
            this.h = h;
            this.v = v;
            data = new int[h * v][64];
        }

        void clear() {
            for (int i = 0; i < data.length; i++) {
                System.arraycopy(zero, 0, data[i], 0, 64);
            }
        }
    }

    private boolean is420;
    private boolean is444;
    private boolean is422;

    public boolean is420() {
        return is420;
    }

    public boolean is444() {
        return is444;
    }

    public boolean is422() {
        return is422;
    }

    Comp lum;
    Comp cb;
    Comp cr;

    /** max horizontal components */
    int h;
    /** max vertical components */
    int v;

    public int getMaxHorizComponents() {
        return h >>> 3;
    }

    public int getMaxVertComponents() {
        return v >>> 3;
    }

    public int getWidth() {
        return h;
    }

    public int getHeight() {
        return v;
    }

    /** decoded int_rgb24 buffer */
    private IntBuffer rgb24;

    private final static int[] zero = new int[64];

    public void clear() {
        lum.clear();
        cb.clear();
        cr.clear();
    }

    public static MCU create(FrameHeader frame) {
        Asserts.assertEquals(3, frame.components.length);
        MCU mcu = new MCU();
        Component cLum = frame.components[0];
        Component cCb = frame.components[1];
        Component cCr = frame.components[2];
        mcu.lum = new Comp(cLum.h, cLum.v);
        mcu.cb = new Comp(cCb.h, cCb.v);
        mcu.cr = new Comp(cCr.h, cCr.v);
        mcu.h = frame.getHmax() * 8;
        mcu.v = frame.getVmax() * 8;
        mcu.is420 = (mcu.lum.h == 2 && mcu.lum.v == 2 && mcu.cb.h == 1
                && mcu.cb.v == 1 && mcu.cr.h == 1 && mcu.cr.v == 1);
        mcu.is444 = (mcu.lum.h == 1 && mcu.lum.v == 1 && mcu.cb.h == 1
                && mcu.cb.v == 1 && mcu.cr.h == 1 && mcu.cr.v == 1);
        mcu.is422 = (mcu.lum.h == 2 && mcu.lum.v == 1 && mcu.cb.h == 1
                && mcu.cb.v == 1 && mcu.cr.h == 1 && mcu.cr.v == 1);
        mcu.setRgb24(IntBuffer.allocate(mcu.getWidth() * mcu.getWidth()));
        return mcu;
    }

    void setRgb24(IntBuffer rgb24) {
        this.rgb24 = rgb24;
    }

    public IntBuffer getRgb24() {
        return rgb24.duplicate();
    }
}
