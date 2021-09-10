package org.jcodec.codecs.vpx.vp8;

import java.util.ArrayList;
import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.OnyxIf.TimeStampRange;
import org.jcodec.codecs.vpx.vp8.data.CodecAlgPRiv;
import org.jcodec.codecs.vpx.vp8.data.CodecEncCfg;
import org.jcodec.codecs.vpx.vp8.data.OnyxInt;
import org.jcodec.codecs.vpx.vp8.data.Compressor;
import org.jcodec.codecs.vpx.vp8.data.CodecPkt;
import org.jcodec.codecs.vpx.vp8.data.YV12buffer;
import org.jcodec.codecs.vpx.vp8.enums.AlgoFlags;
import org.jcodec.codecs.vpx.vp8.enums.CompressMode;
import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;
import org.jcodec.codecs.vpx.vp8.enums.GeneralFrameFlags;
import org.jcodec.codecs.vpx.vp8.enums.InitFlags;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;
import org.jcodec.codecs.vpx.vp8.enums.PacketKind;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;
import org.jcodec.common.model.Picture;

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
public class CXInterface {

    /* !\brief deadline parameter analogous to VPx REALTIME mode. */
    public static int VPX_DL_REALTIME = 1;
    /* !\brief deadline parameter analogous to VPx GOOD QUALITY mode. */
    public static int VPX_DL_GOOD_QUALITY = 1000000;
    /* !\brief deadline parameter analogous to VPx BEST QUALITY mode. */
    public static int VPX_DL_BEST_QUALITY = 0;

    static void validate_img(CodecAlgPRiv ctx, Picture img) {
        if ((img.getWidth() != ctx.cfg.getG_w()) || (img.getHeight() != ctx.cfg.getG_h()))
            VP8Exception.ERROR("Image size must match encoder init configuration size");
    }

    static void pick_quickcompress_mode(CodecAlgPRiv ctx, long duration, long deadline) {
        CompressMode new_qc;

        /* Use best quality mode if no deadline is given. */
        new_qc = CompressMode.BESTQUALITY;

        if (deadline != 0) {
            /* Convert duration parameter from stream timebase to microseconds */
            long duration_us;

            duration_us = duration * ctx.timestamp_ratio.getNum()
                    / (ctx.timestamp_ratio.getDen() * (OnyxInt.TICKS_PER_SEC / 1000000));

            /*
             * If the deadline is more that the duration this frame is to be shown, use good
             * quality mode. Otherwise use realtime mode.
             */
            new_qc = (deadline > duration_us) ? CompressMode.GOODQUALITY : CompressMode.REALTIME;
        }

        if (deadline == VPX_DL_REALTIME) {
            new_qc = CompressMode.REALTIME;
        }

        if (ctx.oxcf.Mode != new_qc) {
            ctx.oxcf.Mode = new_qc;
            ctx.cpi.vp8_change_config(ctx.oxcf);
        }
    }

