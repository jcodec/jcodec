package org.jcodec.codecs.h264;
import static org.junit.Assert.assertEquals;

import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.common.io.NIOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;

public class PPSReadTest {
    private PictureParameterSet expected;

    @Before
    public void setUp() throws Exception {
        expected = new PictureParameterSet();
        expected.picParameterSetId = 0;
        expected.seqParameterSetId = 0;
        expected.entropyCodingModeFlag = true;
        expected.picOrderPresentFlag = false;
        expected.numSliceGroupsMinus1 = 0;
        expected.numRefIdxActiveMinus1 = new int[] { 0, 0 };
        expected.weightedPredFlag = false;
        expected.weightedBipredIdc = 0;
        expected.picInitQpMinus26 = 0;
        expected.picInitQsMinus26 = 0;
        expected.chromaQpIndexOffset = -2;
        expected.deblockingFilterControlPresentFlag = true;
        expected.constrainedIntraPredFlag = false;
        expected.redundantPicCntPresentFlag = false;
    }

    @Test
    public void testRead() throws Exception {

        ByteBuffer bb = NIOUtils.fetchFromFile(new File("src/test/resources/h264/pps/pps.dat"));
        PictureParameterSet pps = PictureParameterSet.read(bb);
        assertEquals(expected, pps);
    }

}
