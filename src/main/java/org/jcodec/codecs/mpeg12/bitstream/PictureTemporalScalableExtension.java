package org.jcodec.codecs.mpeg12.bitstream;

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
public class PictureTemporalScalableExtension implements MPEGHeader {
    public int reference_select_code;
    public int forward_temporal_reference;
    public int backward_temporal_reference;
    public static final int Picture_Temporal_Scalable_Extension = 0x10;

    public static PictureTemporalScalableExtension read(BitReader _in) {
        PictureTemporalScalableExtension ptse = new PictureTemporalScalableExtension();
        ptse.reference_select_code = _in.readNBit(2);
        ptse.forward_temporal_reference = _in.readNBit(10);
        _in.read1Bit();
        ptse.backward_temporal_reference = _in.readNBit(10);

        return ptse;
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(PictureTemporalScalableExtension.Picture_Temporal_Scalable_Extension, 4);

        bw.writeNBit(reference_select_code, 2);
        bw.writeNBit(forward_temporal_reference, 10);
        bw.write1Bit(1); // todo: verify this
        bw.writeNBit(backward_temporal_reference, 10);
        bw.flush();
    }
}
