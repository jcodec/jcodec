package org.jcodec.codecs.mpeg12.bitstream;

import static org.jcodec.codecs.mpeg12.MPEGConst.EXTENSION_START_CODE;

import java.io.IOException;
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
public class PictureHeader {

    public static final int Quant_Matrix_Extension = 0x3;
    public static final int Copyright_Extension = 0x4;
    public static final int Picture_Display_Extension = 0x7;
    public static final int Picture_Coding_Extension = 0x8;
    public static final int Picture_Spatial_Scalable_Extension = 0x9;
    public static final int Picture_Temporal_Scalable_Extension = 0x10;

    public static final int IntraCoded = 0x1;
    public static final int PredictiveCoded = 0x2;
    public static final int BiPredictiveCoded = 0x3;

    public int temporal_reference;
    public int picture_coding_type;
    public int vbv_delay;
    public int full_pel_forward_vector;
    public int forward_f_code;
    public int full_pel_backward_vector;
    public int backward_f_code;

    public QuantMatrixExtension quantMatrixExtension;
    public CopyrightExtension copyrightExtension;
    public PictureDisplayExtension pictureDisplayExtension;
    public PictureCodingExtension pictureCodingExtension;
    public PictureSpatialScalableExtension pictureSpatialScalableExtension;
    public PictureTemporalScalableExtension pictureTemporalScalableExtension;
    private boolean hasExtensions;

    public static PictureHeader read(ByteBuffer bb) {
        BitReader in = new BitReader(bb);
        PictureHeader ph = new PictureHeader();
        ph.temporal_reference = in.readNBit(10);
        ph.picture_coding_type = in.readNBit(3);
        ph.vbv_delay = in.readNBit(16);
        if (ph.picture_coding_type == 2 || ph.picture_coding_type == 3) {
            ph.full_pel_forward_vector = in.read1Bit();
            ph.forward_f_code = in.readNBit(3);
        }
        if (ph.picture_coding_type == 3) {
            ph.full_pel_backward_vector = in.read1Bit();
            ph.backward_f_code = in.readNBit(3);
        }
        while (in.read1Bit() == 1) {
            in.readNBit(8);
        }

        return ph;
    }

    public static void readExtension(ByteBuffer bb, PictureHeader ph, SequenceHeader sh) {
        ph.hasExtensions = true;
        BitReader in = new BitReader(bb);
        int extType = in.readNBit(4);
        switch (extType) {
        case Quant_Matrix_Extension:
            ph.quantMatrixExtension = QuantMatrixExtension.read(in);
            break;
        case Copyright_Extension:
            ph.copyrightExtension = CopyrightExtension.read(in);
            break;
        case Picture_Display_Extension:
            ph.pictureDisplayExtension = PictureDisplayExtension.read(in, sh.sequenceExtension,
                    ph.pictureCodingExtension);
            break;
        case Picture_Coding_Extension:
            ph.pictureCodingExtension = PictureCodingExtension.read(in);
            break;
        case Picture_Spatial_Scalable_Extension:
            ph.pictureSpatialScalableExtension = PictureSpatialScalableExtension.read(in);
            break;
        case Picture_Temporal_Scalable_Extension:
            ph.pictureTemporalScalableExtension = PictureTemporalScalableExtension.read(in);
            break;
        default:
            throw new RuntimeException("Unsupported extension: " + extType);
        }
    }

    public void write(ByteBuffer os) throws IOException {
        BitWriter out = new BitWriter(os);
        out.writeNBit(temporal_reference, 10);
        out.writeNBit(picture_coding_type, 3);
        out.writeNBit(vbv_delay, 16);
        if (picture_coding_type == 2 || picture_coding_type == 3) {
            out.write1Bit(full_pel_forward_vector);
            out.write1Bit(forward_f_code);
        }
        if (picture_coding_type == 3) {
            out.write1Bit(full_pel_backward_vector);
            out.writeNBit(backward_f_code, 3);
        }
        out.write1Bit(0);

        writeExtensions(os);
    }

    private void writeExtensions(ByteBuffer out) throws IOException {
        if (quantMatrixExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Quant_Matrix_Extension, 4);
            quantMatrixExtension.write(os);
        }

        if (copyrightExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Copyright_Extension, 4);
            copyrightExtension.write(os);
        }

        if (pictureCodingExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Picture_Coding_Extension, 4);
            pictureCodingExtension.write(os);
        }

        if (pictureDisplayExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Picture_Display_Extension, 4);
            pictureDisplayExtension.write(os);
        }

        if (pictureSpatialScalableExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Picture_Spatial_Scalable_Extension, 4);
            pictureSpatialScalableExtension.write(os);
        }

        if (pictureTemporalScalableExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            BitWriter os = new BitWriter(out);
            os.writeNBit(Picture_Temporal_Scalable_Extension, 4);
            pictureTemporalScalableExtension.write(os);
        }
    }

    public boolean hasExtensions() {
        return hasExtensions;
    }
}
