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
public class SequenceHeader {

    public static final int Sequence_Extension = 0x1;
    public static final int Sequence_Display_Extension = 0x2;
    public static final int Sequence_Scalable_Extension = 0x5;
    private static boolean hasExtensions;

    public int horizontal_size;
    public int vertical_size;
    public int aspect_ratio_information;
    public int frame_rate_code;
    public int bit_rate;
    public int marker_bit;
    public int vbv_buffer_size_value;
    public int constrained_parameters_flag;
    public int[] intra_quantiser_matrix;
    public int[] non_intra_quantiser_matrix;

    public SequenceExtension sequenceExtension;
    public SequenceScalableExtension sequenceScalableExtension;
    public SequenceDisplayExtension sequenceDisplayExtension;

    public static SequenceHeader read(ByteBuffer bb) {
        BitReader in = new BitReader(bb);
        SequenceHeader sh = new SequenceHeader();
        sh.horizontal_size = in.readNBit(12);
        sh.vertical_size = in.readNBit(12);
        sh.aspect_ratio_information = in.readNBit(4);
        sh.frame_rate_code = in.readNBit(4);
        sh.bit_rate = in.readNBit(18);
        sh.marker_bit = in.read1Bit();
        sh.vbv_buffer_size_value = in.readNBit(10);
        sh.constrained_parameters_flag = in.read1Bit();
        if (in.read1Bit() != 0) {
            sh.intra_quantiser_matrix = new int[64];
            for (int i = 0; i < 64; i++) {
                sh.intra_quantiser_matrix[i] = in.readNBit(8);
            }
        }
        if (in.read1Bit() != 0) {
            sh.non_intra_quantiser_matrix = new int[64];
            for (int i = 0; i < 64; i++) {
                sh.non_intra_quantiser_matrix[i] = in.readNBit(8);
            }
        }

        return sh;
    }

    public static void readExtension(ByteBuffer bb, SequenceHeader sh) {
        hasExtensions = true;

        BitReader in = new BitReader(bb);
        int extType = in.readNBit(4);
        switch (extType) {
        case Sequence_Extension:
            sh.sequenceExtension = SequenceExtension.read(in);
            break;
        case Sequence_Scalable_Extension:
            sh.sequenceScalableExtension = SequenceScalableExtension.read(in);
            break;
        case Sequence_Display_Extension:
            sh.sequenceDisplayExtension = SequenceDisplayExtension.read(in);
            break;
        default:
            throw new RuntimeException("Unsupported extension: " + extType);
        }
    }

    public void write(ByteBuffer os) {
        BitWriter out = new BitWriter(os);
        out.writeNBit(horizontal_size, 12);
        out.writeNBit(vertical_size, 12);
        out.writeNBit(aspect_ratio_information, 4);
        out.writeNBit(frame_rate_code, 4);
        out.writeNBit(bit_rate, 18);
        out.write1Bit(marker_bit);
        out.writeNBit(vbv_buffer_size_value, 10);
        out.write1Bit(constrained_parameters_flag);
        out.write1Bit(intra_quantiser_matrix != null ? 1 : 0);
        if (intra_quantiser_matrix != null) {
            for (int i = 0; i < 64; i++) {
                out.writeNBit(intra_quantiser_matrix[i], 8);
            }
        }
        out.write1Bit(non_intra_quantiser_matrix != null ? 1 : 0);
        if (non_intra_quantiser_matrix != null) {
            for (int i = 0; i < 64; i++) {
                out.writeNBit(non_intra_quantiser_matrix[i], 8);
            }
        }

        writeExtensions(os);
    }

    private void writeExtensions(ByteBuffer out) {
        if (sequenceExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Sequence_Extension, 4);
            sequenceExtension.write(os);
        }

        if (sequenceScalableExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Sequence_Scalable_Extension, 4);
            sequenceScalableExtension.write(os);
        }

        if (sequenceDisplayExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Sequence_Display_Extension, 4);
            sequenceDisplayExtension.write(os);
        }
    }

    public boolean hasExtensions() {
        return hasExtensions;
    }

    public void copyExtensions(SequenceHeader sh) {
        sequenceExtension = sh.sequenceExtension;
        sequenceScalableExtension = sh.sequenceScalableExtension;
        sequenceDisplayExtension = sh.sequenceDisplayExtension;
    }
}