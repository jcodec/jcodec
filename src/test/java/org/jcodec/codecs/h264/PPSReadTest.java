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
        expected.pic_parameter_set_id = 0;
        expected.seq_parameter_set_id = 0;
        expected.entropy_coding_mode_flag = true;
        expected.pic_order_present_flag = false;
        expected.num_slice_groups_minus1 = 0;
        expected.num_ref_idx_active_minus1 = new int[] { 0, 0 };
        expected.weighted_pred_flag = false;
        expected.weighted_bipred_idc = 0;
        expected.pic_init_qp_minus26 = 0;
        expected.pic_init_qs_minus26 = 0;
        expected.chroma_qp_index_offset = -2;
        expected.deblocking_filter_control_present_flag = true;
        expected.constrained_intra_pred_flag = false;
        expected.redundant_pic_cnt_present_flag = false;
    }

    @Test
    public void testRead() throws Exception {

        ByteBuffer bb = NIOUtils.fetchFromFile(new File("src/test/resources/h264/pps/pps.dat"));
        PictureParameterSet pps = PictureParameterSet.read(bb);
        assertEquals(expected, pps);
    }

}
