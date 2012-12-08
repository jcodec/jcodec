package org.jcodec.codecs.mpeg12.bitstream;

import java.io.IOException;

import org.jcodec.common.io.InBits;
import org.jcodec.common.io.OutBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PictureCodingExtension {
    
    
    public static final int Top_Field = 1;
    public static final int Bottom_Field = 2;
    public static final int Frame = 3;
    
    public int[][] f_code;
    public int intra_dc_precision;
    public int picture_structure;
    public int top_field_first;
    public int frame_pred_frame_dct;
    public int concealment_motion_vectors;
    public int q_scale_type;
    public int intra_vlc_format;
    public int alternate_scan;
    public int repeat_first_field;
    public int chroma_420_type;
    public int progressive_frame;
    public CompositeDisplay compositeDisplay;

    public static class CompositeDisplay {
        public int v_axis;
        public int field_sequence;
        public int sub_carrier;
        public int burst_amplitude;
        public int sub_carrier_phase;

        public static CompositeDisplay read(InBits in) throws IOException {
            CompositeDisplay cd = new CompositeDisplay();
            cd.v_axis = in.read1Bit();
            cd.field_sequence = in.readNBit(3);
            cd.sub_carrier = in.read1Bit();
            cd.burst_amplitude = in.readNBit(7);
            cd.sub_carrier_phase = in.readNBit(8);
            return cd;
        }

        public void write(OutBits out) throws IOException {
            out.write1Bit(v_axis);
            out.writeNBit(field_sequence, 3);
            out.write1Bit(sub_carrier);
            out.writeNBit(burst_amplitude, 7);
            out.writeNBit(sub_carrier_phase, 8);
        }
    }

    public static PictureCodingExtension read(InBits in) throws IOException {
        PictureCodingExtension pce = new PictureCodingExtension();
        pce.f_code = new int[2][2];
        pce.f_code[0][0] = in.readNBit(4);
        pce.f_code[0][1] = in.readNBit(4);
        pce.f_code[1][0] = in.readNBit(4);
        pce.f_code[1][1] = in.readNBit(4);
        pce.intra_dc_precision = in.readNBit(2);
        pce.picture_structure = in.readNBit(2);
        pce.top_field_first = in.read1Bit();
        pce.frame_pred_frame_dct = in.read1Bit();
        pce.concealment_motion_vectors = in.read1Bit();
        pce.q_scale_type = in.read1Bit();
        pce.intra_vlc_format = in.read1Bit();
        pce.alternate_scan = in.read1Bit();
        pce.repeat_first_field = in.read1Bit();
        pce.chroma_420_type = in.read1Bit();
        pce.progressive_frame = in.read1Bit();
        if (in.read1Bit() != 0) {
            pce.compositeDisplay = CompositeDisplay.read(in);
        }

        return pce;
    }

    public void write(OutBits out) throws IOException {
        out.writeNBit(f_code[0][0], 4);
        out.writeNBit(f_code[0][1], 4);
        out.writeNBit(f_code[1][0], 4);
        out.writeNBit(f_code[1][1], 4);
        out.writeNBit(intra_dc_precision, 2);
        out.writeNBit(picture_structure, 2);
        out.write1Bit(top_field_first);
        out.write1Bit(frame_pred_frame_dct);
        out.write1Bit(concealment_motion_vectors);
        out.write1Bit(q_scale_type);
        out.write1Bit(intra_vlc_format);
        out.write1Bit(alternate_scan);
        out.write1Bit(repeat_first_field);
        out.write1Bit(chroma_420_type);
        out.write1Bit(progressive_frame);
        out.write1Bit(compositeDisplay != null ? 1 : 0);
        if (compositeDisplay != null)
            compositeDisplay.write(out);
    }
}
