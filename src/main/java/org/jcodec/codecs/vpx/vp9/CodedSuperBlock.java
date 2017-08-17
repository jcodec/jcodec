package org.jcodec.codecs.vpx.vp9;

import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X4;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_4X8;
import static org.jcodec.codecs.vpx.vp9.Consts.BLOCK_8X4;
import static org.jcodec.codecs.vpx.vp9.Consts.PARTITION_HORZ;
import static org.jcodec.codecs.vpx.vp9.Consts.PARTITION_NONE;
import static org.jcodec.codecs.vpx.vp9.Consts.PARTITION_SPLIT;
import static org.jcodec.codecs.vpx.vp9.Consts.PARTITION_VERT;
import static org.jcodec.codecs.vpx.vp9.Consts.SZ_8x8;
import static org.jcodec.codecs.vpx.vp9.Consts.TREE_PARTITION;
import static org.jcodec.codecs.vpx.vp9.Consts.blSizeLookup;

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

	protected CodedSuperBlock() {
	}

	public CodedBlock[] getCodedBlocks() {
		return codedBlocks;
	}

	public static CodedSuperBlock read(int miCol, int miRow, VPXBooleanDecoder decoder, DecodingContext c) {

		List<CodedBlock> blocks = new ArrayList<CodedBlock>();

		CodedSuperBlock result = new CodedSuperBlock();
		result.readSubPartition(miCol, miRow, 3, decoder, c, blocks);

		result.codedBlocks = blocks.toArray(CodedBlock.EMPTY_ARR);

		return result;
	}

	/**
	 * Needed for mocking
	 */
	protected CodedBlock readBlock(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder, DecodingContext c) {
		return CodedBlock.read(miCol, miRow, blSz, decoder, c);
	}

	protected void readSubPartition(int miCol, int miRow, int logBlkSize, VPXBooleanDecoder decoder, DecodingContext c,
			List<CodedBlock> blocks) {
		int part = readPartition(miCol, miRow, logBlkSize, decoder, c);
		int nextBlkSize = (1 << logBlkSize) >> 1;

		if (logBlkSize > SZ_8x8) {
			if (part == PARTITION_NONE) {
				CodedBlock blk = readBlock(miCol, miRow, blSizeLookup[1 + logBlkSize][1 + logBlkSize], decoder, c);
				blocks.add(blk);
				saveAboveSizes(miCol, 1 + logBlkSize, c);
				saveLeftSizes(miRow, 1 + logBlkSize, c);
			} else if (part == PARTITION_HORZ) {
				CodedBlock blk = readBlock(miCol, miRow, blSizeLookup[1 + logBlkSize][logBlkSize], decoder, c);
				blocks.add(blk);
				saveAboveSizes(miCol, 1 + logBlkSize, c);
				saveLeftSizes(miRow, logBlkSize, c);
				if (miRow + nextBlkSize < c.getMiTileHeight()) {
					blk = readBlock(miCol, miRow + nextBlkSize, blSizeLookup[1 + logBlkSize][logBlkSize], decoder, c);
					blocks.add(blk);
					saveLeftSizes(miRow + nextBlkSize, logBlkSize, c);
				}
			} else if (part == PARTITION_VERT) {
				CodedBlock blk = readBlock(miCol, miRow, blSizeLookup[logBlkSize][1 + logBlkSize], decoder, c);
				blocks.add(blk);
				saveLeftSizes(miRow, 1 + logBlkSize, c);
				saveAboveSizes(miCol, logBlkSize, c);
				if (miCol + nextBlkSize < c.getMiTileWidth()) {
					blk = readBlock(miCol + nextBlkSize, miRow, blSizeLookup[logBlkSize][1 + logBlkSize], decoder, c);
					blocks.add(blk);
					saveAboveSizes(miCol + nextBlkSize, logBlkSize, c);
				}
			} else {
				readSubPartition(miCol, miRow, logBlkSize - 1, decoder, c, blocks);
				if (miCol + nextBlkSize < c.getMiTileWidth())
					readSubPartition(miCol + nextBlkSize, miRow, logBlkSize - 1, decoder, c, blocks);
				if (miRow + nextBlkSize < c.getMiTileHeight())
					readSubPartition(miCol, miRow + nextBlkSize, logBlkSize - 1, decoder, c, blocks);
				if (miCol + nextBlkSize < c.getMiTileWidth() && miRow + nextBlkSize < c.getMiTileHeight())
					readSubPartition(miCol + nextBlkSize, miRow + nextBlkSize, logBlkSize - 1, decoder, c, blocks);
			}
		} else {
			int subBlSz = Consts.sub8x8PartitiontoBlockType[part];
			CodedBlock blk = readBlock(miCol, miRow, subBlSz, decoder, c);
			blocks.add(blk);
			saveAboveSizes(miCol, 1 + logBlkSize - (subBlSz == BLOCK_4X4 || subBlSz == BLOCK_4X8 ? 1 : 0), c);
			saveLeftSizes(miRow, 1 + logBlkSize - (subBlSz == BLOCK_4X4 || subBlSz == BLOCK_8X4 ? 1 : 0), c);
		}
	}

	private static void saveLeftSizes(int miRow, int blkSize4x4, DecodingContext c) {
		int blkSize8x8 = blkSize4x4 == 0 ? 0 : blkSize4x4 - 1;
		int miBlkSize = 1 << blkSize8x8;

		int[] leftSizes = c.getLeftPartitionSizes();
		for (int i = 0; i < miBlkSize; i++)
			leftSizes[(miRow % 8) + i] = blkSize4x4;
	}

	private static void saveAboveSizes(int miCol, int blkSize4x4, DecodingContext c) {
		int blkSize8x8 = blkSize4x4 == 0 ? 0 : blkSize4x4 - 1;
		int miBlkSize = 1 << blkSize8x8;

		int[] aboveSizes = c.getAbovePartitionSizes();
		for (int i = 0; i < miBlkSize; i++)
			aboveSizes[miCol + i] = blkSize4x4;
	}

	protected static int readPartition(int miCol, int miRow, int blkSize, VPXBooleanDecoder decoder,
			DecodingContext c) {
//		System.out.print(String.format("PARTITION [%d,%d,%d]", miCol, miRow, blkSize));
		int ctx = calcPartitionContext(miCol, miRow, blkSize, c);
//		System.out.printf(String.format(", ctx=%d\n", ctx));
		int[] probs = c.getPartitionProbs()[ctx];
		int halfBlk = (1 << blkSize) >> 1;
		boolean rightEdge = miCol + halfBlk >= c.getMiTileWidth();
		boolean bottomEdge = miRow + halfBlk >= c.getMiTileHeight();

		if (rightEdge && bottomEdge) {
			return PARTITION_SPLIT;
		} else if (rightEdge) {
			return decoder.readBit(probs[2]) == 1 ? PARTITION_SPLIT : PARTITION_VERT;
		} else if (bottomEdge) {
			return decoder.readBit(probs[1]) == 1 ? PARTITION_SPLIT : PARTITION_HORZ;
		} else {
			return decoder.readTree(TREE_PARTITION, probs);
		}
	}

	private static int calcPartitionContext(int miCol, int miRow, int blkSize, DecodingContext c) {
		boolean left = false, above = false;

		int[] aboveSizes = c.getAbovePartitionSizes();
	    above = aboveSizes[miCol] <= blkSize;

		int[] leftSizes = c.getLeftPartitionSizes();
	    left |= leftSizes[miRow % 8] <= blkSize;
	    
//	    System.out.println(String.format("ABOVE: %d, LEFT: %d\n", aboveSizes[miCol], leftSizes[miRow % 8]));

		return blkSize * 4 + (left ? 2 : 0) + (above ? 1 : 0);
	}
}