    static void set_reference_and_update(CodecAlgPRiv ctx, EnumSet<AlgoFlags> flags) {
        /* Handle Flags */
        if ((flags.contains(AlgoFlags.NO_UPD_GF) && flags.contains(AlgoFlags.FORCE_GF))
                || (flags.contains(AlgoFlags.NO_UPD_ARF) && flags.contains(AlgoFlags.FORCE_ARF))) {
            VP8Exception.ERROR("Conflicting flags.");
        }

        boolean update = false;
        EnumSet<MVReferenceFrame> referenceFrames = EnumSet.allOf(MVReferenceFrame.class);

        if (flags.contains(AlgoFlags.NO_REF_LAST)) {
            update = true;
            referenceFrames.remove(MVReferenceFrame.LAST_FRAME);
        }
        if (flags.contains(AlgoFlags.NO_REF_GF)) {
            update = true;
            referenceFrames.remove(MVReferenceFrame.GOLDEN_FRAME);
        }
        if (flags.contains(AlgoFlags.NO_REF_ARF)) {
            update = true;
            referenceFrames.remove(MVReferenceFrame.ALTREF_FRAME);
        }

        if (update) {
            OnyxIf.vp8_use_as_reference(ctx.cpi, referenceFrames);
        }

        update = false;
        referenceFrames = EnumSet.allOf(MVReferenceFrame.class);
        if (flags.contains(AlgoFlags.NO_UPD_LAST)) {
            update = true;
            referenceFrames.remove(MVReferenceFrame.LAST_FRAME);
        }

        if (flags.contains(AlgoFlags.NO_UPD_GF)) {
            update = true;
            referenceFrames.remove(MVReferenceFrame.GOLDEN_FRAME);
        }

        if (flags.contains(AlgoFlags.NO_UPD_ARF)) {
            update = true;
            referenceFrames.remove(MVReferenceFrame.ALTREF_FRAME);
        }

        if (update) {
            OnyxIf.vp8_update_reference(ctx.cpi, referenceFrames);
        }

        if (flags.contains(AlgoFlags.NO_UPD_ENTROPY)) {
            OnyxIf.vp8_update_entropy(ctx.cpi, false);
        }
    }

