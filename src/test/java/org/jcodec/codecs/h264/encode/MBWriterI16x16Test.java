package org.jcodec.codecs.h264.encode;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.Utils;
import org.jcodec.codecs.h264.H264Encoder.NonRdVector;
import org.jcodec.codecs.h264.decode.DeblockerInput;
import org.jcodec.codecs.h264.decode.DecoderState;
import org.jcodec.codecs.h264.decode.MBlock;
import org.jcodec.codecs.h264.decode.MBlockDecoderIntra16x16;
import org.jcodec.codecs.h264.decode.SliceReader;
import org.jcodec.codecs.h264.decode.aso.FlatMBlockMapper;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.MBType;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class MBWriterI16x16Test {
    @Test
    public void testGrad() {
//        byte[][] data= new byte[][] {{-26, -25, -18, 1, 11, 0, 3, 24, -2, 22, -7, 0, -14, 1, 3, 1, -13, -13, 12, 5, 7, 15, 10, 22, 10, -7, 18, 13, 18, 4, -1, -24, -27, -13, -10, -25, -3, 17, -15, 29, -3, 6, 16, 26, 31, 21, -1, -6, 2, -14, -19, -16, -23, 12, 2, 38, -14, 20, 26, 40, 48, 32, -4, -25, 14, 16, 10, 0, -14, -24, -31, -24, 5, 6, 0, 18, 38, 32, 22, -6, -9, -1, 10, 12, 4, -7, -7, 14, -14, 1, -8, 22, 8, 6, -15, -25, -14, 7, 26, 9, 0, -23, -32, -1, 5, 23, -12, -19, -32, -4, -14, -26, 5, 19, -3, -23, -30, -3, -23, -5, 10, 29, 46, 56, 55, 58, 54, 38, -4, -8, -17, 6, -1, -16, -13, 3, -7, -17, -35, -34, -29, -34, -20, 2, 7, -14, -5, -4, -1, -24, -2, 8, 21, -16, -12, -36, -12, -1, -12, 11, 3, -23, -29, -17, -11, 10, -13, -15, 14, 23, 0, -6, -25, 7, -1, 26, 20, 9, -7, -23, -1, 18, -1, 19, 27, 6, 10, -14, -30, -23, -14, 6, 22, 15, -6, 5, -2, 6, 10, 15, 35, 37, 3, -4, -21, -16, -35, -34, -1, -9, -17, -21, -14, -12, 4, 10, -4, 12, -16, 12, 11, -1, -24, -45, 20, -17, -34, -30, -11, -32, 1, 17, -13, -10, -2, -1, -5, -28, -34, -22, -3, 12, -9, -17, -20, -37, -41, -25, -22, -31, -20, -2, -10, -33, -12, -17},
//            {-62, -37, -13, -1, -13, -10, 0, 5, -32, -9, 7, -17, 12, 6, 0, -1, -50, 2, 25, 2, 1, -3, -20, -18, -30, 17, 27, -1, 0, -10, -16, -6, -40, 16, 4, -2, -23, -10, -35, -19, -46, 6, 3, 15, 4, 13, -19, 5, -51, -13, -2, -12, 18, -3, 3, 28, -42, 3, -14, -26, 15, -18, -11, -8},
//            {72, 54, 44, 54, 31, 11, -1, 8, 44, 37, 57, 65, 42, 36, 16, -13, 52, 15, 36, 46, 32, 36, 4, -5, 57, 37, 11, 48, 9, 13, -4, 15, 70, 36, 16, 58, 24, 9, 7, 28, 70, 9, -5, 65, -5, 30, -12, 37, 53, -5, -14, 60, 0, 15, 4, 33, 20, -15, 0, 61, -10, -7, 10, 39}};
        for (int t = 0; t < 1024; t++) {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    Picture left = Utils.buildSmoothRandomPic(16, 16, 50, 0.2);
                    Picture top = Utils.buildSmoothRandomPic(16, 16, 50, 0.2);
                    // Picture pic = Picture.createPicture(16, 16, data,
                    // ColorSpace.YUV420);//Utils.buildSmoothRandomPic(16, 16, 50, 0.2);
                    Picture pic = Utils.buildSmoothRandomPic(16, 16, 50, 0.2);
                    oneTest(pic, j == 0 ? null : left, i == 0 ? null : top, j, i, 2, 2);
                }
            }
        }
    }

    private void oneTest(Picture pic, Picture left, Picture top, int mbX, int mbY, int mbW, int mbH) {
        System.out.println();
        int qp = 31;
        SeqParameterSet sps = new SeqParameterSet();
        PictureParameterSet pps = new PictureParameterSet();
        pps.picInitQpMinus26 = qp - 26;
        sps.chromaFormatIdc = ColorSpace.YUV420J;
        sps.picWidthInMbsMinus1 = mbW - 1;
        SliceHeader sh = new SliceHeader();
        sh.sps = sps;
        sh.pps = pps;
        DecoderState s = new DecoderState(sh);
        EncodingContext ctx = new EncodingContext(2, 2);
        for (int i = 0; i < 16; i++) {
            if (left != null)
                s.leftRow[0][i] = ctx.leftRow[0][i] = left.getPlaneData(0)[15 + (i << 4)];
            if (top != null)
                s.topLine[0][(mbX << 4) + i] = ctx.topLine[0][(mbX << 4) + i] = top.getPlaneData(0)[240 + i];
        }
        if (left != null) {
            s.topLeft[0][0] = ctx.topLeft[0] = 0;
            s.topLeft[0][1] = ctx.topLeft[1] = left.getPlaneData(0)[63];
            s.topLeft[0][2] = ctx.topLeft[2] = left.getPlaneData(0)[127];
            s.topLeft[0][3] = ctx.topLeft[3] = left.getPlaneData(0)[191];
        }

        ctx.cavlc = new CAVLC[] { new CAVLC(sps, null, 2, 2), new CAVLC(sps, null, 1, 1), new CAVLC(sps, null, 1, 1) };
        ctx.prevQp = qp;
        ByteBuffer buf = ByteBuffer.allocate(1024);
        BitWriter out = new BitWriter(buf);
        int chrPred = 2;
        NonRdVector params = new NonRdVector(null, 2, null, chrPred);
        EncodedMB outMB = new EncodedMB();
        boolean cbpLuma = new MBWriterI16x16().encodeMacroblock(ctx, pic, mbX, mbY, out, outMB, qp, params);
        int cbpChroma = 2;
        out.flush();
        buf.flip();

        // READ
        BitReader br = BitReader.createBitReader(buf);
        sh.sliceType = SliceType.I;
        CAVLC[] cavlc = new CAVLC[] { new CAVLC(sh.sps, sh.pps, 2, 2), new CAVLC(sh.sps, sh.pps, 1, 1),
                new CAVLC(sh.sps, sh.pps, 1, 1) };
        FlatMBlockMapper mapper = new FlatMBlockMapper(mbW, 0);
        SliceReader sr = new SliceReader(pps, null, cavlc, null, br, mapper, sh, null);
        MBlock mBlock = new MBlock(ColorSpace.YUV420J);
        mBlock.curMbType = MBType.I_16x16;
        mBlock.mbIdx = mbX + mbW * mbY;
        int i16x16TypeOffset = (cbpLuma ? 12 : 0) + cbpChroma * 4 + params.lumaPred16x16;

        sr.readIntra16x16(i16x16TypeOffset, mBlock);
        DeblockerInput di = new DeblockerInput(sps);

        MBlockDecoderIntra16x16 decoder = new MBlockDecoderIntra16x16(mapper, sh, di, 0, s);
        Picture mb = Picture.create(16, 16, ColorSpace.YUV420J);
        decoder.decode(mBlock, mb);

        for (int p = 0; p < 3; p++)
            Assert.assertTrue(String.format("%d", p),
                    Arrays.equals(outMB.getPixels().getPlaneData(p), mb.getPlaneData(p)));
    }
}
