package org.jcodec.codecs.h264;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.VUIParameters;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.ByteBuffer;

public class SPSReadTest {

    private SeqParameterSet sps1;

    @Before
    public void setUp() throws Exception {
        sps1 = new SeqParameterSet();
        sps1.profileIdc = 66;

        sps1.isConstraintSet0Flag = false;
        sps1.isConstraintSet1Flag = false;
        sps1.isConstraintSet2Flag = false;
        sps1.isConstraintSet3Flag = false;
        sps1.isConstraintSet4Flag = false;
        sps1.isConstraintSet5Flag = false;
        sps1.levelIdc = 30;
        sps1.seqParameterSetId = 0;
        sps1.log2MaxFrameNumMinus4 = 5;
        sps1.picOrderCntType = 0;
        sps1.log2MaxPicOrderCntLsbMinus4 = 6;
        sps1.numRefFrames = 1;
        sps1.isGapsInFrameNumValueAllowedFlag = false;
        sps1.picWidthInMbsMinus1 = 31;
        sps1.picHeightInMapUnitsMinus1 = 23;
        sps1.isFrameMbsOnlyFlag = true;
        sps1.isDirect8x8InferenceFlag = true;

        sps1.isFrameCroppingFlag = false;
        sps1.vuiParams = new VUIParameters();
        sps1.vuiParams.aspectRatioInfoPresentFlag = false;
        sps1.vuiParams.overscanInfoPresentFlag = false;
        sps1.vuiParams.videoSignalTypePresentFlag = false;
        sps1.vuiParams.chromaLocInfoPresentFlag = false;
        sps1.vuiParams.timingInfoPresentFlag = true;
        sps1.vuiParams.numUnitsInTick = 1000;
        sps1.vuiParams.timeScale = 50000;
        sps1.vuiParams.fixedFrameRateFlag = true;
        sps1.vuiParams.picStructPresentFlag = false;
        sps1.vuiParams.bitstreamRestriction = new VUIParameters.BitstreamRestriction();

        sps1.vuiParams.bitstreamRestriction.motionVectorsOverPicBoundariesFlag = true;

        sps1.vuiParams.bitstreamRestriction.maxBytesPerPicDenom = 0;

        sps1.vuiParams.bitstreamRestriction.maxBitsPerMbDenom = 0;

        sps1.vuiParams.bitstreamRestriction.log2MaxMvLengthHorizontal = 10;

        sps1.vuiParams.bitstreamRestriction.log2MaxMvLengthVertical = 10;
        sps1.vuiParams.bitstreamRestriction.numReorderFrames = 0;

        sps1.vuiParams.bitstreamRestriction.maxDecFrameBuffering = 1;

    }

    @Test
    public void testRead() throws Exception {
        String path = "src/test/resources/h264/sps/sps1.dat";
        BufferedInputStream is = null;
        try {
            SeqParameterSet sps = SeqParameterSet.read(NIOUtils.fetchFromFile(new File(path)));

            assertEquals(sps.profileIdc, 66);

            assertEquals(sps.isConstraintSet0Flag, false);
            assertEquals(sps.isConstraintSet1Flag, false);
            assertEquals(sps.isConstraintSet2Flag, false);
            assertEquals(sps.isConstraintSet3Flag, false);
            assertEquals(sps.isConstraintSet4Flag, false);
            assertEquals(sps.isConstraintSet5Flag, false);
            assertEquals(sps.levelIdc, 30);
            assertEquals(sps.seqParameterSetId, 0);
            assertEquals(sps.log2MaxFrameNumMinus4, 5);
            assertEquals(sps.picOrderCntType, 0);
            assertEquals(sps.log2MaxPicOrderCntLsbMinus4, 6);
            assertEquals(sps.numRefFrames, 1);
            assertEquals(sps.isGapsInFrameNumValueAllowedFlag, false);
            assertEquals(sps.picWidthInMbsMinus1, 31);
            assertEquals(sps.picHeightInMapUnitsMinus1, 23);
            assertEquals(sps.isFrameMbsOnlyFlag, true);
            assertEquals(sps.isDirect8x8InferenceFlag, true);

            assertEquals(sps.isFrameCroppingFlag, false);
            assertNotNull(sps.vuiParams);
            assertEquals(sps.vuiParams.aspectRatioInfoPresentFlag, false);
            assertEquals(sps.vuiParams.overscanInfoPresentFlag, false);
            assertEquals(sps.vuiParams.videoSignalTypePresentFlag, false);
            assertEquals(sps.vuiParams.chromaLocInfoPresentFlag, false);
            assertEquals(sps.vuiParams.timingInfoPresentFlag, true);
            assertEquals(sps.vuiParams.numUnitsInTick, 1000);
            assertEquals(sps.vuiParams.timeScale, 50000);
            assertEquals(sps.vuiParams.fixedFrameRateFlag, true);
            assertNull(sps.vuiParams.nalHRDParams);
            assertNull(sps.vuiParams.vclHRDParams);
            assertEquals(sps.vuiParams.picStructPresentFlag, false);
            assertNotNull(sps.vuiParams.bitstreamRestriction);
            assertEquals(sps.vuiParams.bitstreamRestriction.motionVectorsOverPicBoundariesFlag, true);
            assertEquals(sps.vuiParams.bitstreamRestriction.maxBytesPerPicDenom, 0);
            assertEquals(sps.vuiParams.bitstreamRestriction.maxBitsPerMbDenom, 0);
            assertEquals(sps.vuiParams.bitstreamRestriction.log2MaxMvLengthHorizontal, 10);
            assertEquals(sps.vuiParams.bitstreamRestriction.log2MaxMvLengthVertical, 10);
            assertEquals(sps.vuiParams.bitstreamRestriction.numReorderFrames, 0);
            assertEquals(sps.vuiParams.bitstreamRestriction.maxDecFrameBuffering, 1);

        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Test
    public void testWrite() throws Exception {

        String path = "src/test/resources/h264/sps/sps1.dat";
        ByteBuffer bb = ByteBuffer.allocate(1024);
        sps1.write(bb);
        bb.flip();

        ByteBuffer expect = NIOUtils.fetchFromFile(new File(path));

        Assert.assertArrayEquals(NIOUtils.toArray(bb), NIOUtils.toArray(expect));

    }
}