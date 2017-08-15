package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.*;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CodedSuperBlock {

    private CodedBlock[] codedBlocks;

    public CodedSuperBlock(CodedBlock[] codedBlocks) {
        this.codedBlocks = codedBlocks;
    }

    public CodedBlock[] getCodedBlocks() {
        return codedBlocks;
    }

    public static CodedSuperBlock read(int miCol, int miRow, VPXBooleanDecoder decoder,
            DecodingContext c) {

        List<CodedBlock> blocks = new ArrayList<CodedBlock>();

        readCodedBlocks(miCol, miRow, 3, decoder, c, blocks);

        return new CodedSuperBlock(blocks.toArray(CodedBlock.EMPTY_ARR));
    }

    private static void readCodedBlocks(int miCol, int miRow, int logBlkSize, VPXBooleanDecoder decoder,
            DecodingContext c, List<CodedBlock> blocks) {
        int part = readPartition(miCol, miRow, logBlkSize, decoder, c);
        int nextBlkSize = (1 << logBlkSize) >> 1;

        if (part == PARTITION_NONE) {
            CodedBlock blk = CodedBlock.read(miCol, miRow, blSizeLookup[logBlkSize][logBlkSize], decoder, c);
            blocks.add(blk);
            saveAboveSizes(miCol, logBlkSize, c);
            saveLeftSizes(miRow, logBlkSize, c);
        } else if (part == PARTITION_HORZ) {
            CodedBlock blk = CodedBlock.read(miCol, miRow, blSizeLookup[logBlkSize][logBlkSize], decoder, c);
            blocks.add(blk);
            saveAboveSizes(miCol, nextBlkSize, c);
            if (miCol + nextBlkSize < c.getTileWidth()) {
                blk = CodedBlock.read(miCol + nextBlkSize, miRow, blSizeLookup[logBlkSize][logBlkSize], decoder,
                         c);
                blocks.add(blk);
                saveAboveSizes(miCol + nextBlkSize, nextBlkSize, c);
            }
            saveLeftSizes(miRow, logBlkSize, c);
        } else if (part == PARTITION_VERT) {
            CodedBlock blk = CodedBlock.read(miCol, miRow, blSizeLookup[logBlkSize][logBlkSize], decoder, c);
            blocks.add(blk);
            saveLeftSizes(miRow, nextBlkSize, c);
            if (miRow + nextBlkSize < c.getTileHeight()) {
                blk = CodedBlock.read(miCol, miRow + nextBlkSize, blSizeLookup[logBlkSize][logBlkSize], decoder,
                        c);
                blocks.add(blk);
                saveAboveSizes(miCol, logBlkSize, c);
            }
            saveLeftSizes(miRow + nextBlkSize, nextBlkSize, c);
        } else {
            if (nextBlkSize > SZ_8x8) {
                readCodedBlocks(miCol, miRow, logBlkSize - 1, decoder, c, blocks);
                if (miCol + nextBlkSize < c.getTileWidth())
                    readCodedBlocks(miCol + nextBlkSize, miRow, logBlkSize - 1, decoder, c, blocks);
                if (miRow + nextBlkSize < c.getTileHeight())
                    readCodedBlocks(miCol, miRow + nextBlkSize, logBlkSize - 1, decoder, c, blocks);
                if (miCol + nextBlkSize < c.getTileWidth() && miRow + nextBlkSize < c.getTileHeight())
                    readCodedBlocks(miCol + nextBlkSize, miRow + nextBlkSize, logBlkSize - 1, decoder, c,
                            blocks);
            } else {
                CodedBlock blk = CodedBlock.read(miCol, miRow, blSizeLookup[logBlkSize][logBlkSize], decoder,
                        c);
                blocks.add(blk);
                saveAboveSizes(miCol, nextBlkSize, c);
                saveLeftSizes(miRow, nextBlkSize, c);
                if (miCol + nextBlkSize < c.getTileWidth()) {
                    blk = CodedBlock.read(miCol + nextBlkSize, miRow, blSizeLookup[logBlkSize][logBlkSize], decoder,
                            c);
                    blocks.add(blk);
                    saveAboveSizes(miCol + nextBlkSize, nextBlkSize, c);
                }
                if (miRow + nextBlkSize < c.getTileHeight()) {
                    blk = CodedBlock.read(miCol, miRow + nextBlkSize, blSizeLookup[logBlkSize][logBlkSize], decoder,
                            c);
                    blocks.add(blk);
                    saveLeftSizes(miRow + nextBlkSize, nextBlkSize, c);
                }
                if (miCol + nextBlkSize < c.getTileWidth() && miRow + nextBlkSize < c.getTileHeight()) {
                    blk = CodedBlock.read(miCol + nextBlkSize, miRow + nextBlkSize,
                            blSizeLookup[logBlkSize][logBlkSize], decoder, c);
                    blocks.add(blk);
                }
            }
        }
    }

    private static void saveLeftSizes(int miRow, int logBlkSize, DecodingContext c) {
        int miBlkSize = 1 << logBlkSize;

        int[] leftSizes = c.getLeftPartitionSizes();
        for (int i = 0; i < miBlkSize; i++)
            leftSizes[(miRow % 8) + i] = logBlkSize;
    }

    private static void saveAboveSizes(int miCol, int logBlkSize, DecodingContext c) {
        int miBlkSize = 1 << logBlkSize;

        int[] aboveSizes = c.getAbovePartitionSizes();
        for (int i = 0; i < miBlkSize; i++)
            aboveSizes[miCol + i] = logBlkSize;
    }

    private static int readPartition(int miCol, int miRow, int blkSize, VPXBooleanDecoder decoder,
            DecodingContext c) {
        int ctx = calcPartitionContext(miCol, miRow, blkSize, c);
        int[] probs = c.getPartitionProbs()[ctx];
        int halfBlk = (1 << blkSize) >> 1;
        boolean rightEdge = miCol + halfBlk < c.getTileWidth();
        boolean bottomEdge = miRow + halfBlk < c.getTileHeight();

        if (rightEdge && bottomEdge) {
            return PARTITION_SPLIT;
        } else if (rightEdge) {
            return decoder.readBit(probs[0]) == 1 ? PARTITION_VERT : PARTITION_NONE;
        } else if (bottomEdge) {
            return decoder.readBit(probs[1]) == 1 ? PARTITION_HORZ : PARTITION_NONE;
        } else {
            return decoder.readTree(TREE_PARTITION, probs);
        }
    }

    private static int calcPartitionContext(int miCol, int miRow, int blkSize, DecodingContext c) {
        boolean left = false, above = false;
        int miBlkSize = 1 << blkSize;

        int[] aboveSizes = c.getAbovePartitionSizes();
        for (int i = 0; i < miBlkSize; i++)
            above |= aboveSizes[miCol + i] <= blkSize;

        int[] leftSizes = c.getLeftPartitionSizes();
        for (int i = 0; i < miBlkSize; i++)
            left |= leftSizes[(miRow % 8) + i] <= blkSize;

        return blkSize * 4 + (above ? 2 : 0) + (left ? 1 : 0);
    }
}
