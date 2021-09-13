package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.*;

import java.nio.ByteBuffer;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;
import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DecodingContext {

    private int profile;
    private int showExistingFrame;
    private int frameToShowMapIdx;
    private int frameType;
    private int showFrame;
    private int errorResilientMode;
    private int refreshFrameFlags;
    private int frameIsIntra;
    private int intraOnly;
    private int resetFrameContext;
    private int colorSpace;
    int subsamplingX;
    int subsamplingY;
    int bitDepth;
    int frameWidth;
    int frameHeight;
    private int renderWidth;
    private int renderHeight;
    private int[] refFrameWidth = new int[MAX_REF_FRAMES];
    private int[] refFrameHeight = new int[MAX_REF_FRAMES];
    private int[] refFrameIdx = new int[3];
    private int[] refFrameSignBias = new int[3];
    private int allowHighPrecisionMv;
    int interpFilter;
    private int frameParallelDecodingMode;
    private int refreshFrameContext;
    private int frameContextIdx;
    private int[] loopFilterRefDeltas = new int[4];
    private int[] loopFilterModeDeltas = new int[2];
    private int baseQIdx;
    private int deltaQYDc;
    private int deltaQUvDc;
    private int deltaQUvAc;
    private boolean lossless;
    private boolean segmentationEnabled;
    private short[] segmentationTreeProbs = new short[7];
    private int[] segmentationPredProbs = new int[3];
    private int[][] featureEnabled = new int[MAX_SEGMENTS][SEG_LVL_MAX];
    private int[][] featureData = new int[MAX_SEGMENTS][SEG_LVL_MAX];
    private int tileRowsLog2;
    private int tileColsLog2;
    int txMode;
    private int compFixedRef;
    private int compVarRef0;
    private int compVarRef1;

    int refMode;

    short[][] tx8x8Probs = new short[TX_SIZE_CONTEXTS][TX_SIZES - 3];
    short[][] tx16x16Probs = new short[TX_SIZE_CONTEXTS][TX_SIZES - 2];
    short[][] tx32x32Probs = new short[TX_SIZE_CONTEXTS][TX_SIZES - 1];
    short[][][][][][] coefProbs;
    private short[] skipProbs = new short[SKIP_CONTEXTS];
    short[][] interModeProbs = new short[INTER_MODE_CONTEXTS][INTER_MODES - 1];
    short[][] interpFilterProbs = new short[INTERP_FILTER_CONTEXTS][SWITCHABLE_FILTERS - 1];
    private short[] isInterProbs = new short[IS_INTER_CONTEXTS];

    private short[] compModeProbs = new short[COMP_MODE_CONTEXTS];
    private short[][] singleRefProbs = new short[REF_CONTEXTS][2];
    private short[] compRefProbs = new short[REF_CONTEXTS];

    short[][] yModeProbs = new short[BLOCK_SIZE_GROUPS][INTRA_MODES - 1];
    short[][] partitionProbs = new short[PARTITION_CONTEXTS][PARTITION_TYPES - 1];

    public short[][] uvModeProbs = new short[INTRA_MODES][INTRA_MODES - 1];

    private short[] mvJointProbs = new short[MV_JOINTS - 1];
    private short[] mvSignProbs = new short[2];
    private short[][] mvClassProbs = new short[2][MV_CLASSES - 1];
    private short[] mvClass0BitProbs = new short[2];
    private short[][] mvBitsProbs = new short[2][MV_OFFSET_BITS];
    private short[][][] mvClass0FrProbs = new short[2][CLASS0_SIZE][MV_FR_SIZE - 1];
    private short[][] mvFrProbs = new short[2][MV_FR_SIZE - 1];
    private short[] mvClass0HpProb = new short[2];
    private short[] mvHpProbs = new short[2];
    private int filterLevel;
    private int sharpnessLevel;
    int[] leftPartitionSizes;
    int[] abovePartitionSizes;
    int tileHeight;
    int tileWidth;
    boolean[] leftSkipped;
    boolean[] aboveSkipped;
    int[][] aboveNonzeroContext;
    int[][] leftNonzeroContext;
    int[] aboveModes;
    int[] leftModes;
    private int colorRange;
    int[] aboveRefs;
    int[] leftRefs;
    int[] leftInterpFilters;
    int[] aboveInterpFilters;
    int miTileStartCol;
    int[] leftTxSizes;
    int[] aboveTxSizes;
    boolean[] leftCompound;
    boolean[] aboveCompound;

    private static final short[] defaultSkipProb = { 192, 128, 64 };

    private static final short[][] defaultTxProbs8x8 = { { 100 }, { 66 } };
    private static final short[][] defaultTxProbs16x16 = { { 20, 152 }, { 15, 101 } };
    private static final short[][] defaultTxProbs32x32 = { { 3, 136, 37 }, { 5, 52, 13 } };
    public static final short[][][][][][] defaultCoefProbs = { { { /* block Type 0 */
            { /* Intra */
                    { /* Coeff Band 0 */
                            { 195, 29, 183 }, { 84, 49, 136 }, { 8, 42, 71 }, { 0, 0, 0 }, // unused
                            { 0, 0, 0 }, // unused
                            { 0, 0, 0 } // unused
                    }, { /* Coeff Band 1 */
                            { 31, 107, 169 }, { 35, 99, 159 }, { 17, 82, 140 }, { 8, 66, 114 }, { 2, 44, 76 },
                            { 1, 19, 32 } },
                    { /* Coeff Band 2 */
                            { 40, 132, 201 }, { 29, 114, 187 }, { 13, 91, 157 }, { 7, 75, 127 }, { 3, 58, 95 },
                            { 1, 28, 47 } },
                    { /* Coeff Band 3 */
                            { 69, 142, 221 }, { 42, 122, 201 }, { 15, 91, 159 }, { 6, 67, 121 }, { 1, 42, 77 },
                            { 1, 17, 31 } },
                    { /* Coeff Band 4 */
                            { 102, 148, 228 }, { 67, 117, 204 }, { 17, 82, 154 }, { 6, 59, 114 }, { 2, 39, 75 },
                            { 1, 15, 29 } },
                    { /* Coeff Band 5 */
                            { 156, 57, 233 }, { 119, 57, 212 }, { 58, 48, 163 }, { 29, 40, 124 }, { 12, 30, 81 },
                            { 3, 12, 31 } } },
            { /* Inter */
                    { /* Coeff Band 0 */
                            { 191, 107, 226 }, { 124, 117, 204 }, { 25, 99, 155 }, { 0, 0, 0 }, // unused
                            { 0, 0, 0 }, // unused
                            { 0, 0, 0 } // unused
                    }, { /* Coeff Band 1 */
                            { 29, 148, 210 }, { 37, 126, 194 }, { 8, 93, 157 }, { 2, 68, 118 }, { 1, 39, 69 },
                            { 1, 17, 33 } },
                    { /* Coeff Band 2 */
                            { 41, 151, 213 }, { 27, 123, 193 }, { 3, 82, 144 }, { 1, 58, 105 }, { 1, 32, 60 },
                            { 1, 13, 26 } },
                    { /* Coeff Band 3 */
                            { 59, 159, 220 }, { 23, 126, 198 }, { 4, 88, 151 }, { 1, 66, 114 }, { 1, 38, 71 },
                            { 1, 18, 34 } },
                    { /* Coeff Band 4 */
                            { 114, 136, 232 }, { 51, 114, 207 }, { 11, 83, 155 }, { 3, 56, 105 }, { 1, 33, 65 },
                            { 1, 17, 34 } },
                    { /* Coeff Band 5 */
                            { 149, 65, 234 }, { 121, 57, 215 }, { 61, 49, 166 }, { 28, 36, 114 }, { 12, 25, 76 },
                            { 3, 16, 42 } } } },
            { /* block Type 1 */
                    { /* Intra */
                            { /* Coeff Band 0 */
                                    { 214, 49, 220 }, { 132, 63, 188 }, { 42, 65, 137 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 85, 137, 221 }, { 104, 131, 216 }, { 49, 111, 192 }, { 21, 87, 155 },
                                    { 2, 49, 87 }, { 1, 16, 28 } },
                            { /* Coeff Band 2 */
                                    { 89, 163, 230 }, { 90, 137, 220 }, { 29, 100, 183 }, { 10, 70, 135 },
                                    { 2, 42, 81 }, { 1, 17, 33 } },
                            { /* Coeff Band 3 */
                                    { 108, 167, 237 }, { 55, 133, 222 }, { 15, 97, 179 }, { 4, 72, 135 }, { 1, 45, 85 },
                                    { 1, 19, 38 } },
                            { /* Coeff Band 4 */
                                    { 124, 146, 240 }, { 66, 124, 224 }, { 17, 88, 175 }, { 4, 58, 122 }, { 1, 36, 75 },
                                    { 1, 18, 37 } },
                            { /* Coeff Band 5 */
                                    { 141, 79, 241 }, { 126, 70, 227 }, { 66, 58, 182 }, { 30, 44, 136 },
                                    { 12, 34, 96 }, { 2, 20, 47 } } },
                    { /* Inter */
                            { /* Coeff Band 0 */
                                    { 229, 99, 249 }, { 143, 111, 235 }, { 46, 109, 192 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 82, 158, 236 }, { 94, 146, 224 }, { 25, 117, 191 }, { 9, 87, 149 }, { 3, 56, 99 },
                                    { 1, 33, 57 } },
                            { /* Coeff Band 2 */
                                    { 83, 167, 237 }, { 68, 145, 222 }, { 10, 103, 177 }, { 2, 72, 131 }, { 1, 41, 79 },
                                    { 1, 20, 39 } },
                            { /* Coeff Band 3 */
                                    { 99, 167, 239 }, { 47, 141, 224 }, { 10, 104, 178 }, { 2, 73, 133 }, { 1, 44, 85 },
                                    { 1, 22, 47 } },
                            { /* Coeff Band 4 */
                                    { 127, 145, 243 }, { 71, 129, 228 }, { 17, 93, 177 }, { 3, 61, 124 }, { 1, 41, 84 },
                                    { 1, 21, 52 } },
                            { /* Coeff Band 5 */
                                    { 157, 78, 244 }, { 140, 72, 231 }, { 69, 58, 184 }, { 31, 44, 137 },
                                    { 14, 38, 105 }, { 8, 23, 61 } } } } },
            { { /* block Type 0 */
                    { /* Intra */
                            { /* Coeff Band 0 */
                                    { 125, 34, 187 }, { 52, 41, 133 }, { 6, 31, 56 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 37, 109, 153 }, { 51, 102, 147 }, { 23, 87, 128 }, { 8, 67, 101 }, { 1, 41, 63 },
                                    { 1, 19, 29 } },
                            { /* Coeff Band 2 */
                                    { 31, 154, 185 }, { 17, 127, 175 }, { 6, 96, 145 }, { 2, 73, 114 }, { 1, 51, 82 },
                                    { 1, 28, 45 } },
                            { /* Coeff Band 3 */
                                    { 23, 163, 200 }, { 10, 131, 185 }, { 2, 93, 148 }, { 1, 67, 111 }, { 1, 41, 69 },
                                    { 1, 14, 24 } },
                            { /* Coeff Band 4 */
                                    { 29, 176, 217 }, { 12, 145, 201 }, { 3, 101, 156 }, { 1, 69, 111 }, { 1, 39, 63 },
                                    { 1, 14, 23 } },
                            { /* Coeff Band 5 */
                                    { 57, 192, 233 }, { 25, 154, 215 }, { 6, 109, 167 }, { 3, 78, 118 }, { 1, 48, 69 },
                                    { 1, 21, 29 } } },
                    { /* Inter */
                            { /* Coeff Band 0 */
                                    { 202, 105, 245 }, { 108, 106, 216 }, { 18, 90, 144 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 33, 172, 219 }, { 64, 149, 206 }, { 14, 117, 177 }, { 5, 90, 141 }, { 2, 61, 95 },
                                    { 1, 37, 57 } },
                            { /* Coeff Band 2 */
                                    { 33, 179, 220 }, { 11, 140, 198 }, { 1, 89, 148 }, { 1, 60, 104 }, { 1, 33, 57 },
                                    { 1, 12, 21 } },
                            { /* Coeff Band 3 */
                                    { 30, 181, 221 }, { 8, 141, 198 }, { 1, 87, 145 }, { 1, 58, 100 }, { 1, 31, 55 },
                                    { 1, 12, 20 } },
                            { /* Coeff Band 4 */
                                    { 32, 186, 224 }, { 7, 142, 198 }, { 1, 86, 143 }, { 1, 58, 100 }, { 1, 31, 55 },
                                    { 1, 12, 22 } },
                            { /* Coeff Band 5 */
                                    { 57, 192, 227 }, { 20, 143, 204 }, { 3, 96, 154 }, { 1, 68, 112 }, { 1, 42, 69 },
                                    { 1, 19, 32 } } } },
                    { /* block Type 1 */
                            { /* Intra */
                                    { /* Coeff Band 0 */
                                            { 212, 35, 215 }, { 113, 47, 169 }, { 29, 48, 105 }, { 0, 0, 0 }, // unused
                                            { 0, 0, 0 }, // unused
                                            { 0, 0, 0 } // unused
                                    }, { /* Coeff Band 1 */
                                            { 74, 129, 203 }, { 106, 120, 203 }, { 49, 107, 178 }, { 19, 84, 144 },
                                            { 4, 50, 84 }, { 1, 15, 25 } },
                                    { /* Coeff Band 2 */
                                            { 71, 172, 217 }, { 44, 141, 209 }, { 15, 102, 173 }, { 6, 76, 133 },
                                            { 2, 51, 89 }, { 1, 24, 42 } },
                                    { /* Coeff Band 3 */
                                            { 64, 185, 231 }, { 31, 148, 216 }, { 8, 103, 175 }, { 3, 74, 131 },
                                            { 1, 46, 81 }, { 1, 18, 30 } },
                                    { /* Coeff Band 4 */
                                            { 65, 196, 235 }, { 25, 157, 221 }, { 5, 105, 174 }, { 1, 67, 120 },
                                            { 1, 38, 69 }, { 1, 15, 30 } },
                                    { /* Coeff Band 5 */
                                            { 65, 204, 238 }, { 30, 156, 224 }, { 7, 107, 177 }, { 2, 70, 124 },
                                            { 1, 42, 73 }, { 1, 18, 34 } } },
                            { /* Inter */
                                    { /* Coeff Band 0 */
                                            { 225, 86, 251 }, { 144, 104, 235 }, { 42, 99, 181 }, { 0, 0, 0 }, // unused
                                            { 0, 0, 0 }, // unused
                                            { 0, 0, 0 } // unused
                                    }, { /* Coeff Band 1 */
                                            { 85, 175, 239 }, { 112, 165, 229 }, { 29, 136, 200 }, { 12, 103, 162 },
                                            { 6, 77, 123 }, { 2, 53, 84 } },
                                    { /* Coeff Band 2 */
                                            { 75, 183, 239 }, { 30, 155, 221 }, { 3, 106, 171 }, { 1, 74, 128 },
                                            { 1, 44, 76 }, { 1, 17, 28 } },
                                    { /* Coeff Band 3 */
                                            { 73, 185, 240 }, { 27, 159, 222 }, { 2, 107, 172 }, { 1, 75, 127 },
                                            { 1, 42, 73 }, { 1, 17, 29 } },
                                    { /* Coeff Band 4 */
                                            { 62, 190, 238 }, { 21, 159, 222 }, { 2, 107, 172 }, { 1, 72, 122 },
                                            { 1, 40, 71 }, { 1, 18, 32 } },
                                    { /* Coeff Band 5 */
                                            { 61, 199, 240 }, { 27, 161, 226 }, { 4, 113, 180 }, { 1, 76, 129 },
                                            { 1, 46, 80 }, { 1, 23, 41 } } } } },
            { { /* block Type 0 */
                    { /* Intra */
                            { /* Coeff Band 0 */
                                    { 7, 27, 153 }, { 5, 30, 95 }, { 1, 16, 30 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 50, 75, 127 }, { 57, 75, 124 }, { 27, 67, 108 }, { 10, 54, 86 }, { 1, 33, 52 },
                                    { 1, 12, 18 } },
                            { /* Coeff Band 2 */
                                    { 43, 125, 151 }, { 26, 108, 148 }, { 7, 83, 122 }, { 2, 59, 89 }, { 1, 38, 60 },
                                    { 1, 17, 27 } },
                            { /* Coeff Band 3 */
                                    { 23, 144, 163 }, { 13, 112, 154 }, { 2, 75, 117 }, { 1, 50, 81 }, { 1, 31, 51 },
                                    { 1, 14, 23 } },
                            { /* Coeff Band 4 */
                                    { 18, 162, 185 }, { 6, 123, 171 }, { 1, 78, 125 }, { 1, 51, 86 }, { 1, 31, 54 },
                                    { 1, 14, 23 } },
                            { /* Coeff Band 5 */
                                    { 15, 199, 227 }, { 3, 150, 204 }, { 1, 91, 146 }, { 1, 55, 95 }, { 1, 30, 53 },
                                    { 1, 11, 20 } } },
                    { /* Inter */
                            { /* Coeff Band 0 */
                                    { 19, 55, 240 }, { 19, 59, 196 }, { 3, 52, 105 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 41, 166, 207 }, { 104, 153, 199 }, { 31, 123, 181 }, { 14, 101, 152 },
                                    { 5, 72, 106 }, { 1, 36, 52 } },
                            { /* Coeff Band 2 */
                                    { 35, 176, 211 }, { 12, 131, 190 }, { 2, 88, 144 }, { 1, 60, 101 }, { 1, 36, 60 },
                                    { 1, 16, 28 } },
                            { /* Coeff Band 3 */
                                    { 28, 183, 213 }, { 8, 134, 191 }, { 1, 86, 142 }, { 1, 56, 96 }, { 1, 30, 53 },
                                    { 1, 12, 20 } },
                            { /* Coeff Band 4 */
                                    { 20, 190, 215 }, { 4, 135, 192 }, { 1, 84, 139 }, { 1, 53, 91 }, { 1, 28, 49 },
                                    { 1, 11, 20 } },
                            { /* Coeff Band 5 */
                                    { 13, 196, 216 }, { 2, 137, 192 }, { 1, 86, 143 }, { 1, 57, 99 }, { 1, 32, 56 },
                                    { 1, 13, 24 } } } },
                    { /* block Type 1 */
                            { /* Intra */
                                    { /* Coeff Band 0 */
                                            { 211, 29, 217 }, { 96, 47, 156 }, { 22, 43, 87 }, { 0, 0, 0 }, // unused
                                            { 0, 0, 0 }, // unused
                                            { 0, 0, 0 } // unused
                                    }, { /* Coeff Band 1 */
                                            { 78, 120, 193 }, { 111, 116, 186 }, { 46, 102, 164 }, { 15, 80, 128 },
                                            { 2, 49, 76 }, { 1, 18, 28 } },
                                    { /* Coeff Band 2 */
                                            { 71, 161, 203 }, { 42, 132, 192 }, { 10, 98, 150 }, { 3, 69, 109 },
                                            { 1, 44, 70 }, { 1, 18, 29 } },
                                    { /* Coeff Band 3 */
                                            { 57, 186, 211 }, { 30, 140, 196 }, { 4, 93, 146 }, { 1, 62, 102 },
                                            { 1, 38, 65 }, { 1, 16, 27 } },
                                    { /* Coeff Band 4 */
                                            { 47, 199, 217 }, { 14, 145, 196 }, { 1, 88, 142 }, { 1, 57, 98 },
                                            { 1, 36, 62 }, { 1, 15, 26 } },
                                    { /* Coeff Band 5 */
                                            { 26, 219, 229 }, { 5, 155, 207 }, { 1, 94, 151 }, { 1, 60, 104 },
                                            { 1, 36, 62 }, { 1, 16, 28 } } },
                            { /* Inter */
                                    { /* Coeff Band 0 */
                                            { 233, 29, 248 }, { 146, 47, 220 }, { 43, 52, 140 }, { 0, 0, 0 }, // unused
                                            { 0, 0, 0 }, // unused
                                            { 0, 0, 0 } // unused
                                    }, { /* Coeff Band 1 */
                                            { 100, 163, 232 }, { 179, 161, 222 }, { 63, 142, 204 }, { 37, 113, 174 },
                                            { 26, 89, 137 }, { 18, 68, 97 } },
                                    { /* Coeff Band 2 */
                                            { 85, 181, 230 }, { 32, 146, 209 }, { 7, 100, 164 }, { 3, 71, 121 },
                                            { 1, 45, 77 }, { 1, 18, 30 } },
                                    { /* Coeff Band 3 */
                                            { 65, 187, 230 }, { 20, 148, 207 }, { 2, 97, 159 }, { 1, 68, 116 },
                                            { 1, 40, 70 }, { 1, 14, 29 } },
                                    { /* Coeff Band 4 */
                                            { 40, 194, 227 }, { 8, 147, 204 }, { 1, 94, 155 }, { 1, 65, 112 },
                                            { 1, 39, 66 }, { 1, 14, 26 } },
                                    { /* Coeff Band 5 */
                                            { 16, 208, 228 }, { 3, 151, 207 }, { 1, 98, 160 }, { 1, 67, 117 },
                                            { 1, 41, 74 }, { 1, 17, 31 } } } } },
            { { /* block Type 0 */
                    { /* Intra */
                            { /* Coeff Band 0 */
                                    { 17, 38, 140 }, { 7, 34, 80 }, { 1, 17, 29 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 37, 75, 128 }, { 41, 76, 128 }, { 26, 66, 116 }, { 12, 52, 94 }, { 2, 32, 55 },
                                    { 1, 10, 16 } },
                            { /* Coeff Band 2 */
                                    { 50, 127, 154 }, { 37, 109, 152 }, { 16, 82, 121 }, { 5, 59, 85 }, { 1, 35, 54 },
                                    { 1, 13, 20 } },
                            { /* Coeff Band 3 */
                                    { 40, 142, 167 }, { 17, 110, 157 }, { 2, 71, 112 }, { 1, 44, 72 }, { 1, 27, 45 },
                                    { 1, 11, 17 } },
                            { /* Coeff Band 4 */
                                    { 30, 175, 188 }, { 9, 124, 169 }, { 1, 74, 116 }, { 1, 48, 78 }, { 1, 30, 49 },
                                    { 1, 11, 18 } },
                            { /* Coeff Band 5 */
                                    { 10, 222, 223 }, { 2, 150, 194 }, { 1, 83, 128 }, { 1, 48, 79 }, { 1, 27, 45 },
                                    { 1, 11, 17 } } },
                    { /* Inter */
                            { /* Coeff Band 0 */
                                    { 36, 41, 235 }, { 29, 36, 193 }, { 10, 27, 111 }, { 0, 0, 0 }, // unused
                                    { 0, 0, 0 }, // unused
                                    { 0, 0, 0 } // unused
                            }, { /* Coeff Band 1 */
                                    { 85, 165, 222 }, { 177, 162, 215 }, { 110, 135, 195 }, { 57, 113, 168 },
                                    { 23, 83, 120 }, { 10, 49, 61 } },
                            { /* Coeff Band 2 */
                                    { 85, 190, 223 }, { 36, 139, 200 }, { 5, 90, 146 }, { 1, 60, 103 }, { 1, 38, 65 },
                                    { 1, 18, 30 } },
                            { /* Coeff Band 3 */
                                    { 72, 202, 223 }, { 23, 141, 199 }, { 2, 86, 140 }, { 1, 56, 97 }, { 1, 36, 61 },
                                    { 1, 16, 27 } },
                            { /* Coeff Band 4 */
                                    { 55, 218, 225 }, { 13, 145, 200 }, { 1, 86, 141 }, { 1, 57, 99 }, { 1, 35, 61 },
                                    { 1, 13, 22 } },
                            { /* Coeff Band 5 */
                                    { 15, 235, 212 }, { 1, 132, 184 }, { 1, 84, 139 }, { 1, 57, 97 }, { 1, 34, 56 },
                                    { 1, 14, 23 } } } },
                    { /* block Type 1 */
                            { /* Intra */
                                    { /* Coeff Band 0 */
                                            { 181, 21, 201 }, { 61, 37, 123 }, { 10, 38, 71 }, { 0, 0, 0 }, // unused
                                            { 0, 0, 0 }, // unused
                                            { 0, 0, 0 } // unused
                                    }, { /* Coeff Band 1 */
                                            { 47, 106, 172 }, { 95, 104, 173 }, { 42, 93, 159 }, { 18, 77, 131 },
                                            { 4, 50, 81 }, { 1, 17, 23 } },
                                    { /* Coeff Band 2 */
                                            { 62, 147, 199 }, { 44, 130, 189 }, { 28, 102, 154 }, { 18, 75, 115 },
                                            { 2, 44, 65 }, { 1, 12, 19 } },
                                    { /* Coeff Band 3 */
                                            { 55, 153, 210 }, { 24, 130, 194 }, { 3, 93, 146 }, { 1, 61, 97 },
                                            { 1, 31, 50 }, { 1, 10, 16 } },
                                    { /* Coeff Band 4 */
                                            { 49, 186, 223 }, { 17, 148, 204 }, { 1, 96, 142 }, { 1, 53, 83 },
                                            { 1, 26, 44 }, { 1, 11, 17 } },
                                    { /* Coeff Band 5 */
                                            { 13, 217, 212 }, { 2, 136, 180 }, { 1, 78, 124 }, { 1, 50, 83 },
                                            { 1, 29, 49 }, { 1, 14, 23 } } },
                            { /* Inter */
                                    { /* Coeff Band 0 */
                                            { 197, 13, 247 }, { 82, 17, 222 }, { 25, 17, 162 }, { 0, 0, 0 }, // unused
                                            { 0, 0, 0 }, // unused
                                            { 0, 0, 0 } // unused
                                    }, { /* Coeff Band 1 */
                                            { 126, 186, 247 }, { 234, 191, 243 }, { 176, 177, 234 }, { 104, 158, 220 },
                                            { 66, 128, 186 }, { 55, 90, 137 } },
                                    { /* Coeff Band 2 */
                                            { 111, 197, 242 }, { 46, 158, 219 }, { 9, 104, 171 }, { 2, 65, 125 },
                                            { 1, 44, 80 }, { 1, 17, 91 } },
                                    { /* Coeff Band 3 */
                                            { 104, 208, 245 }, { 39, 168, 224 }, { 3, 109, 162 }, { 1, 79, 124 },
                                            { 1, 50, 102 }, { 1, 43, 102 } },
                                    { /* Coeff Band 4 */
                                            { 84, 220, 246 }, { 31, 177, 231 }, { 2, 115, 180 }, { 1, 79, 134 },
                                            { 1, 55, 77 }, { 1, 60, 79 } },
                                    { /* Coeff Band 5 */
                                            { 43, 243, 240 }, { 8, 180, 217 }, { 1, 115, 166 }, { 1, 84, 121 },
                                            { 1, 51, 67 }, { 1, 16, 6 } } } } }

    };

    public static final short[] defaultMvJointProbs = { 32, 64, 96 };

    public static final short[][] defaultMvBitsProb = { { 136, 140, 148, 160, 176, 192, 224, 234, 234, 240 },
            { 136, 140, 148, 160, 176, 192, 224, 234, 234, 240 } };

    public static final short[] defaultMvClass0BitProb = { 216, 208 };

    public static final short[] defaultMvClass0HpProb = { 160, 160 };

    public static final short[] defaultMvSignProb = { 128, 128 };

    public static final short[][] defaultMvClassProbs = { { 224, 144, 192, 168, 192, 176, 192, 198, 198, 245 },
            { 216, 128, 176, 160, 176, 176, 192, 198, 198, 208 } };

    public static final short[][][] defaultMvClass0FrProbs = { { { 128, 128, 64 }, { 96, 112, 64 } },
            { { 128, 128, 64 }, { 96, 112, 64 } } };
    public static final short[][] defaultMvFrProbs = { { 64, 96, 64 }, { 64, 96, 64 } };
    public static final short[] defaultMvHpProb = { 128, 128 };

    public static final short[][] defaultInterModeProbs = { { 2, 173, 34 }, { 7, 145, 85 }, { 7, 166, 63 },
            { 7, 94, 66 }, { 8, 64, 46 }, { 17, 81, 31 }, { 25, 29, 30 }, };

    public static final short[][] defaultInterpFilterProbs = { { 235, 162 }, { 36, 255 }, { 34, 3 }, { 149, 144 } };

    public static final short[] defaultIsInterProbs = { 9, 102, 187, 225 };

    private static final short[][] defaultPartitionProbs = {
            // 8x8 -> 4x4
            { 199, 122, 141 }, // a/l both not split
            { 147, 63, 159 }, // a split, l not split
            { 148, 133, 118 }, // l split, a not split
            { 121, 104, 114 }, // a/l both split
            // 16x16 -> 8x8
            { 174, 73, 87 }, // a/l both not split
            { 92, 41, 83 }, // a split, l not split
            { 82, 99, 50 }, // l split, a not split
            { 53, 39, 39 }, // a/l both split
            // 32x32 -> 16x16
            { 177, 58, 59 }, // a/l both not split
            { 68, 26, 63 }, // a split, l not split
            { 52, 79, 25 }, // l split, a not split
            { 17, 14, 12 }, // a/l both split
            // 64x64 -> 32x32
            { 222, 34, 30 }, // a/l both not split
            { 72, 16, 44 }, // a split, l not split
            { 58, 32, 12 }, // l split, a not split
            { 10, 7, 6 } // a/l both split
    };

    public static final short[][][] kfYmodeProbs = { { // above = dc
            { 137, 30, 42, 148, 151, 207, 70, 52, 91 }, // left = dc
            { 92, 45, 102, 136, 116, 180, 74, 90, 100 }, // left = v
            { 73, 32, 19, 187, 222, 215, 46, 34, 100 }, // left = h
            { 91, 30, 32, 116, 121, 186, 93, 86, 94 }, // left = d45
            { 72, 35, 36, 149, 68, 206, 68, 63, 105 }, // left = d135
            { 73, 31, 28, 138, 57, 124, 55, 122, 151 }, // left = d117
            { 67, 23, 21, 140, 126, 197, 40, 37, 171 }, // left = d153
            { 86, 27, 28, 128, 154, 212, 45, 43, 53 }, // left = d207
            { 74, 32, 27, 107, 86, 160, 63, 134, 102 }, // left = d63
            { 59, 67, 44, 140, 161, 202, 78, 67, 119 } // left = tm
            }, { // above = v
                    { 63, 36, 126, 146, 123, 158, 60, 90, 96 }, // left = dc
                    { 43, 46, 168, 134, 107, 128, 69, 142, 92 }, // left = v
                    { 44, 29, 68, 159, 201, 177, 50, 57, 77 }, // left = h
                    { 58, 38, 76, 114, 97, 172, 78, 133, 92 }, // left = d45
                    { 46, 41, 76, 140, 63, 184, 69, 112, 57 }, // left = d135
                    { 38, 32, 85, 140, 46, 112, 54, 151, 133 }, // left = d117
                    { 39, 27, 61, 131, 110, 175, 44, 75, 136 }, // left = d153
                    { 52, 30, 74, 113, 130, 175, 51, 64, 58 }, // left = d207
                    { 47, 35, 80, 100, 74, 143, 64, 163, 74 }, // left = d63
                    { 36, 61, 116, 114, 128, 162, 80, 125, 82 } // left = tm
            }, { // above = h
                    { 82, 26, 26, 171, 208, 204, 44, 32, 105 }, // left = dc
                    { 55, 44, 68, 166, 179, 192, 57, 57, 108 }, // left = v
                    { 42, 26, 11, 199, 241, 228, 23, 15, 85 }, // left = h
                    { 68, 42, 19, 131, 160, 199, 55, 52, 83 }, // left = d45
                    { 58, 50, 25, 139, 115, 232, 39, 52, 118 }, // left = d135
                    { 50, 35, 33, 153, 104, 162, 64, 59, 131 }, // left = d117
                    { 44, 24, 16, 150, 177, 202, 33, 19, 156 }, // left = d153
                    { 55, 27, 12, 153, 203, 218, 26, 27, 49 }, // left = d207
                    { 53, 49, 21, 110, 116, 168, 59, 80, 76 }, // left = d63
                    { 38, 72, 19, 168, 203, 212, 50, 50, 107 } // left = tm
            }, { // above = d45
                    { 103, 26, 36, 129, 132, 201, 83, 80, 93 }, // left = dc
                    { 59, 38, 83, 112, 103, 162, 98, 136, 90 }, // left = v
                    { 62, 30, 23, 158, 200, 207, 59, 57, 50 }, // left = h
                    { 67, 30, 29, 84, 86, 191, 102, 91, 59 }, // left = d45
                    { 60, 32, 33, 112, 71, 220, 64, 89, 104 }, // left = d135
                    { 53, 26, 34, 130, 56, 149, 84, 120, 103 }, // left = d117
                    { 53, 21, 23, 133, 109, 210, 56, 77, 172 }, // left = d153
                    { 77, 19, 29, 112, 142, 228, 55, 66, 36 }, // left = d207
                    { 61, 29, 29, 93, 97, 165, 83, 175, 162 }, // left = d63
                    { 47, 47, 43, 114, 137, 181, 100, 99, 95 } // left = tm
            }, { // above = d135
                    { 69, 23, 29, 128, 83, 199, 46, 44, 101 }, // left = dc
                    { 53, 40, 55, 139, 69, 183, 61, 80, 110 }, // left = v
                    { 40, 29, 19, 161, 180, 207, 43, 24, 91 }, // left = h
                    { 60, 34, 19, 105, 61, 198, 53, 64, 89 }, // left = d45
                    { 52, 31, 22, 158, 40, 209, 58, 62, 89 }, // left = d135
                    { 44, 31, 29, 147, 46, 158, 56, 102, 198 }, // left = d117
                    { 35, 19, 12, 135, 87, 209, 41, 45, 167 }, // left = d153
                    { 55, 25, 21, 118, 95, 215, 38, 39, 66 }, // left = d207
                    { 51, 38, 25, 113, 58, 164, 70, 93, 97 }, // left = d63
                    { 47, 54, 34, 146, 108, 203, 72, 103, 151 } // left = tm
            }, { // above = d117
                    { 64, 19, 37, 156, 66, 138, 49, 95, 133 }, // left = dc
                    { 46, 27, 80, 150, 55, 124, 55, 121, 135 }, // left = v
                    { 36, 23, 27, 165, 149, 166, 54, 64, 118 }, // left = h
                    { 53, 21, 36, 131, 63, 163, 60, 109, 81 }, // left = d45
                    { 40, 26, 35, 154, 40, 185, 51, 97, 123 }, // left = d135
                    { 35, 19, 34, 179, 19, 97, 48, 129, 124 }, // left = d117
                    { 36, 20, 26, 136, 62, 164, 33, 77, 154 }, // left = d153
                    { 45, 18, 32, 130, 90, 157, 40, 79, 91 }, // left = d207
                    { 45, 26, 28, 129, 45, 129, 49, 147, 123 }, // left = d63
                    { 38, 44, 51, 136, 74, 162, 57, 97, 121 } // left = tm
            }, { // above = d153
                    { 75, 17, 22, 136, 138, 185, 32, 34, 166 }, // left = dc
                    { 56, 39, 58, 133, 117, 173, 48, 53, 187 }, // left = v
                    { 35, 21, 12, 161, 212, 207, 20, 23, 145 }, // left = h
                    { 56, 29, 19, 117, 109, 181, 55, 68, 112 }, // left = d45
                    { 47, 29, 17, 153, 64, 220, 59, 51, 114 }, // left = d135
                    { 46, 16, 24, 136, 76, 147, 41, 64, 172 }, // left = d117
                    { 34, 17, 11, 108, 152, 187, 13, 15, 209 }, // left = d153
                    { 51, 24, 14, 115, 133, 209, 32, 26, 104 }, // left = d207
                    { 55, 30, 18, 122, 79, 179, 44, 88, 116 }, // left = d63
                    { 37, 49, 25, 129, 168, 164, 41, 54, 148 } // left = tm
            }, { // above = d207
                    { 82, 22, 32, 127, 143, 213, 39, 41, 70 }, // left = dc
                    { 62, 44, 61, 123, 105, 189, 48, 57, 64 }, // left = v
                    { 47, 25, 17, 175, 222, 220, 24, 30, 86 }, // left = h
                    { 68, 36, 17, 106, 102, 206, 59, 74, 74 }, // left = d45
                    { 57, 39, 23, 151, 68, 216, 55, 63, 58 }, // left = d135
                    { 49, 30, 35, 141, 70, 168, 82, 40, 115 }, // left = d117
                    { 51, 25, 15, 136, 129, 202, 38, 35, 139 }, // left = d153
                    { 68, 26, 16, 111, 141, 215, 29, 28, 28 }, // left = d207
                    { 59, 39, 19, 114, 75, 180, 77, 104, 42 }, // left = d63
                    { 40, 61, 26, 126, 152, 206, 61, 59, 93 } // left = tm
            }, { // above = d63
                    { 78, 23, 39, 111, 117, 170, 74, 124, 94 }, // left = dc
                    { 48, 34, 86, 101, 92, 146, 78, 179, 134 }, // left = v
                    { 47, 22, 24, 138, 187, 178, 68, 69, 59 }, // left = h
                    { 56, 25, 33, 105, 112, 187, 95, 177, 129 }, // left = d45
                    { 48, 31, 27, 114, 63, 183, 82, 116, 56 }, // left = d135
                    { 43, 28, 37, 121, 63, 123, 61, 192, 169 }, // left = d117
                    { 42, 17, 24, 109, 97, 177, 56, 76, 122 }, // left = d153
                    { 58, 18, 28, 105, 139, 182, 70, 92, 63 }, // left = d207
                    { 46, 23, 32, 74, 86, 150, 67, 183, 88 }, // left = d63
                    { 36, 38, 48, 92, 122, 165, 88, 137, 91 } // left = tm
            }, { // above = tm
                    { 65, 70, 60, 155, 159, 199, 61, 60, 81 }, // left = dc
                    { 44, 78, 115, 132, 119, 173, 71, 112, 93 }, // left = v
                    { 39, 38, 21, 184, 227, 206, 42, 32, 64 }, // left = h
                    { 58, 47, 36, 124, 137, 193, 80, 82, 78 }, // left = d45
                    { 49, 50, 35, 144, 95, 205, 63, 78, 59 }, // left = d135
                    { 41, 53, 52, 148, 71, 142, 65, 128, 51 }, // left = d117
                    { 40, 36, 28, 143, 143, 202, 40, 55, 137 }, // left = d153
                    { 52, 34, 29, 129, 183, 227, 42, 35, 43 }, // left = d207
                    { 42, 44, 44, 104, 105, 164, 64, 130, 80 }, // left = d63
                    { 43, 81, 53, 140, 169, 204, 68, 84, 72 } // left = tm
            } };

    public static final short[][] kfUvModeProbs = { { 144, 11, 54, 157, 195, 130, 46, 58, 108 }, // y = dc
            { 118, 15, 123, 148, 131, 101, 44, 93, 131 }, // y = v
            { 113, 12, 23, 188, 226, 142, 26, 32, 125 }, // y = h
            { 120, 11, 50, 123, 163, 135, 64, 77, 103 }, // y = d45
            { 113, 9, 36, 155, 111, 157, 32, 44, 161 }, // y = d135
            { 116, 9, 55, 176, 76, 96, 37, 61, 149 }, // y = d117
            { 115, 9, 28, 141, 161, 167, 21, 25, 193 }, // y = d153
            { 120, 12, 32, 145, 195, 142, 32, 38, 86 }, // y = d207
            { 116, 12, 64, 120, 140, 125, 49, 115, 121 }, // y = d63
            { 102, 19, 66, 162, 182, 122, 35, 59, 128 } // y = tm
    };
    public static final short[][] defaultYModeProbs = { { 65, 32, 18, 144, 162, 194, 41, 51, 98 }, // block_size < 8x8
            { 132, 68, 18, 165, 217, 196, 45, 40, 78 }, // block_size < 16x16
            { 173, 80, 19, 176, 240, 193, 64, 35, 46 }, // block_size < 32x32
            { 221, 135, 38, 194, 248, 121, 96, 85, 29 } // block_size >= 32x32
    };

    public static final short[][] defaultUvModeProbs = { { 120, 7, 76, 176, 208, 126, 28, 54, 103 }, // y = dc
            { 48, 12, 154, 155, 139, 90, 34, 117, 119 }, // y = v
            { 67, 6, 25, 204, 243, 158, 13, 21, 96 }, // y = h
            { 97, 5, 44, 131, 176, 139, 48, 68, 97 }, // y = d45
            { 83, 5, 42, 156, 111, 152, 26, 49, 152 }, // y = d135
            { 80, 5, 58, 178, 74, 83, 33, 62, 145 }, // y = d117
            { 86, 5, 32, 154, 192, 168, 14, 22, 163 }, // y = d153
            { 85, 5, 32, 156, 216, 148, 19, 29, 73 }, // y = d207
            { 77, 7, 64, 116, 132, 122, 37, 126, 120 }, // y = d63
            { 101, 21, 107, 181, 192, 103, 19, 67, 125 } // y = tm
    };

    public static final short[][] defaultSingleRefProb = { { 33, 16 }, { 77, 74 }, { 142, 142 }, { 172, 170 },
            { 238, 247 } };

    public static final short[] defaultCompRefProb = { 50, 126, 123, 221, 226 };

    /**
     * Reads VP9 frame headers and creates the decoding context
     * 
     * @param bb ByteBuffer with the encoded frame, after the call to this function
     *           the header portion of this buffer will be read and the byte buffer
     *           will be pointing at the first compressed frame byte after the
     *           headers.
     * @return Initialized DecodingContext object that can be used for decoding the
     *         compressed VP9 frame.
     */
    public static DecodingContext createFromHeaders(ByteBuffer bb) {
        DecodingContext dc = new DecodingContext();
        int compressedHeaderSize = dc.readUncompressedHeader(bb);
        dc.readCompressedHeader(NIOUtils.read(bb, compressedHeaderSize));
        return dc;
    }

    protected DecodingContext() {
        CommonUtils.vp8_copy(skipProbs, defaultSkipProb);
        CommonUtils.vp8_copy(tx8x8Probs, defaultTxProbs8x8);
        CommonUtils.vp8_copy(tx16x16Probs, defaultTxProbs16x16);
        CommonUtils.vp8_copy(tx32x32Probs, defaultTxProbs32x32);

        coefProbs = new short[4][2][2][6][][];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    coefProbs[i][j][k][0] = new short[3][3];
                    for (int l = 1; l < 6; l++) {
                        coefProbs[i][j][k][l] = new short[6][3];
                    }
                }
            }
        }

        CommonUtils.vp8_copy(coefProbs, defaultCoefProbs);

        CommonUtils.vp8_copy(mvJointProbs, defaultMvJointProbs);
        CommonUtils.vp8_copy(mvSignProbs, defaultMvSignProb);
        CommonUtils.vp8_copy(mvClassProbs, defaultMvClassProbs);
        CommonUtils.vp8_copy(mvClass0BitProbs, defaultMvClass0BitProb);
        CommonUtils.vp8_copy(mvBitsProbs, defaultMvBitsProb);
        CommonUtils.vp8_copy(mvClass0FrProbs, defaultMvClass0FrProbs);
        CommonUtils.vp8_copy(mvFrProbs, defaultMvFrProbs);
        CommonUtils.vp8_copy(mvClass0HpProb, defaultMvClass0HpProb);
        CommonUtils.vp8_copy(mvHpProbs, defaultMvHpProb);

        CommonUtils.vp8_copy(interModeProbs, defaultInterModeProbs);
        CommonUtils.vp8_copy(interpFilterProbs, defaultInterpFilterProbs);

        CommonUtils.vp8_copy(isInterProbs, defaultIsInterProbs);

        CommonUtils.vp8_copy(singleRefProbs, defaultSingleRefProb);

        CommonUtils.vp8_copy(yModeProbs, defaultYModeProbs);
        CommonUtils.vp8_copy(uvModeProbs, defaultUvModeProbs);

        CommonUtils.vp8_copy(partitionProbs, defaultPartitionProbs);

        CommonUtils.vp8_copy(compRefProbs, defaultCompRefProb);
    }

    public boolean isKeyIntraFrame() {
        return false;
    }

    public boolean isSegmentationEnabled() {
        return segmentationEnabled;
    }

    public boolean isUpdateSegmentMap() {
        return false;
    }

    public boolean isSegmentFeatureActive(int segmentId, int segLvlSkip) {
        return false;
    }

    public boolean isSegmentMapConditionalUpdate() {
        return false;
    }

    public int getSegmentFeature(int segmentId, int segLvlRefFrame) {
        return 0;
    }

    public int getCompFixedRef() {
        return 0;
    }

    public int refFrameSignBias(int fixedRef) {
        return 0;
    }

    public int getInterpFilter() {
        return interpFilter;
    }

    public int getRefMode() {
        return refMode;
    }

    public long[][] getLeftMVs() {
        return null;
    }

    public long[][] getAboveMVs() {
        return null;
    }

    public long[][] getAboveLeftMVs() {
        return null;
    }

    public long[] getLeft4x4MVs() {
        return null;
    }

    public long[] getAbove4x4MVs() {
        return null;
    }

    public boolean[] getAboveCompound() {
        return aboveCompound;
    }

    public boolean[] getLeftCompound() {
        return leftCompound;
    }

    public boolean isAllowHpMv() {
        return false;
    }

    public boolean isUsePrevFrameMvs() {
        return false;
    }

    public long[][] getPrevFrameMv() {
        return null;
    }

    public int getMiFrameWidth() {
        return (frameWidth + 7) >> 3;
    }

    public int getMiFrameHeight() {
        return (frameHeight + 7) >> 3;
    }

    public int[] getLeftInterpFilters() {
        return leftInterpFilters;
    }

    public int[] getAboveInterpFilters() {
        return aboveInterpFilters;
    }

    public int getMiTileHeight() {
        return tileHeight;
    }

    public int getMiTileWidth() {
        return tileWidth;
    }

    public int getCompVarRef(int i) {
        return 0;
    }

    public int[] getAboveModes() {
        return aboveModes;
    }

    public int[] getLeftModes() {
        return leftModes;
    }

    public int getTxMode() {
        return txMode;
    }

    public boolean[] getAboveSegIdPredicted() {
        return null;
    }

    public boolean[] getLeftSegIdPredicted() {
        return null;
    }

    public int[][] getPrevSegmentIds() {
        return null;
    }

    public int getSubX() {
        return subsamplingX;
    }

    public int getSubY() {
        return subsamplingY;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int[][] getAboveNonzeroContext() {
        return aboveNonzeroContext;
    }

    public int[][] getLeftNonzeroContext() {
        return leftNonzeroContext;
    }

    public int[] getLeftPartitionSizes() {
        return leftPartitionSizes;
    }

    public int[] getAbovePartitionSizes() {
        return abovePartitionSizes;
    }

    public boolean[] getLeftSkipped() {
        return leftSkipped;
    }

    public boolean[] getAboveSkipped() {
        return aboveSkipped;
    }

    /**
     * Reads the uncompressed header of the frame. Will consume only the portion of
     * the frame data that contains the uncompressed header. The ByteBuffer will be
     * pointing at the first byte after the uncompressed header.
     * 
     * @param bb The data for the frame.
     * 
     * @return Size in bytes of the compressed header following this uncompressed
     *         header.
     */
    protected int readUncompressedHeader(ByteBuffer bb) {
        BitReader br = BitReader.createBitReader(bb);

        int frame_marker = br.readNBit(2);
        profile = br.read1Bit() | (br.read1Bit() << 1);
        if (profile == 3)
            br.read1Bit();
        showExistingFrame = br.read1Bit();
        if (showExistingFrame == 1) {
            frameToShowMapIdx = br.readNBit(3);
        }
        frameType = br.read1Bit();
        showFrame = br.read1Bit();
        errorResilientMode = br.read1Bit();
        if (frameType == KEY_FRAME) {
            frame_sync_code(br);
            readColorConfig(br);
            readFrameSize(br);
            readRenderSize(br);
            refreshFrameFlags = 0xFF;
            frameIsIntra = 1;
        } else {
            intraOnly = 0;
            if (showFrame == 0) {
                intraOnly = br.read1Bit();
            }
            resetFrameContext = 0;
            if (errorResilientMode == 0) {
                resetFrameContext = br.readNBit(2);
            }
            if (intraOnly == 1) {
                frame_sync_code(br);
                if (profile > 0) {
                    readColorConfig(br);
                } else {
                    colorSpace = CS_BT_601;
                    subsamplingX = 1;
                    subsamplingY = 1;
                    bitDepth = 8;
                }
                refreshFrameFlags = br.readNBit(8);
                readFrameSize(br);
                readRenderSize(br);
            } else {
                int refreshFrameFlags = br.readNBit(8);

                for (int i = 0; i < 3; i++) {
                    refFrameIdx[i] = br.readNBit(3);
                    refFrameSignBias[LAST_FRAME + i] = br.read1Bit();
                }
                readFrameSizeWithRefs(br);
                allowHighPrecisionMv = br.read1Bit();
                readInterpolationFilter(br);
            }
        }
        refreshFrameContext = 0;
        if (errorResilientMode == 0) {
            refreshFrameContext = br.read1Bit();
            frameParallelDecodingMode = br.read1Bit();
        }
        frameContextIdx = br.readNBit(2);
        readLoopFilterParams(br);
        readQuantizationParams(br);
        readSegmentationParams(br);
        readTileInfo(br);
        int headerSizeInBytes = br.readNBit(16);
        br.terminate();

        return headerSizeInBytes;
    }

    int calc_min_log2_tile_cols() {
        int sb64Cols = (frameWidth + 63) >> 6;
        int minLog2 = 0;
        while ((MAX_TILE_WIDTH_B64 << minLog2) < sb64Cols)
            minLog2++;
        return minLog2;
    }

    int calc_max_log2_tile_cols() {
        int sb64Cols = (frameWidth + 63) >> 6;
        int maxLog2 = 1;
        while ((sb64Cols >> maxLog2) >= MIN_TILE_WIDTH_B64)
            maxLog2++;
        return maxLog2 - 1;
    }

    private void readTileInfo(BitReader br) {
        int minLog2TileCols = calc_min_log2_tile_cols();
        int maxLog2TileCols = calc_max_log2_tile_cols();
        tileColsLog2 = minLog2TileCols;
        while (tileColsLog2 < maxLog2TileCols) {
            int increment_tile_cols_log2 = br.read1Bit();
            if (increment_tile_cols_log2 == 1)
                tileColsLog2++;
            else
                break;
        }
        tileRowsLog2 = br.read1Bit();
        if (tileRowsLog2 == 1) {
            int increment_tile_rows_log2 = br.read1Bit();
            tileRowsLog2 += increment_tile_rows_log2;
        }
    }

    private static short readProb(BitReader br) {
        if (br.read1Bit() == 1) {
            return (short) br.readNBit(8);
        } else {
            return 255;
        }
    }

    private void readSegmentationParams(BitReader br) {
        segmentationEnabled = br.read1Bit() == 1;
        if (segmentationEnabled) {
            if (br.read1Bit() == 1) {
                for (int i = 0; i < 7; i++)
                    segmentationTreeProbs[i] = readProb(br);
                int segmentationTemporalUpdate = br.read1Bit();
                for (int i = 0; i < 3; i++)
                    segmentationPredProbs[i] = segmentationTemporalUpdate == 1 ? readProb(br) : 255;
            }
            if (br.read1Bit() == 1) {
                int segmentationAbsOrDeltaUpdate = br.read1Bit();
                for (int i = 0; i < MAX_SEGMENTS; i++) {
                    for (int j = 0; j < SEG_LVL_MAX; j++) {
                        if (br.read1Bit() == 1) {
                            featureEnabled[i][j] = 1;
                            int bits_to_read = SEGMENTATION_FEATURE_BITS[j];
                            int value = br.readNBit(bits_to_read);
                            if (SEGMENTATION_FEATURE_SIGNED[j] == 1) {
                                if (br.read1Bit() == 1)
                                    value *= -1;
                            }
                            featureData[i][j] = value;
                        }
                    }
                }
            }
        }
    }

    private static int readDeltaQ(BitReader br) {
        int delta_coded = br.read1Bit();
        if (delta_coded == 1) {
            return br.readNBitSigned(4);
        } else {
            return 0;
        }
    }

    private void readQuantizationParams(BitReader br) {
        baseQIdx = br.readNBit(8);
        deltaQYDc = readDeltaQ(br);
        deltaQUvDc = readDeltaQ(br);
        deltaQUvAc = readDeltaQ(br);
        lossless = baseQIdx == 0 && deltaQYDc == 0 && deltaQUvDc == 0 && deltaQUvAc == 0;
    }

    private void readLoopFilterParams(BitReader br) {
        filterLevel = br.readNBit(6);
        sharpnessLevel = br.readNBit(3);
        int modeRefDeltaEnabled = br.read1Bit();
        if (modeRefDeltaEnabled == 1) {
            int modeRefDeltaUpdate = br.read1Bit();
            if (modeRefDeltaUpdate == 1) {
                for (int i = 0; i < 4; i++) {
                    if (br.read1Bit() == 1)
                        loopFilterRefDeltas[i] = br.readNBitSigned(6);
                }
                for (int i = 0; i < 2; i++) {
                    if (br.read1Bit() == 1)
                        loopFilterModeDeltas[i] = br.readNBitSigned(6);
                }
            }
        }
    }

    private void readInterpolationFilter(BitReader br) {
        interpFilter = SWITCHABLE;
        if (br.read1Bit() == 0) {
            interpFilter = LITERAL_TO_FILTER_TYPE[br.readNBit(2)];
        }
    }

    private void readFrameSizeWithRefs(BitReader br) {
        int i;
        for (i = 0; i < 3; i++) {
            if (br.read1Bit() == 1) {
                frameWidth = refFrameWidth[refFrameIdx[i]];
                frameHeight = refFrameHeight[refFrameIdx[i]];
                break;
            }
        }
        if (i == 3) {
            readFrameSize(br);
        }
        readRenderSize(br);
    }

    private void readRenderSize(BitReader br) {
        if (br.read1Bit() == 1) {
            renderWidth = br.readNBit(16) + 1;
            renderHeight = br.readNBit(16) + 1;
        } else {
            renderWidth = frameWidth;
            renderHeight = frameHeight;
        }
    }

    private void readFrameSize(BitReader br) {
        frameWidth = br.readNBit(16) + 1;
        frameHeight = br.readNBit(16) + 1;
    }

    private void readColorConfig(BitReader br) {
        if (profile >= 2) {
            int ten_or_twelve_bit = br.read1Bit();
            bitDepth = ten_or_twelve_bit == 1 ? 12 : 10;
        } else {
            bitDepth = 8;
        }
        int colorSpace = br.readNBit(3);
        if (colorSpace != CS_RGB) {
            colorRange = br.read1Bit();
            if (profile == 1 || profile == 3) {
                subsamplingX = br.read1Bit();
                subsamplingY = br.read1Bit();
                int reserved_zero = br.read1Bit();
            } else {
                subsamplingX = 1;
                subsamplingY = 1;
            }
        } else {
            colorRange = 1;
            if (profile == 1 || profile == 3) {
                subsamplingX = 0;
                subsamplingY = 0;
                int reserved_zero = br.read1Bit();
            }
        }
    }

    private static void frame_sync_code(BitReader br) {
        int code = br.readNBit(24);
    }

    /**
     * Reads compressed header of the frame. This header mostly contains probability
     * updates.
     * 
     * @param boolDec
     */
    protected void readCompressedHeader(ByteBuffer compressedHeader) {
        VPXBooleanDecoder boolDec = new VPXBooleanDecoder(compressedHeader, 0);

        if (boolDec.readBitEq() != 0)
            throw new RuntimeException("Invalid marker bit");

        readTxMode(boolDec);
        // int maxTxSize = tx_mode_to_biggest_tx_size[txMode];

        if (txMode == TX_MODE_SELECT) {
            readTxModeProbs(boolDec);
        }
        readCoefProbs(boolDec);
        readSkipProb(boolDec);
        if (frameIsIntra == 0) {
            readInterModeProbs(boolDec);
            if (interpFilter == SWITCHABLE)
                readInterpFilterProbs(boolDec);
            readIsInterProbs(boolDec);
            frameReferenceMode(boolDec);
            frameReferenceModeProbs(boolDec);
            readYModeProbs(boolDec);
            readPartitionProbs(boolDec);
            mvProbs(boolDec);
        }
    }

    private void readTxMode(VPXBooleanDecoder boolDec) {
        if (lossless) {
            txMode = ONLY_4X4;
        } else {
            txMode = boolDec.decodeInt(2);
            if (txMode == ALLOW_32X32) {
                txMode += boolDec.decodeInt(1);
            }
        }
    }

    private void readTxModeProbs(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < TX_SIZE_CONTEXTS; i++)
            for (int j = 0; j < TX_SIZES - 3; j++) {
                tx8x8Probs[i][j] = diffUpdateProb(boolDec, tx8x8Probs[i][j]);
            }
        for (int i = 0; i < TX_SIZE_CONTEXTS; i++)
            for (int j = 0; j < TX_SIZES - 2; j++) {
                tx16x16Probs[i][j] = diffUpdateProb(boolDec, tx16x16Probs[i][j]);
            }
        for (int i = 0; i < TX_SIZE_CONTEXTS; i++)
            for (int j = 0; j < TX_SIZES - 1; j++) {
                tx32x32Probs[i][j] = diffUpdateProb(boolDec, tx32x32Probs[i][j]);
            }
    }

    private short diffUpdateProb(VPXBooleanDecoder boolDec, short prob) {
        short update_prob = (short) boolDec.readBit(252);
        if (update_prob == 1) {
            short deltaProb = (short)decodeTermSubexp(boolDec);
            prob = invRemapProb(deltaProb, prob);
        }
        return prob;
    }

    private int decodeTermSubexp(VPXBooleanDecoder boolDec) {
        int bit = boolDec.readBitEq();
        if (bit == 0) {
            return boolDec.decodeInt(4);
        }
        bit = boolDec.readBitEq();
        if (bit == 0) {
            return boolDec.decodeInt(4) + 16;
        }
        bit = boolDec.readBitEq();
        if (bit == 0) {
            return boolDec.decodeInt(5) + 32;
        }
        int v = boolDec.decodeInt(7);
        if (v < 65)
            return v + 64;
        bit = boolDec.readBitEq();
        return (v << 1) - 1 + bit;
    }

    private short invRemapProb(short deltaProb, short prob) {
        short m = prob;
        int v = deltaProb;
        v = INV_REMAP_TABLE[v];
        m--;
        if ((m << 1) <= 255)
            m = (short) (1 + invRecenterNonneg(v, m));
        else
            m = (short) (255 - invRecenterNonneg(v, 255 - 1 - m));
        return m;
    }

    private int invRecenterNonneg(int v, int m) {
        if (v > 2 * m)
            return v;
        if ((v & 1) != 0)
            return m - ((v + 1) >> 1);
        return m + (v >> 1);
    }

    private void readCoefProbs(VPXBooleanDecoder boolDec) {
        int maxTxSize = tx_mode_to_biggest_tx_size[txMode];
        for (int txSz = TX_4X4; txSz <= maxTxSize; txSz++) {
            int update_probs = boolDec.readBitEq();
            if (update_probs == 1) {
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = 0; k < 6; k++) {
                            int maxL = (k == 0) ? 3 : 6;
                            for (int l = 0; l < maxL; l++) {
                                for (int m = 0; m < 3; m++) {
                                    coefProbs[txSz][i][j][k][l][m] = diffUpdateProb(boolDec,
                                            coefProbs[txSz][i][j][k][l][m]);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void readSkipProb(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < SKIP_CONTEXTS; i++) {
            skipProbs[i] = diffUpdateProb(boolDec, skipProbs[i]);
        }
    }

    private void readInterModeProbs(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < INTER_MODE_CONTEXTS; i++)
            for (int j = 0; j < INTER_MODES - 1; j++)
                interModeProbs[i][j] = diffUpdateProb(boolDec, interModeProbs[i][j]);
    }

    private void readInterpFilterProbs(VPXBooleanDecoder boolDec) {
        for (int j = 0; j < INTERP_FILTER_CONTEXTS; j++)
            for (int i = 0; i < SWITCHABLE_FILTERS - 1; i++)
                interpFilterProbs[j][i] = diffUpdateProb(boolDec, interpFilterProbs[j][i]);
    }

    private void readIsInterProbs(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < IS_INTER_CONTEXTS; i++)
            isInterProbs[i] = diffUpdateProb(boolDec, isInterProbs[i]);
    }

    private void frameReferenceMode(VPXBooleanDecoder boolDec) {
        int compoundReferenceAllowed = 0;
        for (int i = 1; i < REFS_PER_FRAME; i++)
            if (refFrameSignBias[i] != refFrameSignBias[0])
                compoundReferenceAllowed = 1;
        if (compoundReferenceAllowed == 1) {
            int non_single_reference = boolDec.readBitEq();
            if (non_single_reference == 0) {
                refMode = SINGLE_REF;
            } else {
                int reference_select = boolDec.readBitEq();
                if (reference_select == 0)
                    refMode = COMPOUND_REF;
                else
                    refMode = REFERENCE_MODE_SELECT;
                setupCompoundReferenceMode();
            }
        } else {
            refMode = SINGLE_REF;
        }
    }

    private void frameReferenceModeProbs(VPXBooleanDecoder boolDec) {
        if (refMode == REFERENCE_MODE_SELECT) {
            for (int i = 0; i < COMP_MODE_CONTEXTS; i++)
                compModeProbs[i] = diffUpdateProb(boolDec, compModeProbs[i]);
        }
        if (refMode != COMPOUND_REF) {
            for (int i = 0; i < REF_CONTEXTS; i++) {
                singleRefProbs[i][0] = diffUpdateProb(boolDec, singleRefProbs[i][0]);
                singleRefProbs[i][1] = diffUpdateProb(boolDec, singleRefProbs[i][1]);
            }
        }
        if (refMode != SINGLE_REF) {
            for (int i = 0; i < REF_CONTEXTS; i++)
                compRefProbs[i] = diffUpdateProb(boolDec, compRefProbs[i]);
        }
    }

    private void readYModeProbs(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < BLOCK_SIZE_GROUPS; i++)
            for (int j = 0; j < INTRA_MODES - 1; j++)
                yModeProbs[i][j] = diffUpdateProb(boolDec, yModeProbs[i][j]);
    }

    private void readPartitionProbs(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < PARTITION_CONTEXTS; i++)
            for (int j = 0; j < PARTITION_TYPES - 1; j++)
                partitionProbs[i][j] = diffUpdateProb(boolDec, partitionProbs[i][j]);
    }

    private void mvProbs(VPXBooleanDecoder boolDec) {
        for (int j = 0; j < MV_JOINTS - 1; j++)
            mvJointProbs[j] = updateMvProb(boolDec, mvJointProbs[j]);
        for (int i = 0; i < 2; i++) {
            mvSignProbs[i] = updateMvProb(boolDec, mvSignProbs[i]);
            for (int j = 0; j < MV_CLASSES - 1; j++)
                mvClassProbs[i][j] = updateMvProb(boolDec, mvClassProbs[i][j]);
            mvClass0BitProbs[i] = updateMvProb(boolDec, mvClass0BitProbs[i]);
            for (int j = 0; j < MV_OFFSET_BITS; j++)
                mvBitsProbs[i][j] = updateMvProb(boolDec, mvBitsProbs[i][j]);
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < CLASS0_SIZE; j++)
                for (int k = 0; k < MV_FR_SIZE - 1; k++)
                    mvClass0FrProbs[i][j][k] = updateMvProb(boolDec, mvClass0FrProbs[i][j][k]);
            for (int k = 0; k < MV_FR_SIZE - 1; k++)
                mvFrProbs[i][k] = updateMvProb(boolDec, mvFrProbs[i][k]);
        }
        if (allowHighPrecisionMv == 1) {
            for (int i = 0; i < 2; i++) {
                mvClass0HpProb[i] = updateMvProb(boolDec, mvClass0HpProb[i]);
                mvHpProbs[i] = updateMvProb(boolDec, mvHpProbs[i]);
            }
        }
    }

    private short updateMvProb(VPXBooleanDecoder boolDec, short prob) {
        short update_mv_prob = (short) boolDec.readBit(252);
        if (update_mv_prob == 1) {
            short mv_prob = (short) boolDec.decodeInt(7);
            prob = (short) ((mv_prob << 1) | 1);
        }
        return prob;
    }

    private void setupCompoundReferenceMode() {
        if (refFrameSignBias[LAST_FRAME] == refFrameSignBias[GOLDEN_FRAME]) {
            compFixedRef = ALTREF_FRAME;
            compVarRef0 = LAST_FRAME;
            compVarRef1 = GOLDEN_FRAME;
        } else if (refFrameSignBias[LAST_FRAME] == refFrameSignBias[ALTREF_FRAME]) {
            compFixedRef = GOLDEN_FRAME;
            compVarRef0 = LAST_FRAME;
            compVarRef1 = ALTREF_FRAME;
        } else {
            compFixedRef = LAST_FRAME;
            compVarRef0 = GOLDEN_FRAME;
            compVarRef1 = ALTREF_FRAME;
        }
    }

    public int getFrameContextIdx() {
        return frameContextIdx;
    }

    public int getTileColsLog2() {
        return tileColsLog2;
    }

    public int getTileRowsLog2() {
        return tileRowsLog2;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getBaseQIdx() {
        return baseQIdx;
    }

    public int getDeltaQYDc() {
        return deltaQYDc;
    }

    public int getDeltaQUvDc() {
        return deltaQUvDc;
    }

    public int getDeltaQUvAc() {
        return deltaQUvAc;
    }

    public int getFilterLevel() {
        return filterLevel;
    }

    public int getSharpnessLevel() {
        return sharpnessLevel;
    }

    public short[] getSkipProbs() {
        return skipProbs;
    }

    public short[][] getTx8x8Probs() {
        return tx8x8Probs;
    }

    public short[][] getTx16x16Probs() {
        return tx16x16Probs;
    }

    public short[][] getTx32x32Probs() {
        return tx32x32Probs;
    }

    public short[][][][][][] getCoefProbs() {
        return coefProbs;
    }

    public short[] getMvJointProbs() {
        return mvJointProbs;
    }

    public short[] getMvSignProb() {
        return mvSignProbs;
    }

    public short[][] getMvClassProbs() {
        return mvClassProbs;
    }

    public short[] getMvClass0BitProbs() {
        return mvClass0BitProbs;
    }

    public short[][] getMvBitsProb() {
        return mvBitsProbs;
    }

    public short[][][] getMvClass0FrProbs() {
        return mvClass0FrProbs;
    }

    public short[][] getMvFrProbs() {
        return mvFrProbs;
    }

    public short[] getMvClass0HpProbs() {
        return mvClass0HpProb;
    }

    public short[] getMvHpProbs() {
        return mvHpProbs;
    }

    public short[][] getInterModeProbs() {
        return interModeProbs;
    }

    public short[][] getInterpFilterProbs() {
        return interpFilterProbs;
    }

    public short[] getIsInterProbs() {
        return isInterProbs;
    }

    public short[][] getSingleRefProbs() {
        return singleRefProbs;
    }

    public short[][] getYModeProbs() {
        return yModeProbs;
    }

    public short[][] getPartitionProbs() {
        return partitionProbs;
    }

    public short[][] getUvModeProbs() {
        return uvModeProbs;
    }

    public short[] getCompRefProbs() {
        return compRefProbs;
    }

    public short[][][] getKfYModeProbs() {
        return kfYmodeProbs;
    }

    public short[][] getKfUVModeProbs() {
        return kfUvModeProbs;
    }

    public short[] getSegmentationTreeProbs() {
        return segmentationTreeProbs;
    }

    public int[] getSegmentationPredProbs() {
        return segmentationPredProbs;
    }

    public short[] getCompModeProb() {
        return compModeProbs;
    }

    public int[] getAboveRefs() {
        return aboveRefs;
    }

    public int[] getLeftRefs() {
        return leftRefs;
    }

    public int getMiTileStartCol() {
        return miTileStartCol;
    }

    public int[] getAboveTxSizes() {
        return aboveTxSizes;
    }

    public int[] getLeftTxSizes() {
        return leftTxSizes;
    }
}
