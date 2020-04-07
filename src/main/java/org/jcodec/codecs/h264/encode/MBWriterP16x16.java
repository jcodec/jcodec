package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_LEFT;
import static org.jcodec.codecs.h264.H264Const.MB_BLK_OFF_TOP;
import static org.jcodec.codecs.h264.encode.H264EncoderUtils.median;
import static org.jcodec.codecs.h264.io.model.MBType.P_16x16;

import java.util.Arrays;

import org.jcodec.codecs.h264.H264Const;
import org.jcodec.codecs.h264.H264Encoder.NonRdVector;
import org.jcodec.codecs.h264.decode.BlockInterpolator;
import org.jcodec.codecs.h264.decode.CoeffTransformer;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.write.CAVLCWriter;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Encodes macroblock as P16x16
 * 
 * @author Stanislav Vitvitskyy
 */
public class MBWriterP16x16 {
    private SeqParameterSet sps;
    private Picture ref;

    private BlockInterpolator interpolator;

    public MBWriterP16x16(SeqParameterSet sps, Picture ref) {
        this.sps = sps;
        this.ref = ref;
        interpolator = new BlockInterpolator();
    }

    public void encodeMacroblock(EncodingContext ctx, Picture pic, int mbX, int mbY, BitWriter out, EncodedMB outMB,
            int qp,  NonRdVector params) {
        if (sps.numRefFrames > 1) {
            int refIdx = decideRef();
            CAVLCWriter.writeTE(out, refIdx, sps.numRefFrames - 1);
        }
        int partBlkSize = 4; // 16x16
        int refIdx = 1;

        boolean trAvb = mbY > 0 && mbX < sps.picWidthInMbsMinus1;
        boolean tlAvb = mbX > 0 && mbY > 0;
        int ax = ctx.mvLeftX[0];
        int ay = ctx.mvLeftY[0];
        boolean ar = ctx.mvLeftR[0] == refIdx;

        int bx = ctx.mvTopX[mbX << 2];
        int by = ctx.mvTopY[mbX << 2];
        boolean br = ctx.mvTopR[mbX << 2] == refIdx;

        int cx = trAvb ? ctx.mvTopX[(mbX << 2) + partBlkSize] : 0;
        int cy = trAvb ? ctx.mvTopY[(mbX << 2) + partBlkSize] : 0;
        boolean cr = trAvb ? ctx.mvTopR[(mbX << 2) + partBlkSize] == refIdx : false;

        int dx = tlAvb ? ctx.mvTopLeftX : 0;
        int dy = tlAvb ? ctx.mvTopLeftY : 0;
        boolean dr = tlAvb ? (ctx.mvTopLeftR == refIdx) : false;
        int mvpx = median(ax, ar, bx, br, cx, cr, dx, dr, mbX > 0, mbY > 0, trAvb, tlAvb);
        int mvpy = median(ay, ar, by, br, cy, cr, dy, dr, mbX > 0, mbY > 0, trAvb, tlAvb);

        // Motion estimation for the current macroblock
        CAVLCWriter.writeSE(out, params.mv[0] - mvpx); // mvdx
        CAVLCWriter.writeSE(out, params.mv[1] - mvpy); // mvdy

        Picture mbRef = Picture.create(16, 16, sps.chromaFormatIdc);
        int[][] mb = new int[][] { new int[256], new int[64], new int[64] };

        interpolator.getBlockLuma(ref, mbRef, 0, (mbX << 6) + params.mv[0], (mbY << 6) + params.mv[1], 16, 16);

        BlockInterpolator.getBlockChroma(ref.getPlaneData(1), ref.getPlaneWidth(1), ref.getPlaneHeight(1),
                mbRef.getPlaneData(1), 0, mbRef.getPlaneWidth(1), (mbX << 6) + params.mv[0], (mbY << 6) + params.mv[1], 8, 8);
        BlockInterpolator.getBlockChroma(ref.getPlaneData(2), ref.getPlaneWidth(2), ref.getPlaneHeight(2),
                mbRef.getPlaneData(2), 0, mbRef.getPlaneWidth(2), (mbX << 6) + params.mv[0], (mbY << 6) + params.mv[1], 8, 8);

        MBEncoderHelper.takeSubtract(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX << 4,
                mbY << 4, mb[0], mbRef.getPlaneData(0), 16, 16);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(1), pic.getPlaneWidth(1), pic.getPlaneHeight(1), mbX << 3,
                mbY << 3, mb[1], mbRef.getPlaneData(1), 8, 8);
        MBEncoderHelper.takeSubtract(pic.getPlaneData(2), pic.getPlaneWidth(2), pic.getPlaneHeight(2), mbX << 3,
                mbY << 3, mb[2], mbRef.getPlaneData(2), 8, 8);

