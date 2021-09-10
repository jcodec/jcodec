package org.jcodec.codecs.vpx.vp9;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Consts {

    public static final int KEY_FRAME = 0;
    public static final int INTER_FRAME = 1;

    /**
     * Specifies how the transform size is determined. For tx_mode not equal to
     * 4, the inverse transform will use the largest transform size possible up
     * to the limit set in tx_mode. For tx_mode equal to 4, the choice of size
     * is specified explicitly for each block.
     */
    public static final int ONLY_4X4 = 0;
    public static final int ALLOW_8X8 = 1;
    public static final int ALLOW_16X16 = 2;
    public static final int ALLOW_32X32 = 3;
    public static final int TX_MODE_SELECT = 4;

    public static final int PARTITION_NONE = 0;
    public static final int PARTITION_HORZ = 1;
    public static final int PARTITION_VERT = 2;
    public static final int PARTITION_SPLIT = 3;

    public static final int BLOCK_INVALID = -1;
    public static final int BLOCK_4X4 = 0;
    public static final int BLOCK_4X8 = 1;
    public static final int BLOCK_8X4 = 2;
    public static final int BLOCK_8X8 = 3;
    public static final int BLOCK_8X16 = 4;
    public static final int BLOCK_16X8 = 5;
    public static final int BLOCK_16X16 = 6;
    public static final int BLOCK_16X32 = 7;
    public static final int BLOCK_32X16 = 8;
    public static final int BLOCK_32X32 = 9;
    public static final int BLOCK_32X64 = 10;
    public static final int BLOCK_64X32 = 11;
    public static final int BLOCK_64X64 = 12;

    public static final int TX_4X4 = 0;
    public static final int TX_8X8 = 1;
    public static final int TX_16X16 = 2;
    public static final int TX_32X32 = 3;

    public static final int INTRA_FRAME = 0;
    public static final int LAST_FRAME = 1;
    public static final int ALTREF_FRAME = 2;
    public static final int GOLDEN_FRAME = 3;

    public static final int DC_PRED    = 0;
    public static final int V_PRED     = 1;
    public static final int H_PRED     = 2;
    public static final int D45_PRED   = 3;
    public static final int D135_PRED  = 4;
    public static final int D117_PRED  = 5;
    public static final int D153_PRED  = 6;
    public static final int D207_PRED  = 7;
    public static final int D63_PRED   = 8;
    public static final int TM_PRED    = 9;
    public static final int NEARESTMV  = 10;
    public static final int NEARMV     = 11;
    public static final int ZEROMV     = 12;
    public static final int NEWMV      = 13;

    public static final int SINGLE_REF = 0;
    public static final int COMPOUND_REF = 1;
    public static final int REFERENCE_MODE_SELECT = 2;

    public static final int NORMAL = 0;
    public static final int SMOOTH = 1;
    public static final int SHARP = 2;
    public static final int SWITCHABLE = 3;

    public static final int MV_JOINT_ZERO = 0;
    public static final int MV_JOINT_HNZVZ = 1;
    public static final int MV_JOINT_HZVNZ = 2;
    public static final int MV_JOINT_HNZVNZ = 3;

    public static final int ZERO_TOKEN = 0;
    public static final int ONE_TOKEN = 1;
    public static final int TWO_TOKEN = 2;
    public static final int THREE_TOKEN = 3;
    public static final int FOUR_TOKEN = 4;
    public static final int DCT_VAL_CAT1 = 5;
    public static final int DCT_VAL_CAT2 = 6;
    public static final int DCT_VAL_CAT3 = 7;
    public static final int DCT_VAL_CAT4 = 8;
    public static final int DCT_VAL_CAT5 = 9;
    public static final int DCT_VAL_CAT6 = 10;

    public static final short[] TREE_SEGMENT_ID = new short[] { 2, 4, 6, 8, 10, 12, 0, -1, -2, -3, -4, -5, -6, -7 };
    public static final short[][] TREE_TX_SIZE = new short[][] { null, { -TX_4X4, -TX_8X8 },
            { -TX_4X4, 2, -TX_8X8, -TX_16X16 }, { -TX_4X4, 2, -TX_8X8, 4, -TX_16X16, -TX_32X32 } };

    public static final int[] maxTxLookup = new int[] { 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3 };
    public static final int[] blW = new int[] { 1, 1, 1, 1, 1, 2, 2, 2, 4, 4, 4, 8, 8 };
    public static final int[] blH = new int[] { 1, 1, 1, 1, 2, 1, 2, 4, 2, 4, 8, 4, 8 };

    public static final short[] TREE_INTRA_MODE = new short[] { -DC_PRED, 2, -TM_PRED, 4, -V_PRED, 6, 8, 12, -H_PRED, 10,
            -D135_PRED, -D117_PRED, -D45_PRED, 14, -D63_PRED, 16, -D153_PRED, -D207_PRED };

    public static final int[] size_group_lookup = new int[] { 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3 };

    public static final short[] TREE_INTERP_FILTER = new short[] { -NORMAL, 2, -SMOOTH, -SHARP };

    public static final short[] TREE_INTER_MODE = new short[] { -(ZEROMV - NEARESTMV), 2, -(NEARESTMV - NEARESTMV), 4,
            -(NEARMV - NEARESTMV), -(NEWMV - NEARESTMV) };

    // 0 -> (-1,0); 1 -> (0,-1); 2 -> (-1,1); 3 -> (1,-1), 4 -> (-1, 3), 5 ->
    // (3, -1)
    public static final int[][] mv_ref_blocks_sm = new int[][] { { 0, 1 }, { 0, 1 }, { 0, 1 }, { 0, 1 }, { 1, 0 },
            { 0, 1 }, { 0, 1 }, { 1, 0 }, { 0, 1 }, { 2, 3 }, { 1, 0 }, { 0, 1 }, { 4, 5 } };

    // 6 -> (-1, 2), 7 -> (2, -1), 8 -> (-1, 4), 9 -> (4, -1), 10 -> (-1, 6), 11
    // -> (-1, -1), 12 -> (-2, 0)
    // 13 -> (0, -2), 14 -> (-3, 0), 15 -> (0, -3), 16 -> (-2, -1), 17 -> (-1,
    // -2), 18 -> (-2, -2), 19 -> (-3, -3)
    public static final int[][] mv_ref_blocks = new int[][] { { 0, 1, 11, 12, 13, 16, 17, 18 },
            { 0, 1, 11, 12, 13, 16, 17, 18 }, { 0, 1, 11, 12, 13, 16, 17, 18 }, { 0, 1, 11, 12, 13, 16, 17, 18 },
            { 1, 0, 3, 11, 13, 12, 16, 17 }, { 0, 1, 2, 11, 12, 13, 17, 16 }, { 0, 1, 2, 3, 11, 14, 15, 19 },
            { 1, 0, 7, 11, 2, 15, 14, 19 }, { 0, 1, 6, 11, 3, 14, 15, 19 }, { 2, 3, 6, 7, 11, 14, 15, 19 },
            { 1, 0, 9, 6, 11, 15, 14, 7 }, { 0, 1, 8, 7, 11, 14, 15, 6 }, { 4, 5, 8, 9, 11, 0, 1, 10 } };

    public static final short[] TREE_MV_JOINT = new short[] { -MV_JOINT_ZERO, 2, -MV_JOINT_HNZVZ, 4, -MV_JOINT_HZVNZ,
            -MV_JOINT_HNZVNZ };

    public static final short[] MV_CLASS_TREE = new short[] { -0, 2, -1, 4, 6, 8, -2, -3, 10, 12, -4, -5, -6, 14, 16, 18,
            -7, -8, -9, -10, };

    public static final short[] MV_FR_TREE = new short[] { -0, 2, -1, 4, -2, -3 };

    public static final int[] LITERAL_TO_FILTER_TYPE = new int[] { SMOOTH, NORMAL, SHARP, SWITCHABLE };

    public static final short[][] PARETO_TABLE = {
        { 3, 86, 128, 6, 86, 23, 88, 29 },
        { 6, 86, 128, 11, 87, 42, 91, 52 },
        { 9, 86, 129, 17, 88, 61, 94, 76 },
        { 12, 86, 129, 22, 88, 77, 97, 93 },
        { 15, 87, 129, 28, 89, 93, 100, 110 },
        { 17, 87, 129, 33, 90, 105, 103, 123 },
        { 20, 88, 130, 38, 91, 118, 106, 136 },
        { 23, 88, 130, 43, 91, 128, 108, 146 },
        { 26, 89, 131, 48, 92, 139, 111, 156 },
        { 28, 89, 131, 53, 93, 147, 114, 163 },
        { 31, 90, 131, 58, 94, 156, 117, 171 },
        { 34, 90, 131, 62, 94, 163, 119, 177 },
        { 37, 90, 132, 66, 95, 171, 122, 184 },
        { 39, 90, 132, 70, 96, 177, 124, 189 },
        { 42, 91, 132, 75, 97, 183, 127, 194 },
        { 44, 91, 132, 79, 97, 188, 129, 198 },
        { 47, 92, 133, 83, 98, 193, 132, 202 },
        { 49, 92, 133, 86, 99, 197, 134, 205 },
        { 52, 93, 133, 90, 100, 201, 137, 208 },
        { 54, 93, 133, 94, 100, 204, 139, 211 },
        { 57, 94, 134, 98, 101, 208, 142, 214 },
        { 59, 94, 134, 101, 102, 211, 144, 216 },
        { 62, 94, 135, 105, 103, 214, 146, 218 },
        { 64, 94, 135, 108, 103, 216, 148, 220 },
        { 66, 95, 135, 111, 104, 219, 151, 222 },
        { 68, 95, 135, 114, 105, 221, 153, 223 },
        { 71, 96, 136, 117, 106, 224, 155, 225 },
        { 73, 96, 136, 120, 106, 225, 157, 226 },
        { 76, 97, 136, 123, 107, 227, 159, 228 },
        { 78, 97, 136, 126, 108, 229, 160, 229 },
        { 80, 98, 137, 129, 109, 231, 162, 231 },
        { 82, 98, 137, 131, 109, 232, 164, 232 },
        { 84, 98, 138, 134, 110, 234, 166, 233 },
        { 86, 98, 138, 137, 111, 235, 168, 234 },
        { 89, 99, 138, 140, 112, 236, 170, 235 },
        { 91, 99, 138, 142, 112, 237, 171, 235 },
        { 93, 100, 139, 145, 113, 238, 173, 236 },
        { 95, 100, 139, 147, 114, 239, 174, 237 },
        { 97, 101, 140, 149, 115, 240, 176, 238 },
        { 99, 101, 140, 151, 115, 241, 177, 238 },
        { 101, 102, 140, 154, 116, 242, 179, 239 },
        { 103, 102, 140, 156, 117, 242, 180, 239 },
        { 105, 103, 141, 158, 118, 243, 182, 240 },
        { 107, 103, 141, 160, 118, 243, 183, 240 },
        { 109, 104, 141, 162, 119, 244, 185, 241 },
        { 111, 104, 141, 164, 119, 244, 186, 241 },
        { 113, 104, 142, 166, 120, 245, 187, 242 },
        { 114, 104, 142, 168, 121, 245, 188, 242 },
        { 116, 105, 143, 170, 122, 246, 190, 243 },
        { 118, 105, 143, 171, 122, 246, 191, 243 },
        { 120, 106, 143, 173, 123, 247, 192, 244 },
        { 121, 106, 143, 175, 124, 247, 193, 244 },
        { 123, 107, 144, 177, 125, 248, 195, 244 },
        { 125, 107, 144, 178, 125, 248, 196, 244 },
        { 127, 108, 145, 180, 126, 249, 197, 245 },
        { 128, 108, 145, 181, 127, 249, 198, 245 },
        { 130, 109, 145, 183, 128, 249, 199, 245 },
        { 132, 109, 145, 184, 128, 249, 200, 245 },
        { 134, 110, 146, 186, 129, 250, 201, 246 },
        { 135, 110, 146, 187, 130, 250, 202, 246 },
        { 137, 111, 147, 189, 131, 251, 203, 246 },
        { 138, 111, 147, 190, 131, 251, 204, 246 },
        { 140, 112, 147, 192, 132, 251, 205, 247 },
        { 141, 112, 147, 193, 132, 251, 206, 247 },
        { 143, 113, 148, 194, 133, 251, 207, 247 },
        { 144, 113, 148, 195, 134, 251, 207, 247 },
        { 146, 114, 149, 197, 135, 252, 208, 248 },
        { 147, 114, 149, 198, 135, 252, 209, 248 },
        { 149, 115, 149, 199, 136, 252, 210, 248 },
        { 150, 115, 149, 200, 137, 252, 210, 248 },
        { 152, 115, 150, 201, 138, 252, 211, 248 },
        { 153, 115, 150, 202, 138, 252, 212, 248 },
        { 155, 116, 151, 204, 139, 253, 213, 249 },
        { 156, 116, 151, 205, 139, 253, 213, 249 },
        { 158, 117, 151, 206, 140, 253, 214, 249 },
        { 159, 117, 151, 207, 141, 253, 215, 249 },
        { 161, 118, 152, 208, 142, 253, 216, 249 },
        { 162, 118, 152, 209, 142, 253, 216, 249 },
        { 163, 119, 153, 210, 143, 253, 217, 249 },
        { 164, 119, 153, 211, 143, 253, 217, 249 },
        { 166, 120, 153, 212, 144, 254, 218, 250 },
        { 167, 120, 153, 212, 145, 254, 219, 250 },
        { 168, 121, 154, 213, 146, 254, 220, 250 },
        { 169, 121, 154, 214, 146, 254, 220, 250 },
        { 171, 122, 155, 215, 147, 254, 221, 250 },
        { 172, 122, 155, 216, 147, 254, 221, 250 },
        { 173, 123, 155, 217, 148, 254, 222, 250 },
        { 174, 123, 155, 217, 149, 254, 222, 250 },
        { 176, 124, 156, 218, 150, 254, 223, 250 },
        { 177, 124, 156, 219, 150, 254, 223, 250 },
        { 178, 125, 157, 220, 151, 254, 224, 251 },
        { 179, 125, 157, 220, 151, 254, 224, 251 },
        { 180, 126, 157, 221, 152, 254, 225, 251 },
        { 181, 126, 157, 221, 152, 254, 225, 251 },
        { 183, 127, 158, 222, 153, 254, 226, 251 },
        { 184, 127, 158, 223, 154, 254, 226, 251 },
        { 185, 128, 159, 224, 155, 255, 227, 251 },
        { 186, 128, 159, 224, 155, 255, 227, 251 },
        { 187, 129, 160, 225, 156, 255, 228, 251 },
        { 188, 130, 160, 225, 156, 255, 228, 251 },
        { 189, 131, 160, 226, 157, 255, 228, 251 },
        { 190, 131, 160, 226, 158, 255, 228, 251 },
        { 191, 132, 161, 227, 159, 255, 229, 251 },
        { 192, 132, 161, 227, 159, 255, 229, 251 },
        { 193, 133, 162, 228, 160, 255, 230, 252 },
        { 194, 133, 162, 229, 160, 255, 230, 252 },
        { 195, 134, 163, 230, 161, 255, 231, 252 },
        { 196, 134, 163, 230, 161, 255, 231, 252 },
        { 197, 135, 163, 231, 162, 255, 231, 252 },
        { 198, 135, 163, 231, 162, 255, 231, 252 },
        { 199, 136, 164, 232, 163, 255, 232, 252 },
        { 200, 136, 164, 232, 164, 255, 232, 252 },
        { 201, 137, 165, 233, 165, 255, 233, 252 },
        { 201, 137, 165, 233, 165, 255, 233, 252 },
        { 202, 138, 166, 233, 166, 255, 233, 252 },
        { 203, 138, 166, 233, 166, 255, 233, 252 },
        { 204, 139, 166, 234, 167, 255, 234, 252 },
        { 205, 139, 166, 234, 167, 255, 234, 252 },
        { 206, 140, 167, 235, 168, 255, 235, 252 },
        { 206, 140, 167, 235, 168, 255, 235, 252 },
        { 207, 141, 168, 236, 169, 255, 235, 252 },
        { 208, 141, 168, 236, 170, 255, 235, 252 },
        { 209, 142, 169, 237, 171, 255, 236, 252 },
        { 209, 143, 169, 237, 171, 255, 236, 252 },
        { 210, 144, 169, 237, 172, 255, 236, 252 },
        { 211, 144, 169, 237, 172, 255, 236, 252 },
        { 212, 145, 170, 238, 173, 255, 237, 252 },
        { 213, 145, 170, 238, 173, 255, 237, 252 },
        { 214, 146, 171, 239, 174, 255, 237, 253 },
        { 214, 146, 171, 239, 174, 255, 237, 253 },
        { 215, 147, 172, 240, 175, 255, 238, 253 },
        { 215, 147, 172, 240, 175, 255, 238, 253 },
        { 216, 148, 173, 240, 176, 255, 238, 253 },
        { 217, 148, 173, 240, 176, 255, 238, 253 },
        { 218, 149, 173, 241, 177, 255, 239, 253 },
        { 218, 149, 173, 241, 178, 255, 239, 253 },
        { 219, 150, 174, 241, 179, 255, 239, 253 },
        { 219, 151, 174, 241, 179, 255, 239, 253 },
        { 220, 152, 175, 242, 180, 255, 240, 253 },
        { 221, 152, 175, 242, 180, 255, 240, 253 },
        { 222, 153, 176, 242, 181, 255, 240, 253 },
        { 222, 153, 176, 242, 181, 255, 240, 253 },
        { 223, 154, 177, 243, 182, 255, 240, 253 },
        { 223, 154, 177, 243, 182, 255, 240, 253 },
        { 224, 155, 178, 244, 183, 255, 241, 253 },
        { 224, 155, 178, 244, 183, 255, 241, 253 },
        { 225, 156, 178, 244, 184, 255, 241, 253 },
        { 225, 157, 178, 244, 184, 255, 241, 253 },
        { 226, 158, 179, 244, 185, 255, 242, 253 },
        { 227, 158, 179, 244, 185, 255, 242, 253 },
        { 228, 159, 180, 245, 186, 255, 242, 253 },
        { 228, 159, 180, 245, 186, 255, 242, 253 },
        { 229, 160, 181, 245, 187, 255, 242, 253 },
        { 229, 160, 181, 245, 187, 255, 242, 253 },
        { 230, 161, 182, 246, 188, 255, 243, 253 },
        { 230, 162, 182, 246, 188, 255, 243, 253 },
        { 231, 163, 183, 246, 189, 255, 243, 253 },
        { 231, 163, 183, 246, 189, 255, 243, 253 },
        { 232, 164, 184, 247, 190, 255, 243, 253 },
        { 232, 164, 184, 247, 190, 255, 243, 253 },
        { 233, 165, 185, 247, 191, 255, 244, 253 },
        { 233, 165, 185, 247, 191, 255, 244, 253 },
        { 234, 166, 185, 247, 192, 255, 244, 253 },
        { 234, 167, 185, 247, 192, 255, 244, 253 },
        { 235, 168, 186, 248, 193, 255, 244, 253 },
        { 235, 168, 186, 248, 193, 255, 244, 253 },
        { 236, 169, 187, 248, 194, 255, 244, 253 },
        { 236, 169, 187, 248, 194, 255, 244, 253 },
        { 236, 170, 188, 248, 195, 255, 245, 253 },
        { 236, 170, 188, 248, 195, 255, 245, 253 },
        { 237, 171, 189, 249, 196, 255, 245, 254 },
        { 237, 172, 189, 249, 196, 255, 245, 254 },
        { 238, 173, 190, 249, 197, 255, 245, 254 },
        { 238, 173, 190, 249, 197, 255, 245, 254 },
        { 239, 174, 191, 249, 198, 255, 245, 254 },
        { 239, 174, 191, 249, 198, 255, 245, 254 },
        { 240, 175, 192, 249, 199, 255, 246, 254 },
        { 240, 176, 192, 249, 199, 255, 246, 254 },
        { 240, 177, 193, 250, 200, 255, 246, 254 },
        { 240, 177, 193, 250, 200, 255, 246, 254 },
        { 241, 178, 194, 250, 201, 255, 246, 254 },
        { 241, 178, 194, 250, 201, 255, 246, 254 },
        { 242, 179, 195, 250, 202, 255, 246, 254 },
        { 242, 180, 195, 250, 202, 255, 246, 254 },
        { 242, 181, 196, 250, 203, 255, 247, 254 },
        { 242, 181, 196, 250, 203, 255, 247, 254 },
        { 243, 182, 197, 251, 204, 255, 247, 254 },
        { 243, 183, 197, 251, 204, 255, 247, 254 },
        { 244, 184, 198, 251, 205, 255, 247, 254 },
        { 244, 184, 198, 251, 205, 255, 247, 254 },
        { 244, 185, 199, 251, 206, 255, 247, 254 },
        { 244, 185, 199, 251, 206, 255, 247, 254 },
        { 245, 186, 200, 251, 207, 255, 247, 254 },
        { 245, 187, 200, 251, 207, 255, 247, 254 },
        { 246, 188, 201, 252, 207, 255, 248, 254 },
        { 246, 188, 201, 252, 207, 255, 248, 254 },
        { 246, 189, 202, 252, 208, 255, 248, 254 },
        { 246, 190, 202, 252, 208, 255, 248, 254 },
        { 247, 191, 203, 252, 209, 255, 248, 254 },
        { 247, 191, 203, 252, 209, 255, 248, 254 },
        { 247, 192, 204, 252, 210, 255, 248, 254 },
        { 247, 193, 204, 252, 210, 255, 248, 254 },
        { 248, 194, 205, 252, 211, 255, 248, 254 },
        { 248, 194, 205, 252, 211, 255, 248, 254 },
        { 248, 195, 206, 252, 212, 255, 249, 254 },
        { 248, 196, 206, 252, 212, 255, 249, 254 },
        { 249, 197, 207, 253, 213, 255, 249, 254 },
        { 249, 197, 207, 253, 213, 255, 249, 254 },
        { 249, 198, 208, 253, 214, 255, 249, 254 },
        { 249, 199, 209, 253, 214, 255, 249, 254 },
        { 250, 200, 210, 253, 215, 255, 249, 254 },
        { 250, 200, 210, 253, 215, 255, 249, 254 },
        { 250, 201, 211, 253, 215, 255, 249, 254 },
        { 250, 202, 211, 253, 215, 255, 249, 254 },
        { 250, 203, 212, 253, 216, 255, 249, 254 },
        { 250, 203, 212, 253, 216, 255, 249, 254 },
        { 251, 204, 213, 253, 217, 255, 250, 254 },
        { 251, 205, 213, 253, 217, 255, 250, 254 },
        { 251, 206, 214, 254, 218, 255, 250, 254 },
        { 251, 206, 215, 254, 218, 255, 250, 254 },
        { 252, 207, 216, 254, 219, 255, 250, 254 },
        { 252, 208, 216, 254, 219, 255, 250, 254 },
        { 252, 209, 217, 254, 220, 255, 250, 254 },
        { 252, 210, 217, 254, 220, 255, 250, 254 },
        { 252, 211, 218, 254, 221, 255, 250, 254 },
        { 252, 212, 218, 254, 221, 255, 250, 254 },
        { 253, 213, 219, 254, 222, 255, 250, 254 },
        { 253, 213, 220, 254, 222, 255, 250, 254 },
        { 253, 214, 221, 254, 223, 255, 250, 254 },
        { 253, 215, 221, 254, 223, 255, 250, 254 },
        { 253, 216, 222, 254, 224, 255, 251, 254 },
        { 253, 217, 223, 254, 224, 255, 251, 254 },
        { 253, 218, 224, 254, 225, 255, 251, 254 },
        { 253, 219, 224, 254, 225, 255, 251, 254 },
        { 254, 220, 225, 254, 225, 255, 251, 254 },
        { 254, 221, 226, 254, 225, 255, 251, 254 },
        { 254, 222, 227, 255, 226, 255, 251, 254 },
        { 254, 223, 227, 255, 226, 255, 251, 254 },
        { 254, 224, 228, 255, 227, 255, 251, 254 },
        { 254, 225, 229, 255, 227, 255, 251, 254 },
        { 254, 226, 230, 255, 228, 255, 251, 254 },
        { 254, 227, 230, 255, 229, 255, 251, 254 },
        { 255, 228, 231, 255, 230, 255, 251, 254 },
        { 255, 229, 232, 255, 230, 255, 251, 254 },
        { 255, 230, 233, 255, 231, 255, 252, 254 },
        { 255, 231, 234, 255, 231, 255, 252, 254 },
        { 255, 232, 235, 255, 232, 255, 252, 254 },
        { 255, 233, 236, 255, 232, 255, 252, 254 },
        { 255, 235, 237, 255, 233, 255, 252, 254 },
        { 255, 236, 238, 255, 234, 255, 252, 254 },
        { 255, 238, 240, 255, 235, 255, 252, 255 },
        { 255, 239, 241, 255, 235, 255, 252, 254 },
        { 255, 241, 243, 255, 236, 255, 252, 254 },
        { 255, 243, 245, 255, 237, 255, 252, 254 },
        { 255, 246, 247, 255, 239, 255, 253, 255 }
    };

    public static final int[][] extra_bits = new int[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, 0, 2 }, { 0, 0, 3 },
            { 0, 0, 4 }, { 1, 1, 5 }, { 2, 2, 7 }, { 3, 3, 11 }, { 4, 4, 19 }, { 5, 5, 35 }, { 6, 14, 67 } };

    public static final int[][] cat_probs = new int[][] { { 0 }, { 159 }, { 165, 145 }, { 173, 148, 140 },
            { 176, 155, 140, 135 }, { 180, 157, 141, 134, 130 },
            { 254, 254, 254, 252, 249, 243, 230, 196, 177, 153, 140, 133, 130, 129 } };

    public static final short[] TOKEN_TREE = new short[] { 2, 6, -TWO_TOKEN, 4,
            -THREE_TOKEN, -FOUR_TOKEN, 8, 10, -DCT_VAL_CAT1, -DCT_VAL_CAT2, 12, 14, -DCT_VAL_CAT3, -DCT_VAL_CAT4,
            -DCT_VAL_CAT5, -DCT_VAL_CAT6 };

    public static final int[] coefband_4x4 = new int[] { 0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5 };

    public static final int[] coefband_8x8plus = new int[] { 0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4,
            4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
            5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 };

    public static final int SZ_8x8 = 0;
    public static final int SZ_16x16 = 1;
    public static final int SZ_32x32 = 2;
    public static final int SZ_64x64 = 3;

    public static final int[][] blSizeLookup_ = new int[][] {
            { BLOCK_4X4, BLOCK_4X8, BLOCK_8X4, BLOCK_8X8, BLOCK_8X16, BLOCK_16X8, BLOCK_16X16, BLOCK_16X32,
                    BLOCK_32X16, BLOCK_32X32, BLOCK_32X64, BLOCK_64X32, BLOCK_64X64, },
            { -1, -1, -1, BLOCK_8X4, -1, -1, BLOCK_16X8, -1, -1, BLOCK_32X16, -1, -1, BLOCK_64X32, },
            { -1, -1, -1, BLOCK_4X8, -1, 1, BLOCK_8X16, -1, -1, BLOCK_16X32, -1, -1, BLOCK_32X64, },
            { -1, -1, -1, BLOCK_4X4, -1, -1, BLOCK_8X8, -1, -1, BLOCK_16X16, -1, -1, BLOCK_32X32, } };
            
            
    public static final int[][] blSizeLookup = new int[][] {
                { BLOCK_4X4,  BLOCK_4X8},
                { BLOCK_8X4,  BLOCK_8X8, BLOCK_8X16},
                { -1, BLOCK_16X8, BLOCK_16X16, BLOCK_16X32 },
                { -1, -1, BLOCK_32X16, BLOCK_32X32 }
                };
    public static final int[] sub8x8PartitiontoBlockType = new int[] {BLOCK_8X8, BLOCK_8X4, BLOCK_4X8, BLOCK_4X4};

    public static final short[] TREE_PARTITION = new short[] { -PARTITION_NONE, 2, -PARTITION_HORZ, 4, -PARTITION_VERT,
            -PARTITION_SPLIT };
    public static final int[] TREE_PARTITION_RIGHT_E = new int[] { -PARTITION_NONE, -PARTITION_VERT };
    public static final int[] TREE_PARTITION_BOTTOM_E = new int[] { -PARTITION_NONE, -PARTITION_HORZ };

    public static final int[] INV_REMAP_TABLE = { 7, 20, 33, 46, 59, 72, 85, 98, 111, 124, 137, 150, 163, 176, 189,
            202, 215, 228, 241, 254, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 48, 49, 50, 51, 52, 53,
            54, 55, 56, 57, 58, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82,
            83, 84, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108,
            109, 110, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 125, 126, 127, 128, 129, 130, 131,
            132, 133, 134, 135, 136, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 151, 152, 153, 154,
            155, 156, 157, 158, 159, 160, 161, 162, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 177,
            178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199,
            200, 201, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 216, 217, 218, 219, 220, 221, 222,
            223, 224, 225, 226, 227, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 242, 243, 244, 245,
            246, 247, 248, 249, 250, 251, 252, 253, 253 };

    // Each inter frame can use up to 3 frames for reference
    public static final int REFS_PER_FRAME = 3;
    // Number of values that can be decoded for mv_fr
    public static final int MV_FR_SIZE = 4;
    // Number of positions to search in motion vector prediction
    public static final int MVREF_NEIGHBOURS = 8;
    // Number of contexts when decoding intra_mode
    public static final int BLOCK_SIZE_GROUPS = 4;
    // Number of different block sizes used
    public static final int BLOCK_SIZES = 13;
    // Number of contexts when decoding partition
    public static final int PARTITION_CONTEXTS = 16;
    // Smallest size of a mode info block
    public static final int MI_SIZE = 8;
    // Minimum width of a tile in units of superblocks (although
    // tiles on the right hand edge can be narrower)
    public static final int MIN_TILE_WIDTH_B64 = 4;
    // Maximum width of a tile in units of superblocks
    public static final int MAX_TILE_WIDTH_B64 = 64;
    // Number of motion vectors returned by find_mv_refs process
    public static final int MAX_MV_REF_CANDIDATES = 2;
    // Number of frames that can be stored for future reference
    public static final int NUM_REF_FRAMES = 8;
    // Number of values that can be derived for ref_frame
    public static final int MAX_REF_FRAMES = 4;
    // Number of contexts for is_inter
    public static final int IS_INTER_CONTEXTS = 4;
    // Number of contexts for comp_mode
    public static final int COMP_MODE_CONTEXTS = 5;
    // Number of contexts for single_ref and comp_ref
    public static final int REF_CONTEXTS = 5;
    // Number of segments allowed in segmentation map
    public static final int MAX_SEGMENTS = 8;
    // Index for quantizer segment feature
    public static final int SEG_LVL_ALT_Q = 0;
    // Index for loop filter segment feature
    public static final int SEG_LVL_ALT_L = 1;
    // Index for reference frame segment feature
    public static final int SEG_LVL_REF_FRAME = 2;
    // Index for skip segment feature
    public static final int SEG_LVL_SKIP = 3;
    // Number of segment features
    public static final int SEG_LVL_MAX = 4;
    // Number of different plane types (Y or UV)
    public static final int BLOCK_TYPES = 2;
    // Number of different prediction types (intra or inter)
    public static final int REF_TYPES = 2;
    // Number of coefficient bands
    public static final int COEF_BANDS = 6;
    // Number of contexts for decoding coefficients
    public static final int PREV_COEF_CONTEXTS = 6;
    // Number of coefficient probabilities that are directly
    // transmitted
    public static final int UNCONSTRAINED_NODES = 3;
    // Number of contexts for transform size
    public static final int TX_SIZE_CONTEXTS = 2;
    // Number of values for interp_filter
    public static final int SWITCHABLE_FILTERS = 3;
    // Number of contexts for interp_filter
    public static final int INTERP_FILTER_CONTEXTS = 4;
    // Number of contexts for decoding skip
    public static final int SKIP_CONTEXTS = 3;
    // Number of values for partition
    public static final int PARTITION_TYPES = 4;
    // Number of values for tx_size
    public static final int TX_SIZES = 4;
    // Number of values for tx_mode
    public static final int TX_MODES = 5;
    // Inverse transform rows with DCT and columns with DCT
    public static final int DCT_DCT = 0;
    // Inverse transform rows with DCT and columns with ADST
    public static final int ADST_DCT = 1;
    // Inverse transform rows with ADST and columns with DCT
    public static final int DCT_ADST = 2;
    // Inverse transform rows with ADST and columns with ADST
    public static final int ADST_ADST = 3;
    // Number of values for y_mode
    public static final int MB_MODE_COUNT = 14;
    // Number of values for intra_mode
    public static final int INTRA_MODES = 10;
    // Number of values for inter_mode
    public static final int INTER_MODES = 4;
    // Number of contexts for inter_mode
    public static final int INTER_MODE_CONTEXTS = 7;
    // Number of values for mv_joint
    public static final int MV_JOINTS = 4;
    // Number of values for mv_class
    public static final int MV_CLASSES = 11;
    // Number of values for mv_class0_bit
    public static final int CLASS0_SIZE = 2;
    // Maximum number of bits for decoding motion vectors
    public static final int MV_OFFSET_BITS = 10;
    // Number of values allowed for a probability adjustment
    public static final int MAX_PROB = 255;
    // Number of different mode types for loop filtering
    public static final int MAX_MODE_LF_DELTAS = 2;
    // Threshold at which motion vectors are considered large
    public static final int COMPANDED_MVREF_THRESH = 8;
    // Maximum value used for loop filtering
    public static final int MAX_LOOP_FILTER = 63;
    // Number of bits of precision when scaling reference frames
    public static final int REF_SCALE_SHIFT = 14;
    // Number of bits of precision when performing inter prediction
    public static final int SUBPEL_BITS = 4;
    // 1 << SUBPEL_BITS
    public static final int SUBPEL_SHIFTS = 16;
    // SUBPEL_SHIFTS - 1
    public static final int SUBPEL_MASK = 15;
    // Value used when clipping motion vectors
    public static final int MV_BORDER = 128;
    // Value used when clipping motion vectors
    public static final int INTERP_EXTEND = 4;
    // Value used when clipping motion vectors
    public static final int BORDERINPIXELS = 160;
    // Value used in adapting probabilities
    public static final int MAX_UPDATE_FACTOR = 128;
    // Value used in adapting probabilities
    public static final int COUNT_SAT = 20;
    // Both candidates use ZEROMV
    public static final int BOTH_ZERO = 0;
    // One candidate uses ZEROMV, one uses NEARMV or NEARESTMV
    public static final int ZERO_PLUS_PREDICTED = 1;
    // Both candidates use NEARMV or NEARESTMV
    public static final int BOTH_PREDICTED = 2;
    // One candidate uses NEWMV, one uses ZEROMV
    public static final int NEW_PLUS_NON_INTRA = 3;
    // Both candidates use NEWMV
    public static final int BOTH_NEW = 4;
    // One candidate uses intra prediction, one uses inter prediction
    public static final int INTRA_PLUS_NON_INTRA = 5;
    // Both candidates use intra prediction
    public static final int BOTH_INTRA = 6;
    // Sentinel value marking a case that can never occur
    public static final int INVALID_CASE = 9;

    // Unknown (in this case the color space must be signaled outside the VP9
    // bitstream).
    int CS_UNKNOWN = 0;
    // Rec. ITU-R BT.601-7
    public static final int CS_BT_601 = 1;
    // Rec. ITU-R BT.709-6
    public static final int CS_BT_709 = 2;
    // SMPTE-170
    public static final int CS_SMPTE_170 = 3;
    // SMPTE-240
    public static final int CS_SMPTE_240 = 4;
    // Rec. ITU-R BT.2020-2
    public static final int CS_BT_2020 = 5;
    // Reserved
    public static final int CS_RESERVED = 6;
    // sRGB (IEC 61966-2-1)
    public static final int CS_RGB = 7;

    public static final int[] SEGMENTATION_FEATURE_BITS = { 8, 6, 2, 0 };
    public static final int[] SEGMENTATION_FEATURE_SIGNED = { 1, 1, 0, 0 };

    public static final int[] tx_mode_to_biggest_tx_size = { TX_4X4, TX_8X8, TX_16X16, TX_32X32, TX_32X32 };
    
    
    public static final int[] intra_mode_to_tx_type_lookup = {
        DCT_DCT,    // DC
        ADST_DCT,   // V
        DCT_ADST,   // H
        DCT_DCT,    // D45
        ADST_ADST,  // D135
        ADST_DCT,   // D117
        DCT_ADST,   // D153
        DCT_ADST,   // D207
        ADST_DCT,   // D63
        ADST_ADST,  // TM
    };
    
    public static final int CAT1_MIN_VAL = 5;
    public static final int CAT2_MIN_VAL = 7;
    public static final int CAT3_MIN_VAL = 11;
    public static final int CAT4_MIN_VAL = 19;
    public static final int CAT5_MIN_VAL = 35;
    public static final int CAT6_MIN_VAL = 67;
    
    public static final int[] catMinVal = { CAT1_MIN_VAL, CAT2_MIN_VAL, CAT3_MIN_VAL, CAT4_MIN_VAL, CAT5_MIN_VAL,
            CAT6_MIN_VAL };
    
    public static final int[][][][] uv_txsize_lookup = {
            {
                // BLOCK_4X4
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
            },
            {
                // BLOCK_4X8
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
            },
            {
                // BLOCK_8X4
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
            },
            {
                // BLOCK_8X8
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_4X4 }, { TX_4X4, TX_4X4 } },
            },
            {
                // BLOCK_8X16
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_4X4, TX_4X4 } },
            },
            {
                // BLOCK_16X8
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_4X4 }, { TX_8X8, TX_4X4 } },
                { { TX_8X8, TX_4X4 }, { TX_8X8, TX_8X8 } },
                { { TX_8X8, TX_4X4 }, { TX_8X8, TX_8X8 } },
            },
            {
                // BLOCK_16X16
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_8X8 }, { TX_8X8, TX_8X8 } },
            },
            {
                // BLOCK_16X32
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_16X16 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_16X16 }, { TX_8X8, TX_8X8 } },
            },
            {
                // BLOCK_32X16
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_8X8 }, { TX_16X16, TX_8X8 } },
                { { TX_16X16, TX_8X8 }, { TX_16X16, TX_8X8 } },
            },
            {
                // BLOCK_32X32
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_16X16 }, { TX_16X16, TX_16X16 } },
                { { TX_32X32, TX_16X16 }, { TX_16X16, TX_16X16 } },
            },
            {
                // BLOCK_32X64
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_16X16 }, { TX_16X16, TX_16X16 } },
                { { TX_32X32, TX_32X32 }, { TX_16X16, TX_16X16 } },
            },
            {
                // BLOCK_64X32
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_16X16 }, { TX_16X16, TX_16X16 } },
                { { TX_32X32, TX_16X16 }, { TX_32X32, TX_16X16 } },
            },
            {
                // BLOCK_64X64
                { { TX_4X4, TX_4X4 }, { TX_4X4, TX_4X4 } },
                { { TX_8X8, TX_8X8 }, { TX_8X8, TX_8X8 } },
                { { TX_16X16, TX_16X16 }, { TX_16X16, TX_16X16 } },
                { { TX_32X32, TX_32X32 }, { TX_32X32, TX_32X32 } },
            },
          };
}
