package org.jcodec.codecs.aac.blocks;

import org.jcodec.common.io.BitReader;

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

    public void parse(BitReader _in) {
        int elemType = (int) _in.readNBit(4);
        int byte_align = _in.read1Bit();
        int count = (int) _in.readNBit(8);
        if (count == 255)
            count += _in.readNBit(8);
        if (byte_align != 0)
            _in.align();

        if (_in.skip(8 * count) != 8 * count) {
            throw new RuntimeException("Overread");
        }
    }
}
