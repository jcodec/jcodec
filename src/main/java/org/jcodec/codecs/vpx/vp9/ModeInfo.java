package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X4;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X8;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X4;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X8;
import static org.jcodec.codecs.vpx.vp9.Consts.DC_PRED;
import static org.jcodec.codecs.vpx.vp9.Consts.SEG_LVL_SKIP;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_INTRA_MODE;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_SEGMENT_ID;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_TX_SIZE;
import static org.jcodec.codecs.vpx.vp9.Consts.TX_4X4;
import static org.jcodec.codecs.vpx.vp9.Consts.TX_MODE_SELECT;
import static org.jcodec.codecs.vpx.vp9.Consts.blH;
import static org.jcodec.codecs.vpx.vp9.Consts.blW;
import static org.jcodec.codecs.vpx.vp9.Consts.maxTxLookup;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ModeInfo {

    private int segmentId;
    private boolean skip;
    private int txSize;
    private int yMode;
    private int subModes;
    private int uvMode;

    ModeInfo() {

    }

    public ModeInfo(int segmentId, boolean skip, int txSize, int yMode, int subModes, int uvMode) {
        this.segmentId = segmentId;
        this.skip = skip;
        this.txSize = txSize;
        this.yMode = yMode;
        this.subModes = subModes;
        this.uvMode = uvMode;
    }

    public int getSegmentId() {
        return segmentId;
    }

    public boolean isSkip() {
        return skip;
    }

    public int getTxSize() {
        return txSize;
    }

    public int getYMode() {
        return yMode;
    }

    public int getSubModes() {
        return subModes;
    }

    public int getUvMode() {
        return uvMode;
    }

    public ModeInfo read(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
        int segmentId = 0;
        if (c.isSegmentationEnabled() && c.isUpdateSegmentMap())
            segmentId = readSegmentId(decoder, c);

        boolean skip = true;
        if (!c.isSegmentFeatureActive(segmentId, SEG_LVL_SKIP))
            skip = readSkipFlag(miCol, miRow, blSz, decoder, c);

        int txSize = readTxSize(miCol, miRow, blSz, true, decoder, c);

        int yMode;
        int subModes = 0;
        if (blSz >= BLOCK_8X8) {
            yMode = readKfIntraMode(miCol, miRow, blSz, decoder, c);
        } else {
            subModes = readKfIntraModeSub(miCol, miRow, blSz, decoder, c);
            // last submode is always the lowest byte
            yMode = subModes & 0xff;
        }
        int uvMode = readInterIntraUvMode(yMode, decoder, c);

        return new ModeInfo(segmentId, skip, txSize, yMode, subModes, uvMode);
    }

    public int readKfIntraMode(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getMiTileStartCol(); // Tile based
        int[] aboveIntraModes = c.getAboveModes();
        int[] leftIntraModes = c.getLeftModes();
        int aboveMode;
        int leftMode;
        aboveMode = availAbove ? aboveIntraModes[miCol] : DC_PRED;
        leftMode = availLeft ? leftIntraModes[miRow % 8] : DC_PRED;

        int[][][] probs = c.getKfYModeProbs();

        int intraMode = decoder.readTree(TREE_INTRA_MODE, probs[aboveMode][leftMode]);

        aboveIntraModes[miCol] = intraMode;
        leftIntraModes[miRow % 8] = intraMode;
        return intraMode;
    }

    public int readKfIntraModeSub(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getMiTileStartCol(); // Tile based
        int[] aboveIntraModes = c.getAboveModes();
        int[] leftIntraModes = c.getLeftModes();
        int aboveMode;
        int leftMode;
        int[][][] probs = c.getKfYModeProbs();
        aboveMode = availAbove ? aboveIntraModes[miCol] : DC_PRED;
        leftMode = availLeft ? leftIntraModes[miRow & 0x7] : DC_PRED;
        int mode0 = decoder.readTree(TREE_INTRA_MODE, probs[aboveMode][leftMode]);
        int mode1 = 0, mode2 = 0, mode3 = 0;

        if (blSz == BLOCK_4X4) {
            mode1 = decoder.readTree(TREE_INTRA_MODE, probs[aboveMode][mode0]);
            mode2 = decoder.readTree(TREE_INTRA_MODE, probs[mode0][leftMode]);
            mode3 = decoder.readTree(TREE_INTRA_MODE, probs[mode1][mode2]);
            aboveIntraModes[miCol] = mode2;
            leftIntraModes[miRow & 0x7] = mode1;
            return vect4(mode0, mode1, mode2, mode3);
        } else if (blSz == BLOCK_4X8) {
            mode1 = decoder.readTree(TREE_INTRA_MODE, probs[aboveMode][mode0]);
            aboveIntraModes[miCol] = mode0;
            leftIntraModes[miRow & 0x7] = mode1;
            return vect4(mode0, mode1, mode0, mode1);
        } else if (blSz == BLOCK_8X4) {
            mode1 = decoder.readTree(TREE_INTRA_MODE, probs[mode0][leftMode]);
            aboveIntraModes[miCol] = mode1;
            leftIntraModes[miRow & 0x7] = mode0;
            return vect4(mode0, mode0, mode1, mode1);
        }

        return 0;
    }

    public static int vect4(int val0, int val1, int val2, int val3) {
        return (val0) | (val1 << 8) | (val2 << 16) | (val3 << 24);
    }

    public static int vect4get(int vect, int ind) {
        return (vect >> (ind << 3)) & 0xff;
    }

    public int readTxSize(int miCol, int miRow, int blSz, boolean allowSelect, VPXBooleanDecoder decoder,
            DecodingContext c) {
        if (blSz < BLOCK_8X8)
            return TX_4X4;

        int maxTxSize = maxTxLookup[blSz]; // 4x4 being 0, 32x32 being 3
        int txSize = Math.min(maxTxSize, c.getTxMode());
        if (allowSelect && c.getTxMode() == TX_MODE_SELECT) {
            boolean availAbove = miRow > 0; // Frame based
            boolean availLeft = miCol > c.getMiTileStartCol(); // Tile based
            int above = maxTxSize;
            int left = maxTxSize;
            if (availAbove && !c.getAboveSkipped()[miCol])
                above = c.getAboveTxSizes()[miCol];
            if (availLeft && !c.getLeftSkipped()[miRow & 0x7])
                left = c.getLeftTxSizes()[miRow & 0x7];
            if (!availLeft)
                left = above;
            if (!availAbove)
                above = left;
            int ctx = (above + left) > maxTxSize ? 1 : 0;

            int[][] probs = null;
            switch (maxTxSize) {
            case 3:
                probs = c.getTx32x32Probs();
                break;
            case 2:
                probs = c.getTx16x16Probs();
                break;
            case 1:
                probs = c.getTx8x8Probs();
                break;
            default:
                throw new RuntimeException("Shouldn't happen");
            }
            txSize = decoder.readTree(TREE_TX_SIZE[maxTxSize], probs[ctx]);
        } else {
            txSize = Math.min(maxTxSize, c.getTxMode());
        }

        for (int i = 0; i < blH[blSz]; i++) {
            c.getLeftTxSizes()[(miRow + i) & 0x7] = txSize;
        }
        for (int j = 0; j < blW[blSz]; j++) {
            c.getAboveTxSizes()[(miCol + j) & 0x7] = txSize;
        }

        return txSize;
    }

    public static int readSegmentId(VPXBooleanDecoder decoder, DecodingContext c) {
        int[] probs = c.getSegmentationTreeProbs();
        return decoder.readTree(TREE_SEGMENT_ID, probs);
    }

    public boolean readSkipFlag(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
        int ctx = 0;
        boolean availAbove = miRow > 0; // Frame based
        boolean availLeft = miCol > c.getMiTileStartCol(); // Tile based

        boolean[] aboveSkipped = c.getAboveSkipped();
        boolean[] leftSkipped = c.getLeftSkipped();

        if (availAbove)
            ctx += aboveSkipped[miCol] ? 1 : 0;
        if (availLeft)
            ctx += leftSkipped[miRow & 0x7] ? 1 : 0;

        System.out.println("SKIP CTX: " + ctx);

        int[] probs = c.getSkipProbs();

        boolean ret = decoder.readBit(probs[ctx]) == 1;

        for (int i = 0; i < blH[blSz]; i++) {
            leftSkipped[(i + miRow) & 0x7] = ret;
        }

        for (int j = 0; j < blW[blSz]; j++) {
            aboveSkipped[j + miCol] = ret;
        }

        return ret;
    }

    public boolean isInter() {
        return false;
    }

    public int readInterIntraUvMode(int yMode, VPXBooleanDecoder decoder, DecodingContext c) {
        int[][] probs = c.getKfUVModeProbs();
        return decoder.readTree(TREE_INTRA_MODE, probs[yMode]);
    }
}
