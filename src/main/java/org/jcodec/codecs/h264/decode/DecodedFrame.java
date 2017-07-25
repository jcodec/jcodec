package org.jcodec.codecs.h264.decode;

import static java.lang.System.arraycopy;

import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A decoded frame as a collection of its macroblocks
 *  
 * @author Stan Vitvitskiy
 */
public class DecodedFrame {
    private int mbWidth;
    private int mbHeight;
    private DecodedMBlock[] decoded;
    
    public DecodedFrame(int mbWidth, int mbHeight) {
        this.mbWidth = mbWidth;
        this.mbHeight = mbHeight;
        this.decoded = new DecodedMBlock[mbWidth * mbHeight];
        for(int i = 0; i < mbWidth * mbHeight; i++) {
            decoded[i] = new DecodedMBlock();
        }
    }

    public void put(Frame tgt) {
        for (int mbY = 0; mbY < mbHeight; mbY++) {
            for (int mbX = 0; mbX < mbWidth; mbX++) {
                byte[] luma = tgt.getPlaneData(0);
                int stride = tgt.getPlaneWidth(0);

                byte[] cb = tgt.getPlaneData(1);
                byte[] cr = tgt.getPlaneData(2);
                int strideChroma = tgt.getPlaneWidth(1);

                int dOff = 0;
                DecodedMBlock dd = decoded[mbX + mbY * mbWidth];
                Picture8Bit mb = dd.mb;
                for (int i = 0; i < 16; i++) {
                    arraycopy(mb.getPlaneData(0), dOff, luma, (mbY * 16 + i) * stride + mbX * 16, 16);
                    dOff += 16;
                }
                for (int i = 0; i < 8; i++) {
                    arraycopy(mb.getPlaneData(1), i * 8, cb, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
                }
                for (int i = 0; i < 8; i++) {
                    arraycopy(mb.getPlaneData(2), i * 8, cr, (mbY * 8 + i) * strideChroma + mbX * 8, 8);
                }
                
                for (int blkY = 0; blkY < 4; blkY++) {
                    for (int blkX = 0; blkX < 4; blkX++) {
                        tgt.getMvs().setMv((mbX << 2) + blkX, (mbY << 2) + blkY, 0, dd.mvs.getMv(blkX, blkY, 0));
                        tgt.getMvs().setMv((mbX << 2) + blkX, (mbY << 2) + blkY, 1, dd.mvs.getMv(blkX, blkY, 1));
                    }
                }
                if (dd.refsUsed != null) {
                    tgt.getRefsUsed()[(mbY * mbWidth + mbX) << 1] = dd.refsUsed[0];
                    tgt.getRefsUsed()[((mbY * mbWidth + mbX) << 1) + 1] = dd.refsUsed[1];
                }
            }
        }
    }

    public DecodedMBlock getMb(int mbX, int mbY) {
        return decoded[mbWidth * mbY + mbX];
    }

    public int getMbWidth() {
        return mbWidth;
    }

    public int getMbHeight() {
        return mbHeight;
    }
}
