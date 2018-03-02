package org.jcodec.codecs.mpeg12.bitstream;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PictureCodingExtension implements MPEGHeader {

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
    public static final int Picture_Coding_Extension = 0x8;
    
    public PictureCodingExtension() {
        this.f_code = new int[2][2];
    }
    
    public static class CompositeDisplay {
        public int v_axis;
        public int field_sequence;
        public int sub_carrier;
        public int burst_amplitude;
        public int sub_carrier_phase;

        public static CompositeDisplay read(BitReader _in) {
            CompositeDisplay cd = new CompositeDisplay();
            cd.v_axis = _in.read1Bit();
            cd.field_sequence = _in.readNBit(3);
            cd.sub_carrier = _in.read1Bit();
            cd.burst_amplitude = _in.readNBit(7);
            cd.sub_carrier_phase = _in.readNBit(8);
            return cd;
        }

        public void write(BitWriter out) {
            out.write1Bit(v_axis);
            out.writeNBit(field_sequence, 3);
            out.write1Bit(sub_carrier);
            out.writeNBit(burst_amplitude, 7);
            out.writeNBit(sub_carrier_phase, 8);
        }
    }

    public static PictureCodingExtension read(BitReader _in) {
        PictureCodingExtension pce = new PictureCodingExtension();
        pce.f_code[0][0] = _in.readNBit(4);
        pce.f_code[0][1] = _in.readNBit(4);
        pce.f_code[1][0] = _in.readNBit(4);
        pce.f_code[1][1] = _in.readNBit(4);
        pce.intra_dc_precision = _in.readNBit(2);
        pce.picture_structure = _in.readNBit(2);
        pce.top_field_first = _in.read1Bit();
        pce.frame_pred_frame_dct = _in.read1Bit();
        pce.concealment_motion_vectors = _in.read1Bit();
        pce.q_scale_type = _in.read1Bit();
        pce.intra_vlc_format = _in.read1Bit();
        pce.alternate_scan = _in.read1Bit();
        pce.repeat_first_field = _in.read1Bit();
        pce.chroma_420_type = _in.read1Bit();
        pce.progressive_frame = _in.read1Bit();
        if (_in.read1Bit() != 0) {
            pce.compositeDisplay = CompositeDisplay.read(_in);
        }

        return pce;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(PictureCodingExtension.Picture_Coding_Extension, 4);
        bw.writeNBit(f_code[0][0], 4);
        bw.writeNBit(f_code[0][1], 4);
        bw.writeNBit(f_code[1][0], 4);
        bw.writeNBit(f_code[1][1], 4);
        bw.writeNBit(intra_dc_precision, 2);
        bw.writeNBit(picture_structure, 2);
        bw.write1Bit(top_field_first);
        bw.write1Bit(frame_pred_frame_dct);
        bw.write1Bit(concealment_motion_vectors);
        bw.write1Bit(q_scale_type);
        bw.write1Bit(intra_vlc_format);
        bw.write1Bit(alternate_scan);
        bw.write1Bit(repeat_first_field);
        bw.write1Bit(chroma_420_type);
        bw.write1Bit(progressive_frame);
        bw.write1Bit(compositeDisplay != null ? 1 : 0);
        if (compositeDisplay != null)
            compositeDisplay.write(bw);
        bw.flush();
    }
}
