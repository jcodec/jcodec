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
public class CopyrightExtension implements MPEGHeader {
    public int copyright_flag;
    public int copyright_identifier;
    public int original_or_copy;
    public int copyright_number_1;
    public int copyright_number_2;
    public int copyright_number_3;

    public static CopyrightExtension read(BitReader in) {
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

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        bw.writeNBit(PictureHeader.Copyright_Extension, 4);

        bw.write1Bit(copyright_flag);
        bw.writeNBit(copyright_identifier, 8);
        bw.write1Bit(original_or_copy);
        bw.writeNBit(0, 7);
        bw.write1Bit(1); // todo: verify this
        bw.writeNBit(copyright_number_1, 20);
        bw.write1Bit(1); // todo: verify this
        bw.writeNBit(copyright_number_2, 22);
        bw.write1Bit(1); // todo: verify this
        bw.writeNBit(copyright_number_3, 22);
        
        bw.flush();
    }
}
