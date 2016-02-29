package org.jcodec.codecs.aac;

import static org.jcodec.codecs.aac.BlockType.TYPE_END;

import org.jcodec.codecs.aac.blocks.Block;
import org.jcodec.common.io.BitReader;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Reads blocks of AAC frame
 * 
 * @author The JCodec project
 * 
 */
public class BlockReader {

    public Block nextBlock(BitReader bits) {
        BlockType type = BlockType.values()[(int) (long) bits.readNBit(3)];
        if (type == TYPE_END)
            return null;

        int id = (int) bits.readNBit(4);

        return null;
    }
}
