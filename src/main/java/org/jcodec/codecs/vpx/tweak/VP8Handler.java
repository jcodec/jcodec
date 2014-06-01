package org.jcodec.codecs.vpx.tweak;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Abstract vp8 handler
 * 
 * @author The JCodec project
 * 
 */
public interface VP8Handler {

    void macroblockIntra(int mbAddr, int segId, int[] lumaDC, int[][] lumaAC, int[][][] chroma, int lumaMode,
            int chromaMode);

    void macroblockIntraBPred(int mbAddr, int segId, int[][] luma, int[][][] chroma, int[] lumaMode, int chromaMode);

    void macroblockInter(int mbAddr, int segId, int[] lumaDC, int[][] lumaAC, int[][][] chroma, int mv);

    void macroblockInterSplit(int mbAddr, int segId, int[][] luma, int[][][] chroma, int splitMode, int[] mvs);

    void beginFrame(int[] qps);
}