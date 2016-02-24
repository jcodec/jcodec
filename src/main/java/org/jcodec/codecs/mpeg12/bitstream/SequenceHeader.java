package org.jcodec.codecs.mpeg12.bitstream;

import static org.jcodec.codecs.mpeg12.MPEGConst.EXTENSION_START_CODE;

import java.nio.ByteBuffer;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SequenceHeader implements MPEGHeader {

    public static final int Sequence_Extension = 0x1;
    public static final int Sequence_Display_Extension = 0x2;
    public static final int Sequence_Scalable_Extension = 0x5;
    private static boolean _hasExtensions;

    public int horizontal_size;
    public int vertical_size;
    public int aspect_ratio_information;
    public int frame_rate_code;
    public int bit_rate;
    public int vbv_buffer_size_value;
    public int constrained_parameters_flag;
    public int[] intra_quantiser_matrix;
    public int[] non_intra_quantiser_matrix;

    public SequenceExtension sequenceExtension;
    public SequenceScalableExtension sequenceScalableExtension;
    public SequenceDisplayExtension sequenceDisplayExtension;
    
    public SequenceHeader(int horizontal_size, int vertical_size, int aspect_ratio_information, int frame_rate_code,
            int bit_rate, int vbv_buffer_size_value, int constrained_parameters_flag, int[] intra_quantiser_matrix,
            int[] non_intra_quantiser_matrix) {
        this.horizontal_size = horizontal_size;
        this.vertical_size = vertical_size;
        this.aspect_ratio_information = aspect_ratio_information;
        this.frame_rate_code = frame_rate_code;
        this.bit_rate = bit_rate;
        this.vbv_buffer_size_value = vbv_buffer_size_value;
        this.constrained_parameters_flag = constrained_parameters_flag;
        this.intra_quantiser_matrix = intra_quantiser_matrix;
        this.non_intra_quantiser_matrix = non_intra_quantiser_matrix;
    }
    
    private SequenceHeader() {
    }

    public static SequenceHeader read(ByteBuffer bb) {
        BitReader _in = BitReader.createBitReader(bb);
        SequenceHeader sh = new SequenceHeader();
        sh.horizontal_size = _in.readNBit(12);
        sh.vertical_size = _in.readNBit(12);
        sh.aspect_ratio_information = _in.readNBit(4);
        sh.frame_rate_code = _in.readNBit(4);
        sh.bit_rate = _in.readNBit(18);
        _in.read1Bit();
        sh.vbv_buffer_size_value = _in.readNBit(10);
        sh.constrained_parameters_flag = _in.read1Bit();
        if (_in.read1Bit() != 0) {
            sh.intra_quantiser_matrix = new int[64];
            for (int i = 0; i < 64; i++) {
                sh.intra_quantiser_matrix[i] = _in.readNBit(8);
            }
        }
        if (_in.read1Bit() != 0) {
            sh.non_intra_quantiser_matrix = new int[64];
            for (int i = 0; i < 64; i++) {
                sh.non_intra_quantiser_matrix[i] = _in.readNBit(8);
            }
        }

        return sh;
    }

    public static void readExtension(ByteBuffer bb, SequenceHeader sh) {
        _hasExtensions = true;

        BitReader _in = BitReader.createBitReader(bb);
        int extType = _in.readNBit(4);
        switch (extType) {
        case Sequence_Extension:
            sh.sequenceExtension = SequenceExtension.read(_in);
            break;
        case Sequence_Scalable_Extension:
            sh.sequenceScalableExtension = SequenceScalableExtension.read(_in);
            break;
        case Sequence_Display_Extension:
            sh.sequenceDisplayExtension = SequenceDisplayExtension.read(_in);
            break;
        default:
            throw new RuntimeException("Unsupported extension: " + extType);
        }
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(horizontal_size, 12);
        bw.writeNBit(vertical_size, 12);
        bw.writeNBit(aspect_ratio_information, 4);
        bw.writeNBit(frame_rate_code, 4);
        bw.writeNBit(bit_rate, 18);
        bw.write1Bit(1);
        bw.writeNBit(vbv_buffer_size_value, 10);
        bw.write1Bit(constrained_parameters_flag);
        bw.write1Bit(intra_quantiser_matrix != null ? 1 : 0);
        if (intra_quantiser_matrix != null) {
            for (int i = 0; i < 64; i++) {
                bw.writeNBit(intra_quantiser_matrix[i], 8);
            }
        }
        bw.write1Bit(non_intra_quantiser_matrix != null ? 1 : 0);
        if (non_intra_quantiser_matrix != null) {
            for (int i = 0; i < 64; i++) {
                bw.writeNBit(non_intra_quantiser_matrix[i], 8);
            }
        }
        
        bw.flush();

        writeExtensions(bb);
    }

    private void writeExtensions(ByteBuffer out) {
        if (sequenceExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            sequenceExtension.write(out);
        }

        if (sequenceScalableExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            sequenceScalableExtension.write(out);
        }

        if (sequenceDisplayExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            sequenceDisplayExtension.write(out);
        }
    }

    public boolean hasExtensions() {
        return _hasExtensions;
    }

    public void copyExtensions(SequenceHeader sh) {
        sequenceExtension = sh.sequenceExtension;
        sequenceScalableExtension = sh.sequenceScalableExtension;
        sequenceDisplayExtension = sh.sequenceDisplayExtension;
    }
}