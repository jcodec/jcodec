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
public class PictureTemporalScalableExtension {
    public int reference_select_code;
    public int forward_temporal_reference;
    public int backward_temporal_reference;

    public static PictureTemporalScalableExtension read(InBits in) throws IOException {
        PictureTemporalScalableExtension ptse = new PictureTemporalScalableExtension();
        ptse.reference_select_code = in.readNBit(2);
        ptse.forward_temporal_reference = in.readNBit(10);
        in.read1Bit();
        ptse.backward_temporal_reference = in.readNBit(10);

        return ptse;
    }

    public void write(OutBits out) throws IOException {
        out.writeNBit(reference_select_code, 2);
        out.writeNBit(forward_temporal_reference, 10);
        out.write1Bit(1); // todo: verify this
        out.writeNBit(backward_temporal_reference, 10);
    }
}
