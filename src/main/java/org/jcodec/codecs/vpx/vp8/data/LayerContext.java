package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumMap;

import org.jcodec.codecs.vpx.vp8.OnyxIf;
import org.jcodec.codecs.vpx.vp8.enums.MVReferenceFrame;

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
public class LayerContext {
    /* Layer configuration */
    public double framerate;
    public int target_bandwidth;

    /* Layer specific coding parameters */
    public long starting_buffer_level;
    public long optimal_buffer_level;
    public long maximum_buffer_size;
    public long starting_buffer_level_in_ms;
    public long optimal_buffer_level_in_ms;
    public long maximum_buffer_size_in_ms;

    public int avg_frame_size_for_layer;

    public long buffer_level;
    public long bits_off_target;

    public long total_actual_bits;
    public int total_target_vs_actual;

    public int worst_quality;
    public short active_worst_quality;
    public int best_quality;
    public short active_best_quality;

    public short ni_av_qi;
    public int ni_tot_qi;
    public int ni_frames;
    public short avg_frame_qindex;

    public double rate_correction_factor;
    public double key_frame_rate_correction_factor;
    public double gf_rate_correction_factor;

    public int zbin_over_quant;

    public int inter_frame_target;
    public long total_byte_count;

    public short filter_level;

    public int frames_since_last_drop_overshoot;

    public int force_maxqp;

    public int last_frame_percent_intra;

    public EnumMap<MVReferenceFrame, Integer> count_mb_ref_frame_usage = new EnumMap<MVReferenceFrame, Integer>(
            MVReferenceFrame.class);

    public short[] last_q = new short[2];

    public LayerContext(Compressor cpi, Config oxcf, int layer, double prev_layer_framerate) {
        framerate = cpi.output_framerate / cpi.oxcf.rate_decimator[layer];
        target_bandwidth = cpi.oxcf.target_bitrate[layer] * 1000;

        starting_buffer_level_in_ms = oxcf.starting_buffer_level;
        optimal_buffer_level_in_ms = oxcf.optimal_buffer_level;
        maximum_buffer_size_in_ms = oxcf.maximum_buffer_size;

        starting_buffer_level = OnyxIf.rescale((int) (oxcf.starting_buffer_level), target_bandwidth, 1000);

        if (oxcf.optimal_buffer_level == 0) {
            optimal_buffer_level = target_bandwidth / 8;
        } else {
            optimal_buffer_level = OnyxIf.rescale((int) (oxcf.optimal_buffer_level), target_bandwidth, 1000);
        }

        if (oxcf.maximum_buffer_size == 0) {
            maximum_buffer_size = target_bandwidth / 8;
        } else {
            maximum_buffer_size = OnyxIf.rescale((int) (oxcf.maximum_buffer_size), target_bandwidth, 1000);
        }

        /* Work out the average size of a frame within this layer */
        if (layer > 0) {
            avg_frame_size_for_layer = (int) ((cpi.oxcf.target_bitrate[layer] - cpi.oxcf.target_bitrate[layer - 1])
                    * 1000 / (framerate - prev_layer_framerate));
        }

        active_worst_quality = cpi.oxcf.worst_allowed_q;
        active_best_quality = cpi.oxcf.best_allowed_q;
        avg_frame_qindex = cpi.oxcf.worst_allowed_q;

        buffer_level = starting_buffer_level;
        bits_off_target = starting_buffer_level;

        total_actual_bits = 0;
        ni_av_qi = 0;
        ni_tot_qi = 0;
        ni_frames = 0;
        rate_correction_factor = 1.0;
        key_frame_rate_correction_factor = 1.0;
        gf_rate_correction_factor = 1.0;
        inter_frame_target = 0;

    }

}
