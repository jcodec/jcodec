package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.*;

import java.nio.ByteBuffer;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;
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
    private int subsamplingX;
    private int colorSpace;
    private int subsamplingY;
    private int bitDepth;
    private int frameWidth;
    private int frameHeight;
    private int renderWidth;
    private int renderHeight;
    private int[] refFrameWidth = new int[MAX_REF_FRAMES];
    private int[] refFrameHeight = new int[MAX_REF_FRAMES];
    private int[] refFrameIdx = new int[3];
    //The type (or one of its parents) contains already a method called [refFrameSignBias]. Javascript cannot distinguish methods/fields with the same name
    private int[] _refFrameSignBias = new int[3];
    private int allowHighPrecisionMv;
    private int interpolationFilter;
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
    private int segmentationEnabled;
    private int[] segmentationTreeProbs = new int[7];
    private int[] segmentationPredProb = new int[3];
    private int[][] featureEnabled = new int[MAX_SEGMENTS][SEG_LVL_MAX];
    private int[][] featureData = new int[MAX_SEGMENTS][SEG_LVL_MAX];
    private int miCols;
    private int miRows;
    private int sb64Cols;
    private int sb64Rows;
    private int tileRowsLog2;
    private int tileColsLog2;
    private int txMode;
    private int compFixedRef;
    private int compVarRef0;
    private int compVarRef1;

    private int[][] txProbs8x8 = new int[TX_SIZE_CONTEXTS][TX_SIZES - 3];
    private int[][] txProbs16x16 = new int[TX_SIZE_CONTEXTS][TX_SIZES - 2];
    private int[][] txProbs32x32 = new int[TX_SIZE_CONTEXTS][TX_SIZES - 1];
    private int referenceMode;
    private int[][][][][][] coefProbs;
    private int[] skipProb = new int[SKIP_CONTEXTS];
    private int[][] interModeProbs = new int[INTER_MODE_CONTEXTS][INTER_MODES - 1];
    private int[][] interpFilterProbs = new int[INTERP_FILTER_CONTEXTS][SWITCHABLE_FILTERS - 1];
    private int[] isInterProb = new int[IS_INTER_CONTEXTS];

    private int[] compModeProb = new int[COMP_MODE_CONTEXTS];
    private int[][] singleRefProb = new int[REF_CONTEXTS][2];
    private int[] compRefProb = new int[REF_CONTEXTS];

    private int[][] yModeProbs = new int[BLOCK_SIZE_GROUPS][INTRA_MODES - 1];
    private int[][] partitionProbs = new int[PARTITION_CONTEXTS][PARTITION_TYPES - 1];

    private int[] mvJointProbs = new int[MV_JOINTS - 1];
    private int[] mvSignProb = new int[2];
    private int[][] mvClassProbs = new int[2][MV_CLASSES - 1];
    private int[] mvClass0BitProb = new int[2];
    private int[][] mvBitsProb = new int[2][MV_OFFSET_BITS];
    private int[][][] mvClass0FrProbs = new int[2][CLASS0_SIZE][MV_FR_SIZE - 1];
    private int[][] mvFrProbs = new int[2][MV_FR_SIZE - 1];
    private int[] mvClass0HpProb = new int[2];
    private int[] mvHpProb = new int[2];

    /**
     * Reads VP9 frame headers and creates the decoding context
     * 
     * @param bb
     *            ByteBuffer with the encoded frame, after the call to this
     *            function the header portion of this buffer will be read and
     *            the byte buffer will be pointing at the first compressed frame
     *            byte after the headers.
     * @return Initialized DecodingContext object that can be used for decoding
     *         the compressed VP9 frame.
     */
    public static DecodingContext createFromHeaders(ByteBuffer bb) {
        DecodingContext dc = new DecodingContext();
        dc.readHeaders(bb);
        return dc;
    }

    public boolean isKeyIntraFrame() {
        return false;
    }

    public boolean isSegmentationEnabled() {
        return false;
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
        return 0;
    }

    public int getRefMode() {
        return 0;
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
        return null;
    }

    public boolean[] getLeftCompound() {
        return null;
    }

    public int[][][] getRefs() {
        return null;
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

    public int getMiFrameHeight() {
        return 0;
    }

    public int getMiFrameWidth() {
        return 0;
    }

    public int getTileStart() {
        return 0;
    }

    public int[] getLeftInterpFilters() {
        return null;
    }

    public int[] getAboveInterpFilters() {
        return null;
    }

    public int[] getLeftLumaModes() {
        return null;
    }

    public int[] getAboveLumaModes() {
        return null;
    }

    public int getTileHeight() {
        return 0;
    }

    public int getTileWidth() {
        return 0;
    }

    public int getCompVarRef(int i) {
        return 0;
    }

    public int[] getAboveIntraModes() {
        return null;
    }

    public int[] getLeftIntraModes() {
        return null;
    }

    public int getTxMode() {
        return 0;
    }

    public int[][] getTxSizes() {
        return null;
    }

    public boolean[][] getSkippedBlockes() {
        return null;
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
        return 0;
    }

    public int getSubY() {
        return 0;
    }

    public int[] getScan(int plane, int txSz, int blockIdx) {
        return null;
    }

    public int getTxType(int plane, int txSz, int blockIdx) {
        return 0;
    }

    public int getBitDepth() {
        return 0;
    }

    public int[][] getAboveNonzeroContext() {
        return null;
    }

    public int[][] getLeftNonzeroContext() {
        return null;
    }

    public int[] getTokenCache() {
        return null;
    }

    public int[] getLeftPartitionSizes() {
        return null;
    }

    public int[] getAbovePartitionSizes() {
        return null;
    }

    private void readHeaders(ByteBuffer bb) {
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
                    _refFrameSignBias[LAST_FRAME + i] = br.read1Bit();
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

        ByteBuffer compressedHeader = NIOUtils.read(bb, headerSizeInBytes);
        VPXBooleanDecoder boolDec = new VPXBooleanDecoder(compressedHeader, 0);
        readCompressedHeader(boolDec);
    }

    void computeImageSize() {
        miCols = (frameWidth + 7) >> 3;
        miRows = (frameHeight + 7) >> 3;
        sb64Cols = (miCols + 7) >> 3;
        sb64Rows = (miRows + 7) >> 3;
    }

    int calc_min_log2_tile_cols() {
        int minLog2 = 0;
        while ((MAX_TILE_WIDTH_B64 << minLog2) < sb64Cols)
            minLog2++;
        return minLog2;
    }

    int calc_max_log2_tile_cols() {
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

    private static int readProb(BitReader br) {
        if (br.read1Bit() == 1) {
            return br.readNBit(8);
        } else {
            return 255;
        }
    }

    private void readSegmentationParams(BitReader br) {
        segmentationEnabled = br.read1Bit();
        if (segmentationEnabled == 1) {
            if (br.read1Bit() == 1) {
                for (int i = 0; i < 7; i++)
                    segmentationTreeProbs[i] = readProb(br);
                int segmentationTemporalUpdate = br.read1Bit();
                for (int i = 0; i < 3; i++)
                    segmentationPredProb[i] = segmentationTemporalUpdate == 1 ? readProb(br) : 255;
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
            return br.readNBitSigned(5);
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
        int loopFilterLevel = br.readNBit(6);
        int loopFilterSharpness = br.readNBit(3);
        if (br.read1Bit() == 1) {
            if (br.read1Bit() == 1) {
                for (int i = 0; i < 4; i++) {
                    if (br.read1Bit() == 1)
                        loopFilterRefDeltas[i] = br.readNBit(6);
                }
                for (int i = 0; i < 2; i++) {
                    if (br.read1Bit() == 1)
                        loopFilterModeDeltas[i] = br.readNBit(6);
                }
            }
        }
    }

    private void readInterpolationFilter(BitReader br) {
        interpolationFilter = SWITCHABLE;
        if (br.read1Bit() == 0) {
            interpolationFilter = LITERAL_TO_FILTER_TYPE[br.readNBit(2)];
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
        } else {
            computeImageSize();
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
        computeImageSize();
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
            int color_range = br.read1Bit();
            if (profile == 1 || profile == 3) {
                subsamplingX = br.read1Bit();
                subsamplingY = br.read1Bit();
                int reserved_zero = br.read1Bit();
            } else {
                subsamplingX = 1;
                subsamplingY = 1;
            }
        } else {
            int colorRange = 1;
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

    private void readCompressedHeader(VPXBooleanDecoder boolDec) {
        int maxTxSize = tx_mode_to_biggest_tx_size[txMode];
        coefProbs = new int[maxTxSize + 1][2][2][6][6][3];

        readTxMode(boolDec);
        if (txMode == TX_MODE_SELECT) {
            readTxModeProbs(boolDec);
        }
        readCoefProbs(boolDec);
        readSkipProb(boolDec);
        if (frameIsIntra == 0) {
            readInterModeProbs(boolDec);
            if (interpolationFilter == SWITCHABLE)
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
            int txMode = boolDec.decodeInt(2);
            if (txMode == ALLOW_32X32) {
                txMode += boolDec.decodeInt(1);
            }
        }
    }

    private void readTxModeProbs(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < TX_SIZE_CONTEXTS; i++)
            for (int j = 0; j < TX_SIZES - 3; j++)
                txProbs8x8[i][j] = diffUpdateProb(boolDec, txProbs8x8[i][j]);
        for (int i = 0; i < TX_SIZE_CONTEXTS; i++)
            for (int j = 0; j < TX_SIZES - 2; j++)
                txProbs16x16[i][j] = diffUpdateProb(boolDec, txProbs16x16[i][j]);
        for (int i = 0; i < TX_SIZE_CONTEXTS; i++)
            for (int j = 0; j < TX_SIZES - 1; j++)
                txProbs32x32[i][j] = diffUpdateProb(boolDec, txProbs32x32[i][j]);
    }

    private int diffUpdateProb(VPXBooleanDecoder boolDec, int prob) {
        int update_prob = boolDec.readBit(252);
        if (update_prob == 1) {
            int deltaProb = decodeTermSubexp(boolDec);
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

    private int invRemapProb(int deltaProb, int prob) {
        int m = prob;
        int v = deltaProb;
        v = INV_REMAP_TABLE[v];
        m--;
        if ((m << 1) <= 255)
            m = 1 + invRecenterNonneg(v, m);
        else
            m = 255 - invRecenterNonneg(v, 255 - 1 - m);
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
            if (update_probs == 1)
                for (int i = 0; i < 2; i++)
                    for (int j = 0; j < 2; j++)
                        for (int k = 0; k < 6; k++) {
                            int maxL = (k == 0) ? 3 : 6;
                            for (int l = 0; l < maxL; l++)
                                for (int m = 0; m < 3; m++)
                                    coefProbs[txSz][i][j][k][l][m] = diffUpdateProb(boolDec,
                                            coefProbs[txSz][i][j][k][l][m]);
                        }
        }
    }

    private void readSkipProb(VPXBooleanDecoder boolDec) {
        for (int i = 0; i < SKIP_CONTEXTS; i++)
            skipProb[i] = diffUpdateProb(boolDec, skipProb[i]);
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
            isInterProb[i] = diffUpdateProb(boolDec, isInterProb[i]);
    }

    private void frameReferenceMode(VPXBooleanDecoder boolDec) {
        int compoundReferenceAllowed = 0;
        for (int i = 1; i < REFS_PER_FRAME; i++)
            if (_refFrameSignBias[i + 1] != _refFrameSignBias[1])
                compoundReferenceAllowed = 1;
        if (compoundReferenceAllowed == 1) {
            int non_single_reference = boolDec.readBitEq();
            if (non_single_reference == 0) {
                referenceMode = SINGLE_REF;
            } else {
                int reference_select = boolDec.readBitEq();
                if (reference_select == 0)
                    referenceMode = COMPOUND_REF;
                else
                    referenceMode = REFERENCE_MODE_SELECT;
                setupCompoundReferenceMode();
            }
        } else {
            referenceMode = SINGLE_REF;
        }
    }

    private void frameReferenceModeProbs(VPXBooleanDecoder boolDec) {
        if (referenceMode == REFERENCE_MODE_SELECT) {
            for (int i = 0; i < COMP_MODE_CONTEXTS; i++)
                compModeProb[i] = diffUpdateProb(boolDec, compModeProb[i]);
        }
        if (referenceMode != COMPOUND_REF) {
            for (int i = 0; i < REF_CONTEXTS; i++) {
                singleRefProb[i][0] = diffUpdateProb(boolDec, singleRefProb[i][0]);
                singleRefProb[i][1] = diffUpdateProb(boolDec, singleRefProb[i][1]);
            }
        }
        if (referenceMode != SINGLE_REF) {
            for (int i = 0; i < REF_CONTEXTS; i++)
                compRefProb[i] = diffUpdateProb(boolDec, compRefProb[i]);
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
            mvSignProb[i] = updateMvProb(boolDec, mvSignProb[i]);
            for (int j = 0; j < MV_CLASSES - 1; j++)
                mvClassProbs[i][j] = updateMvProb(boolDec, mvClassProbs[i][j]);
            mvClass0BitProb[i] = updateMvProb(boolDec, mvClass0BitProb[i]);
            for (int j = 0; j < MV_OFFSET_BITS; j++)
                mvBitsProb[i][j] = updateMvProb(boolDec, mvBitsProb[i][j]);
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
                mvHpProb[i] = updateMvProb(boolDec, mvHpProb[i]);
            }
        }
    }

    private int updateMvProb(VPXBooleanDecoder boolDec, int prob) {
        int update_mv_prob = boolDec.readBit(252);
        if (update_mv_prob == 1) {
            int mv_prob = boolDec.decodeInt(7);
            prob = (mv_prob << 1) | 1;
        }
        return prob;
    }

    private void setupCompoundReferenceMode() {
        if (_refFrameSignBias[LAST_FRAME] == _refFrameSignBias[GOLDEN_FRAME]) {
            compFixedRef = ALTREF_FRAME;
            compVarRef0 = LAST_FRAME;
            compVarRef1 = GOLDEN_FRAME;
        } else if (_refFrameSignBias[LAST_FRAME] == _refFrameSignBias[ALTREF_FRAME]) {
            compFixedRef = GOLDEN_FRAME;
            compVarRef0 = LAST_FRAME;
            compVarRef1 = ALTREF_FRAME;
        } else {
            compFixedRef = LAST_FRAME;
            compVarRef0 = GOLDEN_FRAME;
            compVarRef1 = ALTREF_FRAME;
        }
    }
}
