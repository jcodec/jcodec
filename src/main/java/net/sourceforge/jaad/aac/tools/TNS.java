package net.sourceforge.jaad.aac.tools;

import static net.sourceforge.jaad.aac.tools.TNSTables.TNS_TABLES;

import org.jcodec.common.io.BitReader;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.syntax.ICSInfo;
import net.sourceforge.jaad.aac.syntax.ICStream;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * Temporal Noise Shaping
 * 
 * @author in-somnia
 */
public class TNS {

    private static final int TNS_MAX_ORDER = 20;
    private static final int[] SHORT_BITS = { 1, 4, 3 }, LONG_BITS = { 2, 6, 5 };
    // bitstream
    private int[] nFilt;
    private int[][] length, order;
    private boolean[][] direction;
    private float[][][] coef;

    public TNS() {
        nFilt = new int[8];
        length = new int[8][4];
        direction = new boolean[8][4];
        order = new int[8][4];
        coef = new float[8][4][TNS_MAX_ORDER];
    }

    public void decode(BitReader _in, ICSInfo info) throws AACException {
        final int windowCount = info.getWindowCount();
        final int[] bits = info.isEightShortFrame() ? SHORT_BITS : LONG_BITS;

        int w, i, filt, coefLen, coefRes, coefCompress, tmp;
        for (w = 0; w < windowCount; w++) {
            if ((nFilt[w] = _in.readNBit(bits[0])) != 0) {
                coefRes = _in.read1Bit();

                for (filt = 0; filt < nFilt[w]; filt++) {
                    length[w][filt] = _in.readNBit(bits[1]);

                    if ((order[w][filt] = _in.readNBit(bits[2])) > 20)
                        throw new AACException("TNS filter out of range: " + order[w][filt]);
                    else if (order[w][filt] != 0) {
                        direction[w][filt] = _in.readBool();
                        coefCompress = _in.read1Bit();
                        coefLen = coefRes + 3 - coefCompress;
                        tmp = 2 * coefCompress + coefRes;

                        for (i = 0; i < order[w][filt]; i++) {
                            coef[w][filt][i] = TNS_TABLES[tmp][_in.readNBit(coefLen)];
                        }
                    }
                }
            }
        }
    }

    public void process(ICStream ics, float[] spec, SampleFrequency sf, boolean decode) {
        // TODO...
    }
}
