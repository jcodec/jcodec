package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.BPredictionMode;

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
public class Partition_Info {

    public static class BMI {
        public BPredictionMode mode = BPredictionMode.B_DC_PRED;
        public MV mv = new MV();
    }

    public int count;
    public BMI[] bmi = new BMI[16];

    public Partition_Info() {
        count = 0;
        for (int i = 0; i < bmi.length; i++) {
            bmi[i] = new BMI();
        }
    }

    public void copyin(Partition_Info other) {
        this.count = other.count;
        for (int i = 0; i < bmi.length; i++) {
            bmi[i] = other.bmi[i];
        }
    }
}