    public static void vp8e_encode(CodecAlgPRiv ctx, Picture img, long pts_val, long duration, EnumSet<AlgoFlags> flags,
            long deadline) {

        if (ctx.cfg.getRc_target_bitrate() == 0) {
            return;
        }

        if (img != null)
            validate_img(ctx, img);

        if (!ctx.pts_offset_initialized) {
            ctx.pts_offset = (int) pts_val;
            ctx.pts_offset_initialized = true;
        }
        pts_val -= ctx.pts_offset;

        pick_quickcompress_mode(ctx, duration, deadline);
        ctx.pkt_list = new ArrayList<CodecPkt>();

// If no flags are set in the encode call, then use the frame flags as
// defined via the control function: vp8e_set_frame_flags.
        if (flags.isEmpty()) {
            flags = EnumSet.copyOf(ctx.control_frame_flags);
        }
        ctx.control_frame_flags.clear();

        set_reference_and_update(ctx, flags);

        /* Handle fixed keyframe intervals */
        if (ctx.cfg.getKf_mode() == CodecEncCfg.vpx_kf_mode.VPX_KF_AUTO
                && ctx.cfg.getKf_min_dist() == ctx.cfg.getKf_max_dist()) {
            if (++ctx.fixed_kf_cntr > ctx.cfg.getKf_min_dist()) {
                flags.add(AlgoFlags.FORCE_KF);
                ctx.fixed_kf_cntr = 1;
            }
        }

        /* Initialize the encoder instance on the first frame */
        if (ctx.cpi != null) {
            EnumSet<FrameTypeFlags> lib_flags;
            TimeStampRange dst_ts = new TimeStampRange();
            int cx_data_sz;
            FullAccessIntArrPointer cx_data;
            FullAccessIntArrPointer cx_data_end;
            int size = 0;

            /* Set up internal flags */
            ctx.cpi.b_calculate_psnr = ctx.base.init_flags.contains(InitFlags.USE_PSNR);
            ctx.cpi.output_partition = ctx.base.init_flags.contains(InitFlags.USE_OUTPUT_PARTITION);

            /* Convert API flags to internal codec lib flags */
            lib_flags = flags.contains(AlgoFlags.FORCE_KF) ? EnumSet.of(FrameTypeFlags.Key)
                    : EnumSet.noneOf(FrameTypeFlags.class);
            double tsratio = ctx.timestamp_ratio.scalar();
            dst_ts.time_stamp = (long) (pts_val * tsratio);
            dst_ts.time_end = (long) ((pts_val + (long) duration) * tsratio);

            if (img != null) {
                YV12buffer sd = new YV12buffer(img);
                EnumSet<FrameTypeFlags> passedFlags = EnumSet.copyOf(ctx.next_frame_flag);
                passedFlags.addAll(lib_flags);
                OnyxIf.vp8_receive_raw_frame(ctx.cpi, passedFlags, sd, dst_ts.time_stamp, dst_ts.time_end);

                /* reset for next frame */
                ctx.next_frame_flag.clear();
            }

            cx_data = ctx.cx_data.shallowCopy();
            cx_data_sz = (ctx.cx_data.size() - 1);
            cx_data_end = ctx.cx_data.shallowCopyWithPosInc(cx_data_sz);
            lib_flags.clear();

            while (cx_data_sz >= ctx.cx_data.size() / 2) {
                size = OnyxIf.vp8_get_compressed_data(ctx.cpi, lib_flags, cx_data, cx_data_end, dst_ts, img == null);

                if (size == -1) {
                    break;
                }

                if (size != 0) {
                    long round, delta;
                    CodecPkt pkt = new CodecPkt();
                    Compressor cpi = ctx.cpi;

                    /* Add the frame packet to the list of returned packets. */
                    round = ctx.timestamp_ratio.getNum() / 2;
                    if (round > 0)
                        --round;
                    delta = (dst_ts.time_end - dst_ts.time_stamp);
                    pkt.kind = PacketKind.FRAME_PKT;
                    CodecPkt.FramePacket fp = new CodecPkt.FramePacket();
                    pkt.packet = fp;
                    fp.pts = (dst_ts.time_stamp * ctx.timestamp_ratio.getDen() + round) / ctx.timestamp_ratio.getNum()
                            + ctx.pts_offset;
                    fp.duration = (long) ((delta * ctx.timestamp_ratio.getDen() + round)
                            / ctx.timestamp_ratio.getNum());
                    fp.vp8flags = EnumSet.copyOf(lib_flags);
                    fp.flags = EnumSet.noneOf(GeneralFrameFlags.class);
                    fp.width = cpi.common.Width;
                    fp.height = cpi.common.Height;

                    if (lib_flags.contains(FrameTypeFlags.Key)) {
                        fp.flags.add(GeneralFrameFlags.FRAME_IS_KEY);
                    }

                    if (!cpi.common.show_frame) {
                        fp.flags.add(GeneralFrameFlags.FRAME_IS_INVISIBLE);

                        /*
                         * This timestamp should be as close as possible to the prior PTS so that if a
                         * decoder uses pts to schedule when to do this, we start right after last frame
                         * was decoded. Invisible frames have no duration.
                         */
                        fp.pts = ((cpi.last_time_stamp_seen * ctx.timestamp_ratio.getDen() + round)
                                / ctx.timestamp_ratio.getNum()) + ctx.pts_offset + 1;
                        fp.duration = 0;
                    }

                    if (cpi.droppable)
                        fp.flags.add(GeneralFrameFlags.FRAME_IS_DROPPABLE);

                    if (cpi.output_partition) {
                        int i;
                        final int num_partitions = (1 << cpi.common.multi_token_partition.ordinal()) + 1;

                        fp.flags.add(GeneralFrameFlags.FRAME_IS_FRAGMENT);

                        for (i = 0; i < num_partitions; ++i) {
                            fp.buf = cx_data.shallowCopy();
                            cx_data.incBy(cpi.partition_sz[i]);
                            cx_data_sz -= cpi.partition_sz[i];
                            fp.sz = cpi.partition_sz[i];
                            fp.partition_id = i;
                            /* don't set the fragment bit for the last partition */
                            if (i == (num_partitions - 1)) {
                                fp.flags.remove(GeneralFrameFlags.FRAME_IS_FRAGMENT);
                            }
                            ctx.pkt_list.add(pkt);
                        }
                    } else {
                        fp.buf = cx_data.shallowCopy();
                        fp.sz = size;
                        fp.partition_id = -1;
                        ctx.pkt_list.add(pkt);
                        cx_data.incBy(size);
                        cx_data_sz -= size;
                    }
                }
            }
            if (ctx.cpi.oxcf.hinter != null) {
                ctx.cpi.oxcf.hinter.setFrameIsRepeat(false);
            }
        }
    }

}