        int codedBlockPattern = getCodedBlockPattern();
        CAVLCWriter.writeUE(out, H264Const.CODED_BLOCK_PATTERN_INTER_COLOR_INV[codedBlockPattern]);

        CAVLCWriter.writeSE(out, qp - ctx.prevQp);

        luma(ctx, pic, mb[0], mbX, mbY, out, qp, outMB.nc);
        chroma(ctx, pic, mb[1], mb[2], mbX, mbY, out, qp);

        MBEncoderHelper.putBlk(outMB.pixels.getPlaneData(0), mb[0], mbRef.getPlaneData(0), 4, 0, 0, 16, 16);
        MBEncoderHelper.putBlk(outMB.pixels.getPlaneData(1), mb[1], mbRef.getPlaneData(1), 3, 0, 0, 8, 8);
        MBEncoderHelper.putBlk(outMB.pixels.getPlaneData(2), mb[2], mbRef.getPlaneData(2), 3, 0, 0, 8, 8);

        Arrays.fill(outMB.mx, params.mv[0]);
        Arrays.fill(outMB.my, params.mv[1]);
        Arrays.fill(outMB.mr, refIdx);
        outMB.type = MBType.P_16x16;
        outMB.qp = qp;
        ctx.prevQp = qp;
    }

    private int getCodedBlockPattern() {
        return 47;
    }

    /**
     * Decides which reference to use
     * 
     * @return
     */
    private int decideRef() {
        return 0;
    }

    private void luma(EncodingContext ctx, Picture pic, int[] pix, int mbX, int mbY, BitWriter out, int qp, int[] nc) {
        int[][] ac = new int[16][16];
        for (int i = 0; i < ac.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_4x4[i].length; j++) {
                ac[i][j] = pix[H264Const.PIX_MAP_SPLIT_4x4[i][j]];
            }
            CoeffTransformer.fdct4x4(ac[i]);
        }

        writeAC(ctx, 0, mbX, mbY, out, mbX << 2, mbY << 2, ac, qp, nc);

        for (int i = 0; i < ac.length; i++) {
            CoeffTransformer.dequantizeAC(ac[i], qp, null);
            CoeffTransformer.idct4x4(ac[i]);
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_4x4[i].length; j++)
                pix[H264Const.PIX_MAP_SPLIT_4x4[i][j]] = ac[i][j];
        }
    }

    private void chroma(EncodingContext ctx, Picture pic, int[] pix1, int[] pix2, int mbX, int mbY, BitWriter out,
            int qp) {
        int[][] ac1 = new int[4][16];
        int[][] ac2 = new int[4][16];
        for (int i = 0; i < ac1.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                ac1[i][j] = pix1[H264Const.PIX_MAP_SPLIT_2x2[i][j]];
        }
        for (int i = 0; i < ac2.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                ac2[i][j] = pix2[H264Const.PIX_MAP_SPLIT_2x2[i][j]];
        }
        MBWriterI16x16.chromaResidual(mbX, mbY, out, qp, ac1, ac2, ctx.cavlc[1], ctx.cavlc[2], P_16x16, P_16x16);

        for (int i = 0; i < ac1.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                pix1[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac1[i][j];
        }
        for (int i = 0; i < ac2.length; i++) {
            for (int j = 0; j < H264Const.PIX_MAP_SPLIT_2x2[i].length; j++)
                pix2[H264Const.PIX_MAP_SPLIT_2x2[i][j]] = ac2[i][j];
        }
    }

    private void writeAC(EncodingContext ctx, int comp, int mbX, int mbY, BitWriter out, int mbLeftBlk, int mbTopBlk,
            int[][] ac, int qp, int[] nc) {
        for (int i = 0; i < ac.length; i++) {
            int blkI = H264Const.BLK_INV_MAP[i];
            CoeffTransformer.quantizeAC(ac[blkI], qp);
            int coeffToken = ctx.cavlc[comp].writeACBlock(out, mbLeftBlk + MB_BLK_OFF_LEFT[i], mbTopBlk + MB_BLK_OFF_TOP[i], P_16x16,
                    P_16x16, ac[blkI], H264Const.totalZeros16, 0, 16, CoeffTransformer.zigzag4x4);
            nc[blkI] = coeffToken >> 4; // total coeff
        }
    }
}