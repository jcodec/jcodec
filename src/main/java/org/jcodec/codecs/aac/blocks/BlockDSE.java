package org.jcodec.codecs.aac.blocks;

import java.io.IOException;

import org.jcodec.common.io.InBits;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Skip data_stream_element; reference: table 4.10.
 * 
 * @author The JCodec project
 * 
 */
public class BlockDSE extends Block {

    public void parse(InBits in) throws IOException {
        int elemType = (int) in.readNBit(4);
        int byte_align = in.read1Bit();
        int count = (int) in.readNBit(8);
        if (count == 255)
            count += in.readNBit(8);
        if (byte_align != 0)
            in.align();

        if (in.skip(8 * count) != 8 * count) {
            throw new RuntimeException("Overread");
        }
    }
}
