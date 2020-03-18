package org.jcodec.codecs.h264.encode;

import static org.jcodec.codecs.h264.H264Const.BLK_DISP_MAP;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jcodec.Utils;
import org.jcodec.codecs.h264.H264Encoder.NonRdVector;
import org.jcodec.codecs.h264.decode.DeblockerInput;
import org.jcodec.codecs.h264.decode.DecoderState;
import org.jcodec.codecs.h264.decode.Intra4x4PredictionBuilder;
import org.jcodec.codecs.h264.decode.MBlock;
import org.jcodec.codecs.h264.decode.MBlockDecoderIntraNxN;
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

public class MBWriterINxNTest {
    @Test
    public void testGrad() {
        for (int p = 0; p < 16; p++) {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    Picture left = Utils.buildSmoothRandomPic(16, 16, 50, 0.2);
                    Picture top = Utils.buildSmoothRandomPic(16, 16, 50, 0.2);
                    Picture pic = Utils.buildSmoothRandomPic(16, 16, 50, 0.2);
                    testForPic(pic, j == 0 ? null : left, i == 0 ? null : top, j, i, 2, 2);
                }
            }
        }
    }

    @Test
    public void testChess() {
        Picture pic = createChess((byte) 127);

        testForPic(pic, pic, pic, 1, 1, 2, 2);
    }

    private Picture createChess(byte max) {
        Picture pic = Picture.create(16, 16, ColorSpace.YUV420J);
        byte[] plane0 = pic.getPlaneData(0);
        Arrays.fill(plane0, (byte) -128);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                plane0[(j << 4) + i + 0 + 0] = max;
                plane0[(j << 4) + i + 8 + 0] = max;
                plane0[(j << 4) + i + 4 + 64] = max;
                plane0[(j << 4) + i + 12 + 64] = max;
                plane0[(j << 4) + i + 0 + 128] = max;
                plane0[(j << 4) + i + 8 + 128] = max;
                plane0[(j << 4) + i + 4 + 192] = max;
                plane0[(j << 4) + i + 12 + 192] = max;
            }
        }
        return pic;
    }

    private void testForPic(Picture pic, Picture left, Picture top, int mbX, int mbY, int mbW, int mbH) {
        oneTest(pic, left, top, new int[] {2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 6, 2, 2, 2, 2}, mbX, mbY, mbW, mbH);
        for (int i = 0; i < 1024; i++) {
            int[] lumaPred = new int[16];
            for (int bInd = 0; bInd < 16; bInd++) {
                int dInd = BLK_DISP_MAP[bInd];
                boolean hasLeft = (dInd & 0x3) != 0 || mbX != 0;
                boolean hasTop = dInd >= 4 || mbY != 0;
                do {
                    lumaPred[bInd] = Math.min(8, (int) (Math.random() * 9));
                } while (!Intra4x4PredictionBuilder.available(lumaPred[bInd], hasLeft, hasTop));
            }
            oneTest(pic, left, top, lumaPred, mbX, mbY, mbW, mbH);
        }
    }

    private void oneTest(Picture pic, Picture left, Picture top, int[] lumaPred, int mbX, int mbY, int mbW, int mbH) {
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
        NonRdVector params = new NonRdVector(null, 0, lumaPred, chrPred);
        EncodedMB outMB = new EncodedMB();
        new MBWriterINxN().encodeMacroblock(ctx, pic, mbX, mbY, out, outMB, qp, params);
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
        mBlock.curMbType = MBType.I_NxN;
        mBlock.mbIdx = mbX + mbW * mbY;
        sr.readIntraNxN(mBlock);
        for (int i = 0; i < 16; i++)
            Assert.assertTrue(mBlock.lumaModes[i] == lumaPred[i]);
        DeblockerInput di = new DeblockerInput(sps);

        MBlockDecoderIntraNxN decoder = new MBlockDecoderIntraNxN(mapper, sh, di, 0, s);
        Picture mb = Picture.create(16, 16, ColorSpace.YUV420J);
        decoder.decode(mBlock, mb);

        for (int p = 0; p < 3; p++)
            Assert.assertTrue(Arrays.equals(outMB.getPixels().getPlaneData(p), mb.getPlaneData(p)));
    }
}
