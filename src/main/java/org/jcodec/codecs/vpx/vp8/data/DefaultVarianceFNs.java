package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.vp8.CommonUtils;
import org.jcodec.codecs.vpx.vp8.Sad;
import org.jcodec.codecs.vpx.vp8.SubpixelVariance;
import org.jcodec.codecs.vpx.vp8.Variance;
import org.jcodec.codecs.vpx.vp8.enums.BlockEnum;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class DefaultVarianceFNs {
    public final EnumMap<BlockEnum, VarianceFNs> default_fn_ptr = new EnumMap<BlockEnum, VarianceFNs>(BlockEnum.class);

    public DefaultVarianceFNs() {
        VarianceFNs tfn = new VarianceFNs();
        tfn.sdf = Sad.vpx_sad16x16;
        tfn.vf = Variance.vpx_variance16x16;
        tfn.svf = new SubpixelVariance(16, 16);
        tfn.sdx3f = Sad.vpx_sad16x16x3;
        tfn.sdx8f = Sad.vpx_sad16x16x8;
        tfn.sdx4df = Sad.vpx_sad16x16x4d;
        tfn.copymem = CommonUtils.vp8_copy32xn;
        default_fn_ptr.put(BlockEnum.BLOCK_16X16, tfn);

        tfn = new VarianceFNs();
        tfn.sdf = Sad.vpx_sad16x8;
        tfn.vf = Variance.vpx_variance16x8;
        tfn.svf = new SubpixelVariance(16, 8);
        tfn.sdx3f = Sad.vpx_sad16x8x3;
        tfn.sdx8f = Sad.vpx_sad16x8x8;
        tfn.sdx4df = Sad.vpx_sad16x8x4d;
        tfn.copymem = CommonUtils.vp8_copy32xn;
        default_fn_ptr.put(BlockEnum.BLOCK_16X8, tfn);

        tfn = new VarianceFNs();
        tfn.sdf = Sad.vpx_sad8x16;
        tfn.vf = Variance.vpx_variance8x16;
        tfn.svf = new SubpixelVariance(8, 16);
        tfn.sdx3f = Sad.vpx_sad8x16x3;
        tfn.sdx8f = Sad.vpx_sad8x16x8;
        tfn.sdx4df = Sad.vpx_sad8x16x4d;
        tfn.copymem = CommonUtils.vp8_copy32xn;
        default_fn_ptr.put(BlockEnum.BLOCK_8X16, tfn);

        tfn = new VarianceFNs();
        tfn.sdf = Sad.vpx_sad8x8;
        tfn.vf = Variance.vpx_variance8x8;
        tfn.svf = new SubpixelVariance(8, 8);
        tfn.sdx3f = Sad.vpx_sad8x8x3;
        tfn.sdx8f = Sad.vpx_sad8x8x8;
        tfn.sdx4df = Sad.vpx_sad8x8x4d;
        tfn.copymem = CommonUtils.vp8_copy32xn;
        default_fn_ptr.put(BlockEnum.BLOCK_8X8, tfn);

        tfn = new VarianceFNs();
        tfn.sdf = Sad.vpx_sad4x4;
        tfn.vf = Variance.vpx_variance4x4;
        tfn.svf = new SubpixelVariance(4, 4);
        tfn.sdx3f = Sad.vpx_sad4x4x3;
        tfn.sdx8f = Sad.vpx_sad4x4x8;
        tfn.sdx4df = Sad.vpx_sad4x4x4d;
        tfn.copymem = CommonUtils.vp8_copy32xn;
        default_fn_ptr.put(BlockEnum.BLOCK_4X4, tfn);
    }
}
