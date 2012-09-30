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
public class CopyrightExtension {
    public int copyright_flag;
    public int copyright_identifier;
    public int original_or_copy;
    public int copyright_number_1;
    public int copyright_number_2;
    public int copyright_number_3;

    public static CopyrightExtension read(InBits in) throws IOException {
        CopyrightExtension ce = new CopyrightExtension();
        ce.copyright_flag = in.read1Bit();
        ce.copyright_identifier = in.readNBit(8);
        ce.original_or_copy = in.read1Bit();
        in.skip(7);
        in.read1Bit();
        ce.copyright_number_1 = in.readNBit(20);
        in.read1Bit();
        ce.copyright_number_2 = in.readNBit(22);
        in.read1Bit();
        ce.copyright_number_3 = in.readNBit(22);
        return ce;
    }

    public void write(OutBits out) throws IOException {
        out.write1Bit(copyright_flag);
        out.writeNBit(copyright_identifier, 8);
        out.write1Bit(original_or_copy);
        out.writeNBit(0, 7);
        out.write1Bit(1); // todo: verify this
        out.writeNBit(copyright_number_1, 20);
        out.write1Bit(1); // todo: verify this
        out.writeNBit(copyright_number_2, 22);
        out.write1Bit(1); // todo: verify this
        out.writeNBit(copyright_number_3, 22);
    }
}
