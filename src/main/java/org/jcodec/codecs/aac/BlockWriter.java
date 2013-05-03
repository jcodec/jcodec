package org.jcodec.codecs.aac;

import static org.jcodec.codecs.aac.BlockType.TYPE_END;

import org.jcodec.codecs.aac.blocks.Block;
import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Writes blocks to form AAC frame
 * 
 * @author The JCodec project
 * 
 */
public class BlockWriter {
    
    public void nextBlock(BitWriter bits, Block block) {
        bits.writeNBit(block.getType().getCode(), 3);
        
        if (block.getType() == TYPE_END)
            return;
        
    }

}
