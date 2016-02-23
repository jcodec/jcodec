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
public class PictureHeader implements MPEGHeader {

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
    

    public PictureHeader(int temporal_reference, int picture_coding_type, int vbv_delay, int full_pel_forward_vector,
            int forward_f_code, int full_pel_backward_vector, int backward_f_code) {
        this.temporal_reference = temporal_reference;
        this.picture_coding_type = picture_coding_type;
        this.vbv_delay = vbv_delay;
        this.full_pel_forward_vector = full_pel_forward_vector;
        this.forward_f_code = forward_f_code;
        this.full_pel_backward_vector = full_pel_backward_vector;
        this.backward_f_code = backward_f_code;
    }
    
    private PictureHeader() {
    }

    public static PictureHeader read(ByteBuffer bb) {
        BitReader _in = new BitReader(bb);
        PictureHeader ph = new PictureHeader();
        ph.temporal_reference = _in.readNBit(10);
        ph.picture_coding_type = _in.readNBit(3);
        ph.vbv_delay = _in.readNBit(16);
        if (ph.picture_coding_type == 2 || ph.picture_coding_type == 3) {
            ph.full_pel_forward_vector = _in.read1Bit();
            ph.forward_f_code = _in.readNBit(3);
        }
        if (ph.picture_coding_type == 3) {
            ph.full_pel_backward_vector = _in.read1Bit();
            ph.backward_f_code = _in.readNBit(3);
        }
        while (_in.read1Bit() == 1) {
            _in.readNBit(8);
        }

        return ph;
    }

    public static void readExtension(ByteBuffer bb, PictureHeader ph, SequenceHeader sh) {
        ph.hasExtensions = true;
        BitReader _in = new BitReader(bb);
        int extType = _in.readNBit(4);
        switch (extType) {
        case Quant_Matrix_Extension:
            ph.quantMatrixExtension = QuantMatrixExtension.read(_in);
            break;
        case Copyright_Extension:
            ph.copyrightExtension = CopyrightExtension.read(_in);
            break;
        case Picture_Display_Extension:
            ph.pictureDisplayExtension = PictureDisplayExtension.read(_in, sh.sequenceExtension,
                    ph.pictureCodingExtension);
            break;
        case Picture_Coding_Extension:
            ph.pictureCodingExtension = PictureCodingExtension.read(_in);
            break;
        case Picture_Spatial_Scalable_Extension:
            ph.pictureSpatialScalableExtension = PictureSpatialScalableExtension.read(_in);
            break;
        case Picture_Temporal_Scalable_Extension:
            ph.pictureTemporalScalableExtension = PictureTemporalScalableExtension.read(_in);
            break;
        default:
            throw new RuntimeException("Unsupported extension: " + extType);
        }
    }

    @Override
    public void write(ByteBuffer os) {
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
        out.flush();

        writeExtensions(os);
    }

    private void writeExtensions(ByteBuffer out) {
        if (quantMatrixExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            quantMatrixExtension.write(out);
        }

        if (copyrightExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            copyrightExtension.write(out);
        }

        if (pictureCodingExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            pictureCodingExtension.write(out);
        }

        if (pictureDisplayExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            pictureDisplayExtension.write(out);
        }

        if (pictureSpatialScalableExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            pictureSpatialScalableExtension.write(out);
        }

        if (pictureTemporalScalableExtension != null) {
            out.putInt(EXTENSION_START_CODE);
            pictureTemporalScalableExtension.write(out);
        }
    }

    public boolean hasExtensions() {
        return hasExtensions;
    }
}
