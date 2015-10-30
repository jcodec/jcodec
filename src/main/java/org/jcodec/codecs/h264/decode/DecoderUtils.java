package org.jcodec.codecs.h264.decode;

import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DecoderUtils {
    public static void putMacroblock(Picture8Bit tgt, Picture8Bit decoded, int mbX, int mbY) {

        byte[] luma = tgt.getPlaneData(0);
        int stride = tgt.getPlaneWidth(0);

        byte[] cb = tgt.getPlaneData(1);
        byte[] cr = tgt.getPlaneData(2);
        int strideChroma = tgt.getPlaneWidth(1);

        int dOff = 0;
        for (int i = 0; i < 16; i++) {
            System.arraycopy(decoded.getPlaneData(0), dOff, luma, (mbY * 16 + i) * stride + mbX * 16, 16);
            dOff += 16;
        }
        for (int i = 0; i < 8; i++) {
            System.arraycopy(decoded.getPlaneData(1), i * 8, cb, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
        }
        for (int i = 0; i < 8; i++) {
            System.arraycopy(decoded.getPlaneData(2), i * 8, cr, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
        }
    }
}
