package org.jcodec.codecs.vpx.vp9;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DecodingContext {

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
}
