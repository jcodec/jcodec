package org.jcodec.codecs.mpeg12.bitstream;

import java.io.IOException;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PictureSpatialScalableExtension {
    public int lower_layer_temporal_reference;
    public int lower_layer_horizontal_offset;
    public int lower_layer_vertical_offset;
    public int spatial_temporal_weight_code_table_index;
    public int lower_layer_progressive_frame;
    public int lower_layer_deinterlaced_field_select;

    public static PictureSpatialScalableExtension read(BitReader in) {
        PictureSpatialScalableExtension psse = new PictureSpatialScalableExtension();

        psse.lower_layer_temporal_reference = in.readNBit(10);
        in.read1Bit();
        psse.lower_layer_horizontal_offset = in.readNBit(15);
        in.read1Bit();
        psse.lower_layer_vertical_offset = in.readNBit(15);
        psse.spatial_temporal_weight_code_table_index = in.readNBit(2);
        psse.lower_layer_progressive_frame = in.read1Bit();
        psse.lower_layer_deinterlaced_field_select = in.read1Bit();

        return psse;
    }

    public void write(BitWriter out) throws IOException {
        out.writeNBit(lower_layer_temporal_reference, 10);
        out.write1Bit(1); // todo: verify this
        out.writeNBit(lower_layer_horizontal_offset, 15);
        out.write1Bit(1); // todo: verify this
        out.writeNBit(lower_layer_vertical_offset, 15);
        out.writeNBit(spatial_temporal_weight_code_table_index, 2);
        out.write1Bit(lower_layer_progressive_frame);
        out.write1Bit(lower_layer_deinterlaced_field_select);
    }
}
