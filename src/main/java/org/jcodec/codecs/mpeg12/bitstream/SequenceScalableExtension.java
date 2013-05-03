package org.jcodec.codecs.mpeg12.bitstream;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SequenceScalableExtension {

    public static final int DATA_PARTITIONING = 0;
    public static final int SPATIAL_SCALABILITY = 1;
    public static final int SNR_SCALABILITY = 2;
    public static final int TEMPORAL_SCALABILITY = 3;

    public int scalable_mode;
    public int layer_id;
    public int lower_layer_prediction_horizontal_size;
    public int lower_layer_prediction_vertical_size;
    public int horizontal_subsampling_factor_m;
    public int horizontal_subsampling_factor_n;
    public int vertical_subsampling_factor_m;
    public int vertical_subsampling_factor_n;
    public int picture_mux_enable;
    public int mux_to_progressive_sequence;
    public int picture_mux_order;
    public int picture_mux_factor;

    public static SequenceScalableExtension read(BitReader in) {
        SequenceScalableExtension sse = new SequenceScalableExtension();
        sse.scalable_mode = in.readNBit(2);
        sse.layer_id = in.readNBit(4);

        if (sse.scalable_mode == SPATIAL_SCALABILITY) {
            sse.lower_layer_prediction_horizontal_size = in.readNBit(14);
            in.read1Bit();
            sse.lower_layer_prediction_vertical_size = in.readNBit(14);
            sse.horizontal_subsampling_factor_m = in.readNBit(5);
            sse.horizontal_subsampling_factor_n = in.readNBit(5);
            sse.vertical_subsampling_factor_m = in.readNBit(5);
            sse.vertical_subsampling_factor_n = in.readNBit(5);
        }

        if (sse.scalable_mode == TEMPORAL_SCALABILITY) {
            sse.picture_mux_enable = in.read1Bit();
            if (sse.picture_mux_enable != 0)
                sse.mux_to_progressive_sequence = in.read1Bit();
            sse.picture_mux_order = in.readNBit(3);
            sse.picture_mux_factor = in.readNBit(3);
        }

        return sse;
    }

    public void write(BitWriter out) {
        out.writeNBit(scalable_mode, 2);
        out.writeNBit(layer_id, 4);

        if (scalable_mode == SPATIAL_SCALABILITY) {
            out.writeNBit(lower_layer_prediction_horizontal_size, 14);
            out.write1Bit(1); // todo: check this
            out.writeNBit(lower_layer_prediction_vertical_size, 14);
            out.writeNBit(horizontal_subsampling_factor_m, 5);
            out.writeNBit(horizontal_subsampling_factor_n, 5);
            out.writeNBit(vertical_subsampling_factor_m, 5);
            out.writeNBit(vertical_subsampling_factor_n, 5);
        }

        if (scalable_mode == TEMPORAL_SCALABILITY) {
            out.write1Bit(picture_mux_enable);
            if (picture_mux_enable != 0)
                out.write1Bit(mux_to_progressive_sequence);
            out.writeNBit(picture_mux_order, 3);
            out.writeNBit(picture_mux_factor, 3);
        }
    }
}
