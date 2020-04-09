package org.jcodec.codecs.vpx.vp9

import org.jcodec.codecs.vpx.VPXBooleanDecoder
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class CodedSuperBlock(private var codedBlocks: Array<CodedBlock>) {

    /**
     * Needed for mocking
     */
    protected open fun readBlock(miCol: Int, miRow: Int, blSz: Int, decoder: VPXBooleanDecoder?, c: DecodingContext?): CodedBlock {
        return CodedBlock.read(miCol, miRow, blSz, decoder, c)
    }

    fun readSubPartition(miCol: Int, miRow: Int, logBlkSize: Int, decoder: VPXBooleanDecoder, c: DecodingContext,
                         blocks: MutableList<CodedBlock>) {
        val part = readPartition(miCol, miRow, logBlkSize, decoder, c)
        val nextBlkSize = 1 shl logBlkSize shr 1
        if (logBlkSize > Consts.SZ_8x8) {
            if (part == Consts.PARTITION_NONE) {
                val blk = readBlock(miCol, miRow, Consts.blSizeLookup[1 + logBlkSize][1 + logBlkSize], decoder, c)
                blocks.add(blk)
                saveAboveSizes(miCol, 1 + logBlkSize, c)
                saveLeftSizes(miRow, 1 + logBlkSize, c)
            } else if (part == Consts.PARTITION_HORZ) {
                var blk = readBlock(miCol, miRow, Consts.blSizeLookup[1 + logBlkSize][logBlkSize], decoder, c)
                blocks.add(blk)
                saveAboveSizes(miCol, 1 + logBlkSize, c)
                saveLeftSizes(miRow, logBlkSize, c)
                if (miRow + nextBlkSize < c.miTileHeight) {
                    blk = readBlock(miCol, miRow + nextBlkSize, Consts.blSizeLookup[1 + logBlkSize][logBlkSize], decoder, c)
                    blocks.add(blk)
                    saveLeftSizes(miRow + nextBlkSize, logBlkSize, c)
                }
            } else if (part == Consts.PARTITION_VERT) {
                var blk = readBlock(miCol, miRow, Consts.blSizeLookup[logBlkSize][1 + logBlkSize], decoder, c)
                blocks.add(blk)
                saveLeftSizes(miRow, 1 + logBlkSize, c)
                saveAboveSizes(miCol, logBlkSize, c)
                if (miCol + nextBlkSize < c.miTileWidth) {
                    blk = readBlock(miCol + nextBlkSize, miRow, Consts.blSizeLookup[logBlkSize][1 + logBlkSize], decoder, c)
                    blocks.add(blk)
                    saveAboveSizes(miCol + nextBlkSize, logBlkSize, c)
                }
            } else {
                readSubPartition(miCol, miRow, logBlkSize - 1, decoder, c, blocks)
                if (miCol + nextBlkSize < c.miTileWidth) readSubPartition(miCol + nextBlkSize, miRow, logBlkSize - 1, decoder, c, blocks)
                if (miRow + nextBlkSize < c.miTileHeight) readSubPartition(miCol, miRow + nextBlkSize, logBlkSize - 1, decoder, c, blocks)
                if (miCol + nextBlkSize < c.miTileWidth && miRow + nextBlkSize < c.miTileHeight) readSubPartition(miCol + nextBlkSize, miRow + nextBlkSize, logBlkSize - 1, decoder, c, blocks)
            }
        } else {
            val subBlSz = Consts.sub8x8PartitiontoBlockType[part]
            val blk = readBlock(miCol, miRow, subBlSz, decoder, c)
            blocks.add(blk)
            saveAboveSizes(miCol, 1 + logBlkSize - if (subBlSz == Consts.BLOCK_4X4 || subBlSz == Consts.BLOCK_4X8) 1 else 0, c)
            saveLeftSizes(miRow, 1 + logBlkSize - if (subBlSz == Consts.BLOCK_4X4 || subBlSz == Consts.BLOCK_8X4) 1 else 0, c)
        }
    }

    companion object {
        fun read(miCol: Int, miRow: Int, decoder: VPXBooleanDecoder, c: DecodingContext): CodedSuperBlock {
            val blocks: MutableList<CodedBlock> = ArrayList()
            val result = CodedSuperBlock(emptyArray())
            result.readSubPartition(miCol, miRow, 3, decoder, c, blocks)
            result.codedBlocks = blocks.toTypedArray()
            return result
        }

        private fun saveLeftSizes(miRow: Int, blkSize4x4: Int, c: DecodingContext) {
            val blkSize8x8 = if (blkSize4x4 == 0) 0 else blkSize4x4 - 1
            val miBlkSize = 1 shl blkSize8x8
            val leftSizes = c.getLeftPartitionSizes()
            for (i in 0 until miBlkSize) leftSizes[miRow % 8 + i] = blkSize4x4
        }

        private fun saveAboveSizes(miCol: Int, blkSize4x4: Int, c: DecodingContext) {
            val blkSize8x8 = if (blkSize4x4 == 0) 0 else blkSize4x4 - 1
            val miBlkSize = 1 shl blkSize8x8
            val aboveSizes = c.getAbovePartitionSizes()
            for (i in 0 until miBlkSize) aboveSizes[miCol + i] = blkSize4x4
        }

        @JvmStatic
        fun readPartition(miCol: Int, miRow: Int, blkSize: Int, decoder: VPXBooleanDecoder,
                          c: DecodingContext): Int {
//		System.out.print(String.format("PARTITION [%d,%d,%d]", miCol, miRow, blkSize));
            val ctx = calcPartitionContext(miCol, miRow, blkSize, c)
            //		System.out.printf(String.format(", ctx=%d\n", ctx));
            val probs = c.getPartitionProbs()[ctx]
            val halfBlk = 1 shl blkSize shr 1
            val rightEdge = miCol + halfBlk >= c.miTileWidth
            val bottomEdge = miRow + halfBlk >= c.miTileHeight
            return if (rightEdge && bottomEdge) {
                Consts.PARTITION_SPLIT
            } else if (rightEdge) {
                if (decoder.readBit(probs[2]) == 1) Consts.PARTITION_SPLIT else Consts.PARTITION_VERT
            } else if (bottomEdge) {
                if (decoder.readBit(probs[1]) == 1) Consts.PARTITION_SPLIT else Consts.PARTITION_HORZ
            } else {
                decoder.readTree(Consts.TREE_PARTITION, probs)
            }
        }

        private fun calcPartitionContext(miCol: Int, miRow: Int, blkSize: Int, c: DecodingContext): Int {
            var left = false
            var above = false
            val aboveSizes = c.getAbovePartitionSizes()
            above = aboveSizes[miCol] <= blkSize
            val leftSizes = c.getLeftPartitionSizes()
            left = left or (leftSizes[miRow % 8] <= blkSize)

//	    System.out.println(String.format("ABOVE: %d, LEFT: %d\n", aboveSizes[miCol], leftSizes[miRow % 8]));
            return blkSize * 4 + (if (left) 2 else 0) + if (above) 1 else 0
        }
    }
}