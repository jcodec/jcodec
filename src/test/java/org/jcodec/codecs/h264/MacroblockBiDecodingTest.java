package org.jcodec.codecs.h264;

import static java.lang.String.format;
import static org.jcodec.common.ArrayUtil.toByteArrayShifted;
import static org.jcodec.common.JCodecUtil.getAsIntArray;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MacroblockBiDecodingTest {
    @Test
    public void testB16x16CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_b_16x16.264";
        String decoded = "src/test/resources/h264/cabac/test_b_16x16.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB16x8CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_b_16x8.264";
        String decoded = "src/test/resources/h264/cabac/test_b_16x8.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB4x4CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_b_4x4.264";
        String decoded = "src/test/resources/h264/cabac/test_b_4x4.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB4x8CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_b_4x8.264";
        String decoded = "src/test/resources/h264/cabac/test_b_4x8.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB8x16CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_b_8x16.264";
        String decoded = "src/test/resources/h264/cabac/test_b_8x16.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB8x4CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_b_8x4.264";
        String decoded = "src/test/resources/h264/cabac/test_b_8x4.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB8x8CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_b_8x8.264";
        String decoded = "src/test/resources/h264/cabac/test_b_8x8.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }
    
    @Test
    public void testBSkipCABAC() throws IOException {
        // BAD
        String encoded = "src/test/resources/h264/cabac/test_b_skip.264";
        String decoded = "src/test/resources/h264/cabac/test_b_skip.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }
    
    @Test
    public void testBi16x16CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_bi_16x16.264";
        String decoded = "src/test/resources/h264/cabac/test_bi_16x16.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBi16x8CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_bi_16x8.264";
        String decoded = "src/test/resources/h264/cabac/test_bi_16x8.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBi8x16CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_bi_8x16.264";
        String decoded = "src/test/resources/h264/cabac/test_bi_8x16.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBi8x8CABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_bi_8x8.264";
        String decoded = "src/test/resources/h264/cabac/test_bi_8x8.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testMixedBiRef() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_bi_mix_ref.264";
        String decoded = "src/test/resources/h264/cabac/test_bi_mix_ref.yuv";
        int nFrames = 5;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 4, 1, 2, 3});
    }

    @Test
    public void testLongTermCABAC() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_long_term_cabac.264";
        String decoded = "src/test/resources/h264/cabac/test_long_term_cabac.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 1, 2});
    }

    @Test
    public void testBiDirectSpatialNoInference() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_spat_direct_noinf.264";
        String decoded = "src/test/resources/h264/cabac/test_spat_direct_noinf.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testSpatialDirect() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_spat_direct.264";
        String decoded = "src/test/resources/h264/cabac/test_spat_direct.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBiDirectTemporalNoInference() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_temp_direct_noinf.264";
        String decoded = "src/test/resources/h264/cabac/test_temp_direct_noinf.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBiDirectTemporal() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_temp_direct.264";
        String decoded = "src/test/resources/h264/cabac/test_temp_direct.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }
    
    @Test
    public void testBiDirectTemporalHierarchical() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_temp_direct_hierarchical.264";
        String decoded = "src/test/resources/h264/cabac/test_temp_direct_hierarchical.yuv";
        int nFrames = 7;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 4, 2, 1, 3, 6, 5});
    }
    
    @Test
    public void testBiMixHierarchical() throws IOException {
        String encoded = "src/test/resources/h264/cabac/test_bi_mix_hierarchical.264";
        String decoded = "src/test/resources/h264/cabac/test_bi_mix_hierarchical.yuv";
        int nFrames = 7;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 4, 2, 1, 3, 6, 5});
    }

    @Test
    public void testB16x16CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_b_16x16_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_b_16x16_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB16x8CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_b_16x8_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_b_16x8_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB4x4CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_b_4x4_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_b_4x4_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB4x8CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_b_4x8_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_b_4x8_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB8x16CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_b_8x16_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_b_8x16_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB8x4CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_b_8x4_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_b_8x4_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testB8x8CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_b_8x8_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_b_8x8_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBi16x16CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_16x16_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_16x16_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBi16x8CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_16x8_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_16x8_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBi8x16CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_8x16_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_8x16_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBi8x8CAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_8x8_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_8x8_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testMixedBiRefCAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_mix_ref_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_mix_ref_cavlc.yuv";
        int nFrames = 5;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 4, 1, 2, 3});
    }

    @Test
    public void testBiDirectSpatialCAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_spat_direct_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_spat_direct_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBiDirectSpatialNoInferenceCAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_spat_direct_noinf_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_spat_direct_noinf_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBiDirectTemporalCAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_temp_direct_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_temp_direct_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testBiDirectTemporalNoInferenceCAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_bi_temp_direct_noinf_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_bi_temp_direct_noinf_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 2, 1});
    }

    @Test
    public void testLongTermCAVLC() throws IOException {
        String encoded = "src/test/resources/h264/cavlc/test_long_term_cavlc.264";
        String decoded = "src/test/resources/h264/cavlc/test_long_term_cavlc.yuv";
        int nFrames = 3;

        testOneFile(encoded, decoded, nFrames, new int[] {0, 1, 2});
    }
    
    private void testOneFile(String encoded, String decoded, int nFrames, int[] reorderMap) throws IOException {
        MappedH264ES es = new MappedH264ES(NIOUtils.fetchFrom(new File(encoded)));
        H264Decoder dec = new H264Decoder();

        Frame[] out = new Frame[nFrames];
        for (int i = 0; i < nFrames; i++) {
            out[i] = dec.decodeFrame8Bit(es.nextFrame().getData(), Picture8Bit.create(32, 32, ColorSpace.YUV420)
                    .getData());
        }

        ByteBuffer yuv = NIOUtils.fetchFrom(new File(decoded));
        byte[][][] expected = new byte[nFrames][3][];
        for (int i = 0; i < nFrames; i++) {
            expected[i][0] = toByteArrayShifted(getAsIntArray(yuv, 1024));
            expected[i][1] = toByteArrayShifted(getAsIntArray(yuv, 256));
            expected[i][2] = toByteArrayShifted(getAsIntArray(yuv, 256));
        }
        
        for (int i = 0; i < nFrames; i++) {
            Assert.assertArrayEquals(format("Frame %d luma", i), expected[reorderMap[i]][0],
                    out[i].getPlaneData(0));
            Assert.assertArrayEquals(format("Frame %d cb", i), expected[reorderMap[i]][1],
                    out[i].getPlaneData(1));
            Assert.assertArrayEquals(format("Frame %d cr", i), expected[reorderMap[i]][2],
                    out[i].getPlaneData(2));
        }
    }
}