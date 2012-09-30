package org.jcodec.codecs.h264;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.ReflectionComparatorMode;

public class TestPPS extends TestCase {
	private PictureParameterSet expected;

	@Override
	protected void setUp() throws Exception {
		expected = new PictureParameterSet();
		expected.pic_parameter_set_id = 0;
		expected.seq_parameter_set_id = 0;
		expected.entropy_coding_mode_flag = true;
		expected.pic_order_present_flag = false;
		expected.num_slice_groups_minus1 = 0;
		expected.num_ref_idx_l0_active_minus1 = 0;
		expected.num_ref_idx_l1_active_minus1 = 0;
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

		String path = "src/test/resources/h264/pps/pps.dat";
		BufferedInputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(path));
			PictureParameterSet pps = PictureParameterSet.read(is);
			assertEquals(expected, pps);

		} finally {
			IOUtils.closeQuietly(is);
		}
	}

}
