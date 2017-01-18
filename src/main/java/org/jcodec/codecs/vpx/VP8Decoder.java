package org.jcodec.codecs.vpx;
import static org.jcodec.codecs.vpx.VP8Util.MAX_MODE_LF_DELTAS;
import static org.jcodec.codecs.vpx.VP8Util.MAX_REF_LF_DELTAS;
import static org.jcodec.codecs.vpx.VP8Util.getBitInBytes;
import static org.jcodec.codecs.vpx.VP8Util.getBitsInBytes;
import static org.jcodec.codecs.vpx.VP8Util.getDefaultCoefProbs;
import static org.jcodec.codecs.vpx.VP8Util.getMacroblockCount;
import static org.jcodec.codecs.vpx.VP8Util.keyFrameYModeProb;
import static org.jcodec.codecs.vpx.VP8Util.keyFrameYModeTree;
import static org.jcodec.codecs.vpx.VP8Util.vp8CoefUpdateProbs;

import java.nio.ByteBuffer;

import org.jcodec.api.NotSupportedException;
import org.jcodec.codecs.vpx.Macroblock.Subblock;
import org.jcodec.codecs.vpx.VP8Util.QuantizationParams;
import org.jcodec.codecs.vpx.VP8Util.SubblockConstants;
import org.jcodec.common.Assert;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VP8Decoder extends VideoDecoder {

    @Override
    public Picture8Bit decodeFrame8Bit(ByteBuffer frame, byte[][] buffer) {
        byte[] firstThree = new byte[3];
        frame.get(firstThree);

        boolean keyFrame = getBitInBytes(firstThree, 0) == 0;
        if(!keyFrame)
            return null;
        int version = getBitsInBytes(firstThree, 1, 3);
        boolean showFrame = getBitInBytes(firstThree, 4) > 0;
        int partitionSize = getBitsInBytes(firstThree, 5, 19);
        String threeByteToken = printHexByte(frame.get()) + " " + printHexByte(frame.get()) + " "
                + printHexByte(frame.get());

        int twoBytesWidth = (frame.get() & 0xFF) | (frame.get() & 0xFF) << 8;
        int twoBytesHeight = (frame.get() & 0xFF) | (frame.get() & 0xFF) << 8;
        int width = (twoBytesWidth & 0x3fff);
        int height = (twoBytesHeight & 0x3fff);
        int numberOfMBRows = getMacroblockCount(height);
        int numberOfMBCols = getMacroblockCount(width);

        /** Init macroblocks and subblocks */
        Macroblock[][] mbs = new Macroblock[numberOfMBRows + 2][numberOfMBCols + 2];
        for (int row = 0; row < numberOfMBRows + 2; row++)
            for (int col = 0; col < numberOfMBCols + 2; col++)
                mbs[row][col] = new Macroblock(row, col);

        int headerOffset = frame.position();
        VPXBooleanDecoder headerDecoder = new VPXBooleanDecoder(frame, 0);
        boolean isYUVColorSpace = (headerDecoder.decodeBit() == 0);

        boolean clampingRequired = headerDecoder.decodeBit() == 0;
        int segmentation = headerDecoder.decodeBit();
        Assert.assertEquals("Frame has segmentation, segment decoding is not ", 0, segmentation);
        int simpleFilter = headerDecoder.decodeBit();
        int filterLevel = headerDecoder.decodeInt(6);
        int filterType = (filterLevel == 0) ? 0 : (simpleFilter > 0) ? 1 : 2;
        int sharpnessLevel = headerDecoder.decodeInt(3);
        int loopFilterDeltaFlag = headerDecoder.decodeBit();
        Assert.assertEquals(1, loopFilterDeltaFlag);
        int loopFilterDeltaUpdate = headerDecoder.decodeBit();
        Assert.assertEquals(1, loopFilterDeltaUpdate);
        int[] refLoopFilterDeltas = new int[MAX_REF_LF_DELTAS];
        int[] modeLoopFilterDeltas = new int[MAX_MODE_LF_DELTAS];
        for (int i = 0; i < MAX_REF_LF_DELTAS; i++) {

            if (headerDecoder.decodeBit() > 0) {
                refLoopFilterDeltas[i] = headerDecoder.decodeInt(6);
                ;
                if (headerDecoder.decodeBit() > 0) // Apply sign
                    refLoopFilterDeltas[i] = refLoopFilterDeltas[i] * -1;
            }
        }
        for (int i = 0; i < MAX_MODE_LF_DELTAS; i++) {

            if (headerDecoder.decodeBit() > 0) {
                modeLoopFilterDeltas[i] = headerDecoder.decodeInt(6);
                if (headerDecoder.decodeBit() > 0) // Apply sign
                    modeLoopFilterDeltas[i] = modeLoopFilterDeltas[i] * -1;
            }
        }
        int log2OfPartCnt = headerDecoder.decodeInt(2);

        Assert.assertEquals(0, log2OfPartCnt);
        int partitionsCount = 1;
        long runningSize = 0;
        long zSize = frame.limit() - (partitionSize + headerOffset);
        ByteBuffer tokenBuffer = frame.duplicate();
        tokenBuffer.position(partitionSize + headerOffset);
        VPXBooleanDecoder decoder = new VPXBooleanDecoder(tokenBuffer, 0);

        int yacIndex = headerDecoder.decodeInt(7);
        int ydcDelta = ((headerDecoder.decodeBit() > 0) ? VP8Util.delta(headerDecoder) : 0);
        int y2dcDelta = ((headerDecoder.decodeBit() > 0) ? VP8Util.delta(headerDecoder) : 0);
        int y2acDelta = ((headerDecoder.decodeBit() > 0) ? VP8Util.delta(headerDecoder) : 0);
        int chromaDCDelta = ((headerDecoder.decodeBit() > 0) ? VP8Util.delta(headerDecoder) : 0);
        int chromaACDelta = ((headerDecoder.decodeBit() > 0) ? VP8Util.delta(headerDecoder) : 0);
        boolean refreshProbs = headerDecoder.decodeBit() == 0;
        QuantizationParams quants = new QuantizationParams(yacIndex, ydcDelta, y2dcDelta, y2acDelta, chromaDCDelta,
                chromaACDelta);

        int[][][][] coefProbs = getDefaultCoefProbs();
        for (int i = 0; i < VP8Util.BLOCK_TYPES; i++)
            for (int j = 0; j < VP8Util.COEF_BANDS; j++)
                for (int k = 0; k < VP8Util.PREV_COEF_CONTEXTS; k++)
                    for (int l = 0; l < VP8Util.MAX_ENTROPY_TOKENS - 1; l++) {

                        if (headerDecoder.decodeBool(vp8CoefUpdateProbs[i][j][k][l]) > 0) {
                            int newp = headerDecoder.decodeInt(8);
                            coefProbs[i][j][k][l] = newp;
                        }
                    }

        int macroBlockNoCoeffSkip = (int) headerDecoder.decodeBit();
        Assert.assertEquals(1, macroBlockNoCoeffSkip);
        int probSkipFalse = headerDecoder.decodeInt(8);
        for (int mbRow = 0; mbRow < numberOfMBRows; mbRow++) {
            for (int mbCol = 0; mbCol < numberOfMBCols; mbCol++) {
                Macroblock mb = mbs[mbRow + 1][mbCol + 1];
                if ((segmentation > 0))
                    throw new NotSupportedException("TODO: frames with multiple segments are not supported yet");

                if (loopFilterDeltaFlag > 0) {
                    int level = filterLevel;
                    level = level + refLoopFilterDeltas[0];
                    level = (level < 0) ? 0 : (level > 63) ? 63 : level;
                    mb.filterLevel = level;
                } else
                    throw new NotSupportedException("TODO: frames with loopFilterDeltaFlag <= 0 are not supported yet");

                if (macroBlockNoCoeffSkip > 0)
                    mb.skipCoeff = headerDecoder.decodeBool(probSkipFalse);

                mb.lumaMode = headerDecoder.readTree(keyFrameYModeTree, keyFrameYModeProb);
                // 1 is added to account for non-displayed framing macroblocks,
                // which are used for prediction only.
                if (mb.lumaMode == SubblockConstants.B_PRED) {
                    for (int sbRow = 0; sbRow < 4; sbRow++) {
                        for (int sbCol = 0; sbCol < 4; sbCol++) {

                            Subblock sb = mb.ySubblocks[sbRow][sbCol];
                            Subblock A = sb.getAbove(VP8Util.PLANE.Y1, mbs);

                            Subblock L = sb.getLeft(VP8Util.PLANE.Y1, mbs);

                            sb.mode = headerDecoder.readTree(SubblockConstants.subblockModeTree,
                                    SubblockConstants.keyFrameSubblockModeProb[A.mode][L.mode]);

                        }
                    }

                } else {
                    int fixedMode;

                    switch (mb.lumaMode) {
                    case SubblockConstants.DC_PRED:
                        fixedMode = SubblockConstants.B_DC_PRED;
                        break;
                    case SubblockConstants.V_PRED:
                        fixedMode = SubblockConstants.B_VE_PRED;
                        break;
                    case SubblockConstants.H_PRED:
                        fixedMode = SubblockConstants.B_HE_PRED;
                        break;
                    case SubblockConstants.TM_PRED:
                        fixedMode = SubblockConstants.B_TM_PRED;
                        break;
                    default:
                        fixedMode = SubblockConstants.B_DC_PRED;
                        break;
                    }
                    for (int x = 0; x < 4; x++)
                        for (int y = 0; y < 4; y++)
                            mb.ySubblocks[y][x].mode = fixedMode;
                }
                mb.chromaMode = headerDecoder.readTree(VP8Util.vp8UVModeTree, VP8Util.vp8KeyFrameUVModeProb);
            }
        }

        for (int mbRow = 0; mbRow < numberOfMBRows; mbRow++) {
            for (int mbCol = 0; mbCol < numberOfMBCols; mbCol++) {
                Macroblock mb = mbs[mbRow + 1][mbCol + 1];
                mb.decodeMacroBlock(mbs, decoder, coefProbs);
                mb.dequantMacroBlock(mbs, quants);
            }
        }

        if (filterType > 0 && filterLevel != 0) {
            if (filterType == 2) {
                FilterUtil.loopFilterUV(mbs, sharpnessLevel, keyFrame);
                FilterUtil.loopFilterY(mbs, sharpnessLevel, keyFrame);
            } else if (filterType == 1) {
                // loopFilterSimple(frame);
            }
        }

        Picture8Bit p = Picture8Bit.createPicture8Bit(width, height, buffer, ColorSpace.YUV420);

        int mbWidth = getMacroblockCount(width);
        int mbHeight = getMacroblockCount(height);

        for (int mbRow = 0; mbRow < mbHeight; mbRow++) {
            for (int mbCol = 0; mbCol < mbWidth; mbCol++) {
                Macroblock mb = mbs[mbRow + 1][mbCol + 1];
                mb.put(mbRow, mbCol, p);
            }
        }
        return p;
    }

    public static int probe(ByteBuffer data) {
        if ((data.get(3) & 0xff) == 0x9d && (data.get(4) & 0xff) == 0x1 && (data.get(5) & 0xff) == 0x2a)
            return 100;
        return 0;
    }

    public static String printHexByte(byte b) {
        return "0x" + Integer.toHexString(b & 0xFF);
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer frame) {
        NIOUtils.skip(frame, 6);

        int twoBytesWidth = (frame.get() & 0xFF) | (frame.get() & 0xFF) << 8;
        int twoBytesHeight = (frame.get() & 0xFF) | (frame.get() & 0xFF) << 8;
        int width = (twoBytesWidth & 0x3fff);
        int height = (twoBytesHeight & 0x3fff);

        return new VideoCodecMeta(new Size(width, height));
    }
}
