package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public enum MBType {

    I_NxN(true, 0),
    
    I_16x16(true, 1),
    
    I_PCM(true, 25),
    
    P_16x16(false, 0),
    
    P_16x8(false, 1),
    
    P_8x16(false, 2),
    
    P_8x8(false, 3),
    
    P_8x8ref0(false, 4),
    
    B_Direct_16x16(false, 0),
    
    B_L0_16x16(false, 1),
    
    B_L1_16x16(false, 2),
    
    B_Bi_16x16(false, 3),
    
    B_L0_L0_16x8(false, 4),
    
    B_L0_L0_8x16(false, 5),
    
    B_L1_L1_16x8(false, 6),
    
    B_L1_L1_8x16(false, 7),
    
    B_L0_L1_16x8(false, 8),
    
    B_L0_L1_8x16(false, 9),
    
    B_L1_L0_16x8(false, 10),
    
    B_L1_L0_8x16(false, 11),
    
    B_L0_Bi_16x8(false, 12),
    
    B_L0_Bi_8x16(false, 13),
    
    B_L1_Bi_16x8(false, 14),
    
    B_L1_Bi_8x16(false, 15),
    
    B_Bi_L0_16x8(false, 16),
    
    B_Bi_L0_8x16(false, 17),
    
    B_Bi_L1_16x8(false, 18),
    
    B_Bi_L1_8x16(false, 19),
    
    B_Bi_Bi_16x8(false, 20),
    
    B_Bi_Bi_8x16(false, 21),
    
    B_8x8(false, 22);

    public boolean intra;
    public int code;

    private MBType(boolean intra, int code) {
        this.intra = intra;
        this.code = code;
    }

    public boolean isIntra() {
        return intra;
    }

    public int code() {
        return code;
    }
}
