package org.jcodec.codecs.h264;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.VUIParameters;
import org.jcodec.common.IOUtils;
import org.jcodec.common.NIOUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestSPS extends TestCase {

    private SeqParameterSet sps1;

    @Override
    protected void setUp() throws Exception {
        sps1 = new SeqParameterSet();
        sps1.profile_idc = 66;

        sps1.constraint_set_0_flag = false;
        sps1.constraint_set_1_flag = false;
        sps1.constraint_set_2_flag = false;
        sps1.constraint_set_3_flag = false;
        sps1.level_idc = 30;
        sps1.seq_parameter_set_id = 0;
        sps1.log2_max_frame_num_minus4 = 5;
        sps1.pic_order_cnt_type = 0;
        sps1.log2_max_pic_order_cnt_lsb_minus4 = 6;
        sps1.num_ref_frames = 1;
        sps1.gaps_in_frame_num_value_allowed_flag = false;
        sps1.pic_width_in_mbs_minus1 = 31;
        sps1.pic_height_in_map_units_minus1 = 23;
        sps1.frame_mbs_only_flag = true;
        sps1.direct_8x8_inference_flag = true;

        sps1.frame_cropping_flag = false;
        sps1.vuiParams = new VUIParameters();
        sps1.vuiParams.aspect_ratio_info_present_flag = false;
        sps1.vuiParams.overscan_info_present_flag = false;
        sps1.vuiParams.video_signal_type_present_flag = false;
        sps1.vuiParams.chroma_loc_info_present_flag = false;
        sps1.vuiParams.timing_info_present_flag = true;
        sps1.vuiParams.num_units_in_tick = 1000;
        sps1.vuiParams.time_scale = 50000;
        sps1.vuiParams.fixed_frame_rate_flag = true;
        sps1.vuiParams.pic_struct_present_flag = false;
        sps1.vuiParams.bitstreamRestriction = new VUIParameters.BitstreamRestriction();

        sps1.vuiParams.bitstreamRestriction.motion_vectors_over_pic_boundaries_flag = true;

        sps1.vuiParams.bitstreamRestriction.max_bytes_per_pic_denom = 0;

        sps1.vuiParams.bitstreamRestriction.max_bits_per_mb_denom = 0;

        sps1.vuiParams.bitstreamRestriction.log2_max_mv_length_horizontal = 10;

        sps1.vuiParams.bitstreamRestriction.log2_max_mv_length_vertical = 10;
        sps1.vuiParams.bitstreamRestriction.num_reorder_frames = 0;

        sps1.vuiParams.bitstreamRestriction.max_dec_frame_buffering = 1;

    }

    @Test
    public void testRead() throws Exception {
        String path = "src/test/resources/h264/sps/sps1.dat";
        BufferedInputStream is = null;
        try {
            SeqParameterSet sps = SeqParameterSet.read(NIOUtils.fetchFrom(new File(path)));

            assertEquals(sps.profile_idc, 66);

            assertEquals(sps.constraint_set_0_flag, false);
            assertEquals(sps.constraint_set_1_flag, false);
            assertEquals(sps.constraint_set_2_flag, false);
            assertEquals(sps.constraint_set_3_flag, false);
            assertEquals(sps.level_idc, 30);
            assertEquals(sps.seq_parameter_set_id, 0);
            assertEquals(sps.log2_max_frame_num_minus4, 5);
            assertEquals(sps.pic_order_cnt_type, 0);
            assertEquals(sps.log2_max_pic_order_cnt_lsb_minus4, 6);
            assertEquals(sps.num_ref_frames, 1);
            assertEquals(sps.gaps_in_frame_num_value_allowed_flag, false);
            assertEquals(sps.pic_width_in_mbs_minus1, 31);
            assertEquals(sps.pic_height_in_map_units_minus1, 23);
            assertEquals(sps.frame_mbs_only_flag, true);
            assertEquals(sps.direct_8x8_inference_flag, true);

            assertEquals(sps.frame_cropping_flag, false);
            assertNotNull(sps.vuiParams);
            assertEquals(sps.vuiParams.aspect_ratio_info_present_flag, false);
            assertEquals(sps.vuiParams.overscan_info_present_flag, false);
            assertEquals(sps.vuiParams.video_signal_type_present_flag, false);
            assertEquals(sps.vuiParams.chroma_loc_info_present_flag, false);
            assertEquals(sps.vuiParams.timing_info_present_flag, true);
            assertEquals(sps.vuiParams.num_units_in_tick, 1000);
            assertEquals(sps.vuiParams.time_scale, 50000);
            assertEquals(sps.vuiParams.fixed_frame_rate_flag, true);
            assertNull(sps.vuiParams.nalHRDParams);
            assertNull(sps.vuiParams.vclHRDParams);
            assertEquals(sps.vuiParams.pic_struct_present_flag, false);
            assertNotNull(sps.vuiParams.bitstreamRestriction);
            assertEquals(sps.vuiParams.bitstreamRestriction.motion_vectors_over_pic_boundaries_flag, true);
            assertEquals(sps.vuiParams.bitstreamRestriction.max_bytes_per_pic_denom, 0);
            assertEquals(sps.vuiParams.bitstreamRestriction.max_bits_per_mb_denom, 0);
            assertEquals(sps.vuiParams.bitstreamRestriction.log2_max_mv_length_horizontal, 10);
            assertEquals(sps.vuiParams.bitstreamRestriction.log2_max_mv_length_vertical, 10);
            assertEquals(sps.vuiParams.bitstreamRestriction.num_reorder_frames, 0);
            assertEquals(sps.vuiParams.bitstreamRestriction.max_dec_frame_buffering, 1);

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

        ByteBuffer expect = NIOUtils.fetchFrom(new File(path));

        Assert.assertArrayEquals(NIOUtils.toArray(bb), NIOUtils.toArray(expect));

    }
}