package org.jcodec.codecs.mpeg12;

import static org.jcodec.codecs.mpeg12.bitstream.PictureHeader.IntraCoded;
import static org.jcodec.codecs.mpeg12.bitstream.PictureHeader.PredictiveCoded;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceScalableExtension.SNR_SCALABILITY;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceScalableExtension.SPATIAL_SCALABILITY;

import org.jcodec.codecs.mpeg12.bitstream.SequenceScalableExtension;
import org.jcodec.common.io.VLC;
import org.jcodec.common.io.VLCBuilder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEGConst {

    public static final int PICTURE_START_CODE = 0x00;
    public static final int SLICE_START_CODE_FIRST = 0x01;
    public static final int SLICE_START_CODE_LAST = 0xAF;
    public static final int USER_DATA_START_CODE = 0xB2;
    public static final int SEQUENCE_HEADER_CODE = 0xB3;
    public static final int SEQUENCE_ERROR_CODE = 0xB4;
    public static final int EXTENSION_START_CODE = 0xB5;
    public static final int SEQUENCE_END_CODE = 0xB7;
    public static final int GROUP_START_CODE = 0xB8;

    public static VLC vlcAddressIncrement;
    public static VLC vlcMBTypeI;
    public static MBType[] mbTypeValI;
    public static VLC vlcMBTypeP;
    public static MBType[] mbTypeValP;
    public static VLC vlcMBTypeB;
    public static MBType[] mbTypeValB;

    public static VLC vlcMBTypeISpat;
    public static MBType[] mbTypeValISpat;
    public static VLC vlcMBTypePSpat;
    public static MBType[] mbTypeValPSpat;
    public static VLC vlcMBTypeBSpat;
    public static MBType[] mbTypeValBSpat;

    public static VLC vlcMBTypeSNR;
    public static MBType[] mbTypeValSNR;
    public static VLC vlcCBP;
    public static VLC vlcMotionCode;
    public static VLC vlcDualPrime;
    public static VLC vlcDCSizeLuma;
    public static VLC vlcDCSizeChroma;
    public static VLC vlcCoeff0;
    public static VLC vlcCoeff1;

    public static class MBType {
        public int macroblock_quant;
        public int macroblock_motion_forward;
        public int macroblock_motion_backward;
        public int macroblock_pattern;
        public int macroblock_intra;
        public int spatial_temporal_weight_code_flag;
        public int permitted_spatial_temporal_weight_classes;

        private MBType(int macroblock_quant, int macroblock_motion_forward, int macroblock_motion_backward,
                int macroblock_pattern, int macroblock_intra, int spatial_temporal_weight_code_flag,
                int permitted_spatial_temporal_weight_classes) {
            this.macroblock_quant = macroblock_quant;
            this.macroblock_motion_forward = macroblock_motion_forward;
            this.macroblock_motion_backward = macroblock_motion_backward;
            this.macroblock_pattern = macroblock_pattern;
            this.macroblock_intra = macroblock_intra;
            this.spatial_temporal_weight_code_flag = spatial_temporal_weight_code_flag;
            this.permitted_spatial_temporal_weight_classes = permitted_spatial_temporal_weight_classes;
        }
    }
    
    public static final int CODE_ESCAPE = 2049;
    public static final int CODE_END = 2048;

    static {
        vlcAddressIncrement = VLC
                .createVLC("1", "011", "010", "0011", "0010", "00011", "00010", "0000111", "0000110", "00001011", "00001010", "00001001", "00001000", "00000111", "00000110", "0000010111", "0000010110", "0000010101", "0000010100", "0000010011", "0000010010", "00000100011", "00000100010", "00000100001", "00000100000", "00000011111", "00000011110", "00000011101", "00000011100", "00000011011", "00000011010", "00000011001", "00000011000");

        vlcMBTypeI = VLC.createVLC("1", "01");
        mbTypeValI = new MBType[] { new MBType(0, 0, 0, 0, 1, 0, 0), new MBType(1, 0, 0, 0, 1, 0, 0) };

        vlcMBTypeP = VLC.createVLC("1", "01", "001", "00011", "00010", "00001", "000001");
        mbTypeValP = new MBType[] { new MBType(0, 1, 0, 1, 0, 0, 0), new MBType(0, 0, 0, 1, 0, 0, 0),
                new MBType(0, 1, 0, 0, 0, 0, 0), new MBType(0, 0, 0, 0, 1, 0, 0), new MBType(1, 1, 0, 1, 0, 0, 0),
                new MBType(1, 0, 0, 1, 0, 0, 0), new MBType(1, 0, 0, 0, 1, 0, 0) };

        vlcMBTypeB = VLC.createVLC("10", "11", "010", "011", "0010", "0011", "00011", "00010", "000011", "000010", "000001");
        mbTypeValB = new MBType[] { new MBType(0, 1, 1, 0, 0, 0, 0), new MBType(0, 1, 1, 1, 0, 0, 0),
                new MBType(0, 0, 1, 0, 0, 0, 0), new MBType(0, 0, 1, 1, 0, 0, 0), new MBType(0, 1, 0, 0, 0, 0, 0),
                new MBType(0, 1, 0, 1, 0, 0, 0), new MBType(0, 0, 0, 0, 1, 0, 0), new MBType(1, 1, 1, 1, 0, 0, 0),
                new MBType(1, 1, 0, 1, 0, 0, 0), new MBType(1, 0, 1, 1, 0, 0, 0), new MBType(1, 0, 0, 0, 1, 0, 0) };

        vlcMBTypeISpat = VLC.createVLC("1", "01", "0011", "0010", "0001");
        mbTypeValISpat = new MBType[] { new MBType(0, 0, 0, 0, 1, 0, 0), new MBType(1, 0, 0, 0, 1, 0, 0),
                new MBType(0, 0, 0, 0, 1, 0, 0), new MBType(1, 0, 0, 0, 1, 0, 0), new MBType(0, 0, 0, 0, 0, 0, 0) };

        vlcMBTypePSpat = VLC.createVLC("10", "011", "0000100", "000111", "0010", "0000111", "0011", "010", "000100", "0000110", "11", "000101", "000110", "0000101", "0000010", "0000011");
        mbTypeValPSpat = new MBType[] { new MBType(0, 1, 0, 1, 0, 0, 0), new MBType(0, 1, 0, 1, 0, 1, 0),
                new MBType(0, 0, 0, 1, 0, 0, 0), new MBType(0, 0, 0, 1, 0, 1, 0), new MBType(0, 1, 0, 0, 0, 0, 0),
                new MBType(0, 0, 0, 0, 1, 0, 0), new MBType(0, 1, 0, 0, 0, 1, 0), new MBType(1, 1, 0, 1, 0, 0, 0),
                new MBType(1, 0, 0, 1, 0, 0, 0), new MBType(1, 0, 0, 0, 1, 0, 0), new MBType(1, 1, 0, 1, 0, 1, 0),
                new MBType(1, 0, 0, 1, 0, 1, 0), new MBType(0, 0, 0, 0, 0, 1, 0), new MBType(0, 0, 0, 1, 0, 0, 0),
                new MBType(1, 0, 0, 1, 0, 0, 0), new MBType(0, 0, 0, 0, 0, 0, 0) };

        vlcMBTypeBSpat = VLC.createVLC("10", "11", "010", "011", "0010", "0011", "000110", "000111", "000100", "000101", "0000110", "0000111", "0000100", "0000101", "00000100", "00000101", "000001100", "000001110", "000001101", "000001111");
        mbTypeValBSpat = new MBType[] { new MBType(0, 1, 1, 0, 0, 0, 0), new MBType(0, 1, 1, 1, 0, 0, 0),
                new MBType(0, 0, 1, 0, 0, 0, 0), new MBType(0, 0, 1, 1, 0, 0, 0), new MBType(0, 1, 0, 0, 0, 0, 0),
                new MBType(0, 1, 0, 1, 0, 0, 0), new MBType(0, 0, 1, 0, 0, 1, 0), new MBType(0, 0, 1, 1, 0, 1, 0),
                new MBType(0, 1, 0, 0, 0, 1, 0), new MBType(0, 1, 0, 1, 0, 1, 0), new MBType(0, 0, 0, 0, 1, 0, 0),
                new MBType(1, 1, 1, 1, 0, 0, 0), new MBType(1, 1, 0, 1, 0, 0, 0), new MBType(1, 0, 1, 1, 0, 0, 0),
                new MBType(1, 0, 0, 0, 1, 0, 0), new MBType(1, 1, 0, 1, 0, 1, 0), new MBType(1, 0, 1, 1, 0, 1, 0),
                new MBType(0, 0, 0, 0, 0, 0, 0), new MBType(1, 0, 0, 1, 0, 0, 0), new MBType(0, 0, 0, 1, 0, 0, 0) };

        vlcMBTypeSNR = VLC.createVLC("1", "01", "001");
        mbTypeValSNR = new MBType[] { new MBType(0, 0, 0, 1, 0, 0, 0), new MBType(1, 0, 0, 1, 0, 0, 0),
                new MBType(0, 0, 0, 0, 0, 0, 0) };

        vlcCBP = VLC
                .createVLC("000000001", "01011", "01001", "001101", "1101", "0010111", "0010011", "00011111", "1100", "0010110", "0010010", "00011110", "10011", "00011011", "00010111", "00010011", "1011", "0010101", "0010001", "00011101", "10001", "00011001", "00010101", "00010001", "001111", "00001111", "00001101", "000000011", "01111", "00001011", "00000111", "000000111", "1010", "0010100", "0010000", "00011100", "001110", "00001110", "00001100", "000000010", "10000", "00011000", "00010100", "00010000", "01110", "00001010", "00000110", "000000110", "10010", "00011010", "00010110", "00010010", "01101", "00001001", "00000101", "000000101", "01100", "00001000", "00000100", "000000100", "111", "01010", "01000", "001100");

        vlcMotionCode = VLC.createVLC("1", "01", "001", "0001", "000011", "0000101", "0000100", "0000011", "000001011", "000001010", "000001001", "0000010001", "0000010000", "0000001111", "0000001110", "0000001101", "0000001100");

        vlcDualPrime = VLC.createVLC("11", "0", "10");

        vlcDCSizeLuma = VLC.createVLC("100", "00", "01", "101", "110", "1110", "11110", "111110", "1111110", "11111110", "111111110", "111111111");
        vlcDCSizeChroma = VLC.createVLC("00", "01", "10", "110", "1110", "11110", "111110", "1111110", "11111110", "111111110", "1111111110", "1111111111");

        VLCBuilder vlcCoeffBldr = new VLCBuilder();
        vlcCoeffBldr.set(CODE_ESCAPE, "000001");
        vlcCoeffBldr.set(CODE_END, "10");
        vlcCoeffBldr.set((0 << 6) | 1, "11");
        vlcCoeffBldr.set((1 << 6) | 1, "011");
        vlcCoeffBldr.set((0 << 6) | 2, "0100");
        vlcCoeffBldr.set((2 << 6) | 1, "0101");
        vlcCoeffBldr.set((0 << 6) | 3, "00101");
        vlcCoeffBldr.set((3 << 6) | 1, "00111");
        vlcCoeffBldr.set((4 << 6) | 1, "00110");
        vlcCoeffBldr.set((1 << 6) | 2, "000110");
        vlcCoeffBldr.set((5 << 6) | 1, "000111");
        vlcCoeffBldr.set((6 << 6) | 1, "000101");
        vlcCoeffBldr.set((7 << 6) | 1, "000100");
        vlcCoeffBldr.set((0 << 6) | 4, "0000110");
        vlcCoeffBldr.set((2 << 6) | 2, "0000100");
        vlcCoeffBldr.set((8 << 6) | 1, "0000111");
        vlcCoeffBldr.set((9 << 6) | 1, "0000101");
        vlcCoeffBldr.set((0 << 6) | 5, "00100110");
        vlcCoeffBldr.set((0 << 6) | 6, "00100001");
        vlcCoeffBldr.set((1 << 6) | 3, "00100101");
        vlcCoeffBldr.set((3 << 6) | 2, "00100100");
        vlcCoeffBldr.set((10 << 6) | 1, "00100111");
        vlcCoeffBldr.set((11 << 6) | 1, "00100011");
        vlcCoeffBldr.set((12 << 6) | 1, "00100010");
        vlcCoeffBldr.set((13 << 6) | 1, "00100000");
        vlcCoeffBldr.set((0 << 6) | 7, "0000001010");
        vlcCoeffBldr.set((1 << 6) | 4, "0000001100");
        vlcCoeffBldr.set((2 << 6) | 3, "0000001011");
        vlcCoeffBldr.set((4 << 6) | 2, "0000001111");
        vlcCoeffBldr.set((5 << 6) | 2, "0000001001");
        vlcCoeffBldr.set((14 << 6) | 1, "0000001110");
        vlcCoeffBldr.set((15 << 6) | 1, "0000001101");
        vlcCoeffBldr.set((16 << 6) | 1, "0000001000");
        vlcCoeffBldr.set((0 << 6) | 8, "000000011101");
        vlcCoeffBldr.set((0 << 6) | 9, "000000011000");
        vlcCoeffBldr.set((0 << 6) | 10, "000000010011");
        vlcCoeffBldr.set((0 << 6) | 11, "000000010000");
        vlcCoeffBldr.set((1 << 6) | 5, "000000011011");
        vlcCoeffBldr.set((2 << 6) | 4, "000000010100");
        vlcCoeffBldr.set((3 << 6) | 3, "000000011100");
        vlcCoeffBldr.set((4 << 6) | 3, "000000010010");
        vlcCoeffBldr.set((6 << 6) | 2, "000000011110");
        vlcCoeffBldr.set((7 << 6) | 2, "000000010101");
        vlcCoeffBldr.set((8 << 6) | 2, "000000010001");
        vlcCoeffBldr.set((17 << 6) | 1, "000000011111");
        vlcCoeffBldr.set((18 << 6) | 1, "000000011010");
        vlcCoeffBldr.set((19 << 6) | 1, "000000011001");
        vlcCoeffBldr.set((20 << 6) | 1, "000000010111");
        vlcCoeffBldr.set((21 << 6) | 1, "000000010110");
        vlcCoeffBldr.set((0 << 6) | 12, "0000000011010");
        vlcCoeffBldr.set((0 << 6) | 13, "0000000011001");
        vlcCoeffBldr.set((0 << 6) | 14, "0000000011000");
        vlcCoeffBldr.set((0 << 6) | 15, "0000000010111");
        vlcCoeffBldr.set((1 << 6) | 6, "0000000010110");
        vlcCoeffBldr.set((1 << 6) | 7, "0000000010101");
        vlcCoeffBldr.set((2 << 6) | 5, "0000000010100");
        vlcCoeffBldr.set((3 << 6) | 4, "0000000010011");
        vlcCoeffBldr.set((5 << 6) | 3, "0000000010010");
        vlcCoeffBldr.set((9 << 6) | 2, "0000000010001");
        vlcCoeffBldr.set((10 << 6) | 2, "0000000010000");
        vlcCoeffBldr.set((22 << 6) | 1, "0000000011111");
        vlcCoeffBldr.set((23 << 6) | 1, "0000000011110");
        vlcCoeffBldr.set((24 << 6) | 1, "0000000011101");
        vlcCoeffBldr.set((25 << 6) | 1, "0000000011100");
        vlcCoeffBldr.set((26 << 6) | 1, "0000000011011");
        vlcCoeffBldr.set((0 << 6) | 16, "00000000011111");
        vlcCoeffBldr.set((0 << 6) | 17, "00000000011110");
        vlcCoeffBldr.set((0 << 6) | 18, "00000000011101");
        vlcCoeffBldr.set((0 << 6) | 19, "00000000011100");
        vlcCoeffBldr.set((0 << 6) | 20, "00000000011011");
        vlcCoeffBldr.set((0 << 6) | 21, "00000000011010");
        vlcCoeffBldr.set((0 << 6) | 22, "00000000011001");
        vlcCoeffBldr.set((0 << 6) | 23, "00000000011000");
        vlcCoeffBldr.set((0 << 6) | 24, "00000000010111");
        vlcCoeffBldr.set((0 << 6) | 25, "00000000010110");
        vlcCoeffBldr.set((0 << 6) | 26, "00000000010101");
        vlcCoeffBldr.set((0 << 6) | 27, "00000000010100");
        vlcCoeffBldr.set((0 << 6) | 28, "00000000010011");
        vlcCoeffBldr.set((0 << 6) | 29, "00000000010010");
        vlcCoeffBldr.set((0 << 6) | 30, "00000000010001");
        vlcCoeffBldr.set((0 << 6) | 31, "00000000010000");
        vlcCoeffBldr.set((0 << 6) | 32, "000000000011000");
        vlcCoeffBldr.set((0 << 6) | 33, "000000000010111");
        vlcCoeffBldr.set((0 << 6) | 34, "000000000010110");
        vlcCoeffBldr.set((0 << 6) | 35, "000000000010101");
        vlcCoeffBldr.set((0 << 6) | 36, "000000000010100");
        vlcCoeffBldr.set((0 << 6) | 37, "000000000010011");
        vlcCoeffBldr.set((0 << 6) | 38, "000000000010010");
        vlcCoeffBldr.set((0 << 6) | 39, "000000000010001");
        vlcCoeffBldr.set((0 << 6) | 40, "000000000010000");
        vlcCoeffBldr.set((1 << 6) | 8, "000000000011111");
        vlcCoeffBldr.set((1 << 6) | 9, "000000000011110");
        vlcCoeffBldr.set((1 << 6) | 10, "000000000011101");
        vlcCoeffBldr.set((1 << 6) | 11, "000000000011100");
        vlcCoeffBldr.set((1 << 6) | 12, "000000000011011");
        vlcCoeffBldr.set((1 << 6) | 13, "000000000011010");
        vlcCoeffBldr.set((1 << 6) | 14, "000000000011001");
        vlcCoeffBldr.set((1 << 6) | 15, "0000000000010011");
        vlcCoeffBldr.set((1 << 6) | 16, "0000000000010010");
        vlcCoeffBldr.set((1 << 6) | 17, "0000000000010001");
        vlcCoeffBldr.set((1 << 6) | 18, "0000000000010000");
        vlcCoeffBldr.set((6 << 6) | 3, "0000000000010100");
        vlcCoeffBldr.set((11 << 6) | 2, "0000000000011010");
        vlcCoeffBldr.set((12 << 6) | 2, "0000000000011001");
        vlcCoeffBldr.set((13 << 6) | 2, "0000000000011000");
        vlcCoeffBldr.set((14 << 6) | 2, "0000000000010111");
        vlcCoeffBldr.set((15 << 6) | 2, "0000000000010110");
        vlcCoeffBldr.set((16 << 6) | 2, "0000000000010101");
        vlcCoeffBldr.set((27 << 6) | 1, "0000000000011111");
        vlcCoeffBldr.set((28 << 6) | 1, "0000000000011110");
        vlcCoeffBldr.set((29 << 6) | 1, "0000000000011101");
        vlcCoeffBldr.set((30 << 6) | 1, "0000000000011100");
        vlcCoeffBldr.set((31 << 6) | 1, "0000000000011011");
        vlcCoeff0 = vlcCoeffBldr.getVLC();

        vlcCoeffBldr = new VLCBuilder();
        vlcCoeffBldr.set(CODE_ESCAPE, "000001");
        vlcCoeffBldr.set(CODE_END, "0110");
        vlcCoeffBldr.set((0 << 6) | 1, "10");
        vlcCoeffBldr.set((1 << 6) | 1, "010");
        vlcCoeffBldr.set((0 << 6) | 2, "110");
        vlcCoeffBldr.set((2 << 6) | 1, "00101");
        vlcCoeffBldr.set((0 << 6) | 3, "0111");
        vlcCoeffBldr.set((3 << 6) | 1, "00111");
        vlcCoeffBldr.set((4 << 6) | 1, "000110");
        vlcCoeffBldr.set((1 << 6) | 2, "00110");
        vlcCoeffBldr.set((5 << 6) | 1, "000111");
        vlcCoeffBldr.set((6 << 6) | 1, "0000110");
        vlcCoeffBldr.set((7 << 6) | 1, "0000100");
        vlcCoeffBldr.set((0 << 6) | 4, "11100");
        vlcCoeffBldr.set((2 << 6) | 2, "0000111");
        vlcCoeffBldr.set((8 << 6) | 1, "0000101");
        vlcCoeffBldr.set((9 << 6) | 1, "1111000");
        vlcCoeffBldr.set((0 << 6) | 5, "11101");
        vlcCoeffBldr.set((0 << 6) | 6, "000101");
        vlcCoeffBldr.set((1 << 6) | 3, "1111001");
        vlcCoeffBldr.set((3 << 6) | 2, "00100110");
        vlcCoeffBldr.set((10 << 6) | 1, "1111010");
        vlcCoeffBldr.set((11 << 6) | 1, "00100001");
        vlcCoeffBldr.set((12 << 6) | 1, "00100101");
        vlcCoeffBldr.set((13 << 6) | 1, "00100100");
        vlcCoeffBldr.set((0 << 6) | 7, "000100");
        vlcCoeffBldr.set((1 << 6) | 4, "00100111");
        vlcCoeffBldr.set((2 << 6) | 3, "11111100");
        vlcCoeffBldr.set((4 << 6) | 2, "11111101");
        vlcCoeffBldr.set((5 << 6) | 2, "000000100");
        vlcCoeffBldr.set((14 << 6) | 1, "000000101");
        vlcCoeffBldr.set((15 << 6) | 1, "000000111");
        vlcCoeffBldr.set((16 << 6) | 1, "0000001101");
        vlcCoeffBldr.set((0 << 6) | 8, "1111011");
        vlcCoeffBldr.set((0 << 6) | 9, "1111100");
        vlcCoeffBldr.set((0 << 6) | 10, "00100011");
        vlcCoeffBldr.set((0 << 6) | 11, "00100010");
        vlcCoeffBldr.set((1 << 6) | 5, "00100000");
        vlcCoeffBldr.set((2 << 6) | 4, "0000001100");
        vlcCoeffBldr.set((3 << 6) | 3, "000000011100");
        vlcCoeffBldr.set((4 << 6) | 3, "000000010010");
        vlcCoeffBldr.set((6 << 6) | 2, "000000011110");
        vlcCoeffBldr.set((7 << 6) | 2, "000000010101");
        vlcCoeffBldr.set((8 << 6) | 2, "000000010001");
        vlcCoeffBldr.set((17 << 6) | 1, "000000011111");
        vlcCoeffBldr.set((18 << 6) | 1, "000000011010");
        vlcCoeffBldr.set((19 << 6) | 1, "000000011001");
        vlcCoeffBldr.set((20 << 6) | 1, "000000010111");
        vlcCoeffBldr.set((21 << 6) | 1, "000000010110");
        vlcCoeffBldr.set((0 << 6) | 12, "11111010");
        vlcCoeffBldr.set((0 << 6) | 13, "11111011");
        vlcCoeffBldr.set((0 << 6) | 14, "11111110");
        vlcCoeffBldr.set((0 << 6) | 15, "11111111");
        vlcCoeffBldr.set((1 << 6) | 6, "0000000010110");
        vlcCoeffBldr.set((1 << 6) | 7, "0000000010101");
        vlcCoeffBldr.set((2 << 6) | 5, "0000000010100");
        vlcCoeffBldr.set((3 << 6) | 4, "0000000010011");
        vlcCoeffBldr.set((5 << 6) | 3, "0000000010010");
        vlcCoeffBldr.set((9 << 6) | 2, "0000000010001");
        vlcCoeffBldr.set((10 << 6) | 2, "0000000010000");
        vlcCoeffBldr.set((22 << 6) | 1, "0000000011111");
        vlcCoeffBldr.set((23 << 6) | 1, "0000000011110");
        vlcCoeffBldr.set((24 << 6) | 1, "0000000011101");
        vlcCoeffBldr.set((25 << 6) | 1, "0000000011100");
        vlcCoeffBldr.set((26 << 6) | 1, "0000000011011");
        vlcCoeffBldr.set((0 << 6) | 16, "00000000011111");
        vlcCoeffBldr.set((0 << 6) | 17, "00000000011110");
        vlcCoeffBldr.set((0 << 6) | 18, "00000000011101");
        vlcCoeffBldr.set((0 << 6) | 19, "00000000011100");
        vlcCoeffBldr.set((0 << 6) | 20, "00000000011011");
        vlcCoeffBldr.set((0 << 6) | 21, "00000000011010");
        vlcCoeffBldr.set((0 << 6) | 22, "00000000011001");
        vlcCoeffBldr.set((0 << 6) | 23, "00000000011000");
        vlcCoeffBldr.set((0 << 6) | 24, "00000000010111");
        vlcCoeffBldr.set((0 << 6) | 25, "00000000010110");
        vlcCoeffBldr.set((0 << 6) | 26, "00000000010101");
        vlcCoeffBldr.set((0 << 6) | 27, "00000000010100");
        vlcCoeffBldr.set((0 << 6) | 28, "00000000010011");
        vlcCoeffBldr.set((0 << 6) | 29, "00000000010010");
        vlcCoeffBldr.set((0 << 6) | 30, "00000000010001");
        vlcCoeffBldr.set((0 << 6) | 31, "00000000010000");
        vlcCoeffBldr.set((0 << 6) | 32, "000000000011000");
        vlcCoeffBldr.set((0 << 6) | 33, "000000000010111");
        vlcCoeffBldr.set((0 << 6) | 34, "000000000010110");
        vlcCoeffBldr.set((0 << 6) | 35, "000000000010101");
        vlcCoeffBldr.set((0 << 6) | 36, "000000000010100");
        vlcCoeffBldr.set((0 << 6) | 37, "000000000010011");
        vlcCoeffBldr.set((0 << 6) | 38, "000000000010010");
        vlcCoeffBldr.set((0 << 6) | 39, "000000000010001");
        vlcCoeffBldr.set((0 << 6) | 40, "000000000010000");
        vlcCoeffBldr.set((1 << 6) | 8, "000000000011111");
        vlcCoeffBldr.set((1 << 6) | 9, "000000000011110");
        vlcCoeffBldr.set((1 << 6) | 10, "000000000011101");
        vlcCoeffBldr.set((1 << 6) | 11, "000000000011100");
        vlcCoeffBldr.set((1 << 6) | 12, "000000000011011");
        vlcCoeffBldr.set((1 << 6) | 13, "000000000011010");
        vlcCoeffBldr.set((1 << 6) | 14, "000000000011001");
        vlcCoeffBldr.set((1 << 6) | 15, "0000000000010011");
        vlcCoeffBldr.set((1 << 6) | 16, "0000000000010010");
        vlcCoeffBldr.set((1 << 6) | 17, "0000000000010001");
        vlcCoeffBldr.set((1 << 6) | 18, "0000000000010000");
        vlcCoeffBldr.set((6 << 6) | 3, "0000000000010100");
        vlcCoeffBldr.set((11 << 6) | 2, "0000000000011010");
        vlcCoeffBldr.set((12 << 6) | 2, "0000000000011001");
        vlcCoeffBldr.set((13 << 6) | 2, "0000000000011000");
        vlcCoeffBldr.set((14 << 6) | 2, "0000000000010111");
        vlcCoeffBldr.set((15 << 6) | 2, "0000000000010110");
        vlcCoeffBldr.set((16 << 6) | 2, "0000000000010101");
        vlcCoeffBldr.set((27 << 6) | 1, "0000000000011111");
        vlcCoeffBldr.set((28 << 6) | 1, "0000000000011110");
        vlcCoeffBldr.set((29 << 6) | 1, "0000000000011101");
        vlcCoeffBldr.set((30 << 6) | 1, "0000000000011100");
        vlcCoeffBldr.set((31 << 6) | 1, "0000000000011011");
        vlcCoeff1 = vlcCoeffBldr.getVLC();
    }

    public static int[] qScaleTab1 = new int[] { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36,
            38, 40, 42, 44, 46, 48, 50, 52, 54, 56, 58, 60, 62 };
    public static int[] qScaleTab2 = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36,
            40, 44, 48, 52, 56, 64, 72, 80, 88, 96, 104, 112 };

    public static int[] defaultQMatIntra = new int[] { 8, 16, 19, 22, 26, 27, 29, 34, 16, 16, 22, 24, 27, 29, 34, 37,
            19, 22, 26, 27, 29, 34, 34, 38, 22, 22, 26, 27, 29, 34, 37, 40, 22, 26, 27, 29, 32, 35, 40, 48, 26, 27, 29,
            32, 35, 40, 48, 58, 26, 27, 29, 34, 38, 46, 56, 69, 27, 29, 35, 38, 46, 56, 69, 83 };

    public static int[] defaultQMatInter = new int[] { 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 };
    
    public static int[][] scan = new int[][] {
            new int[] { 0, 1, 8, 16, 9, 2, 3, 10, 17, 24, 32, 25, 18, 11, 4, 5, 12, 19, 26, 33, 40, 48, 41, 34, 27, 20,
                    13, 6, 7, 14, 21, 28, 35, 42, 49, 56, 57, 50, 43, 36, 29, 22, 15, 23, 30, 37, 44, 51, 58, 59, 52,
                    45, 38, 31, 39, 46, 53, 60, 61, 54, 47, 55, 62, 63 },

            new int[] { 0, 8, 16, 24, 1, 9, 2, 10, 17, 25, 32, 40, 48, 56, 57, 49, 41, 33, 26, 18, 3, 11, 4, 12, 19,
                    27, 34, 42, 50, 58, 35, 43, 51, 59, 20, 28, 5, 13, 6, 14, 21, 29, 36, 44, 52, 60, 37, 45, 53, 61,
                    22, 30, 7, 15, 23, 31, 38, 46, 54, 62, 39, 47, 55, 63 } };

    public static int[] BLOCK_TO_CC = new int[] { 0, 0, 0, 0, 1, 2, 1, 2, 1, 2, 1, 2 };
    public static int[] BLOCK_POS_X = new int[] { 0, 8, 0, 8, 0, 0, 0, 0, 8, 8, 8, 8, 0, 0, 0, 0, 0, 8, 0, 8, 0, 0, 0,
            0, 8, 8, 8, 8 };
    public static int[] BLOCK_POS_Y = new int[] { 0, 0, 8, 8, 0, 0, 8, 8, 0, 0, 8, 8, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1,
            1, 0, 0, 1, 1 };
    public static int[] STEP_Y = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1 };
    public static int[] SQUEEZE_X = new int[] { 0, 1, 1, 0 };
    public static int[] SQUEEZE_Y = new int[] { 0, 1, 0, 0 };

    public static VLC vlcMBType(int picture_coding_type, SequenceScalableExtension sse) {
        if (sse != null && sse.scalable_mode == SNR_SCALABILITY) {
            return vlcMBTypeSNR;
        } else if (sse != null && sse.scalable_mode == SPATIAL_SCALABILITY) {
            return picture_coding_type == IntraCoded ? vlcMBTypeISpat
                    : (picture_coding_type == PredictiveCoded ? vlcMBTypePSpat : vlcMBTypeBSpat);
        } else {
            return picture_coding_type == IntraCoded ? vlcMBTypeI
                    : (picture_coding_type == PredictiveCoded ? vlcMBTypeP : vlcMBTypeB);
        }
    }

    public static MBType[] mbTypeVal(int picture_coding_type, SequenceScalableExtension sse) {
        if (sse != null && sse.scalable_mode == SNR_SCALABILITY) {
            return mbTypeValSNR;
        } else if (sse != null && sse.scalable_mode == SPATIAL_SCALABILITY) {
            return picture_coding_type == IntraCoded ? mbTypeValISpat
                    : (picture_coding_type == PredictiveCoded ? mbTypeValPSpat : mbTypeValBSpat);
        } else {
            return picture_coding_type == IntraCoded ? mbTypeValI
                    : (picture_coding_type == PredictiveCoded ? mbTypeValP : mbTypeValB);
        }
    }

    // public static void main(String[] args) {
    // for (int i = 0; i < 64; i++) {
    // for (int j = 0; j < 64; j++)
    // if (zigzagFrame[j] == i)
    // System.out.println(j + ",");
    // }
    // }
}
