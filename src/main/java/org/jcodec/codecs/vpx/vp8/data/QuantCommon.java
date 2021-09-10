package org.jcodec.codecs.vpx.vp8.data;

import static org.jcodec.codecs.vpx.vp8.data.CommonData.Comp.AC;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Comp.DC;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Quant.UV;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Quant.Y1;
import static org.jcodec.codecs.vpx.vp8.data.CommonData.Quant.Y2;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.data.CommonData.Comp;
import org.jcodec.codecs.vpx.vp8.data.CommonData.Quant;

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
public class QuantCommon {
    static final short[] dc_qlookup = { 4, 5, 6, 7, 8, 9, 10, 10, 11, 12, 13, 14, 15, 16, 17, 17, 18, 19, 20, 20, 21,
            21, 22, 22, 23, 23, 24, 25, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 37, 38, 39, 40, 41, 42, 43,
            44, 45, 46, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
            70, 71, 72, 73, 74, 75, 76, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 93, 95, 96, 98, 100,
            101, 102, 104, 106, 108, 110, 112, 114, 116, 118, 122, 124, 126, 128, 130, 132, 134, 136, 138, 140, 143,
            145, 148, 151, 154, 157, };
    static final short[] ac_qlookup = { 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
            52, 53, 54, 55, 56, 57, 58, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 82, 84, 86, 88, 90, 92, 94, 96, 98,
            100, 102, 104, 106, 108, 110, 112, 114, 116, 119, 122, 125, 128, 131, 134, 137, 140, 143, 146, 149, 152,
            155, 158, 161, 164, 167, 170, 173, 177, 181, 185, 189, 193, 197, 201, 205, 209, 213, 217, 221, 225, 229,
            234, 239, 245, 249, 254, 259, 264, 269, 274, 279, 284, };

    public static interface QuantLookup {
        short call(short qindex, short delta);
    }

    public static EnumMap<Quant, EnumMap<Comp, QuantLookup>> lookup = new EnumMap<Quant, EnumMap<Comp, QuantLookup>>(
            Quant.class);

    static {
        // Preparing the Y1 quant functions
        EnumMap<Comp, QuantLookup> temp = new EnumMap<Comp, QuantLookup>(Comp.class);
        temp.put(DC, new QuantLookup() {
            @Override
            public short call(short qindex, short delta) {
                return dc_lookup(qindex, delta);
            }
        });
        temp.put(AC, new QuantLookup() {
            @Override
            public short call(short qindex, short delta) {
                return ac_lookup(qindex, (short) 0);
            }
        });
        lookup.put(Y1, temp);

        // Preparing the Y2 quant functions
        temp = new EnumMap<Comp, QuantLookup>(Comp.class);
        temp.put(DC, new QuantLookup() {
            @Override
            public short call(short qindex, short delta) {
                return (short) (dc_lookup(qindex, delta) << 1);
            }
        });
        temp.put(AC, new QuantLookup() {
            @Override
            public short call(short qindex, short delta) {
                /*
                 * For all x in [0..284], x*155/100 is bitwise equal to (x*101581) >> 16. The
                 * smallest precision for that is '(x*6349) >> 12' but 16 is a good word size.
                 */
                return (short) Math.max(8, (ac_lookup(qindex, delta) * 101581) >> 16);
            }
        });
        lookup.put(Y2, temp);

        // Preparing the UV quant functions
        temp = new EnumMap<Comp, QuantLookup>(Comp.class);
        temp.put(DC, new QuantLookup() {
            @Override
            public short call(short qindex, short delta) {
                return CommonUtils.clamp(dc_lookup(qindex, delta), (short) 0, (short) 132);
            }
        });
        temp.put(AC, new QuantLookup() {
            @Override
            public short call(short qindex, short delta) {
                return ac_lookup(qindex, delta);
            }
        });
        lookup.put(UV, temp);
    }

    static short dc_lookup(short QIndex, short Delta) {
        return dc_qlookup[CommonUtils.clamp((short) (QIndex + Delta), (short) 0, (short) 127)];
    }

    static short ac_lookup(short QIndex, short Delta) {
        return ac_qlookup[CommonUtils.clamp((short) (QIndex + Delta), (short) 0, (short) 127)];
    }

    public static short doLookup(CommonData cm, Quant q, Comp c, short qindex) {
        return doLookup(q, c, qindex, cm.delta_q.get(q).get(c));
    }

    public static short doLookup(Quant q, Comp c, short qindex, short deltaQ) {
        return lookup.get(q).get(c).call(qindex, deltaQ);
    }
}
