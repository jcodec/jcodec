package org.jcodec.codecs.vpx;

import static org.jcodec.codecs.vpx.VP8Util.vp8CoefUpdateProbs;
import static org.jcodec.common.tools.MathUtil.clip;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jcodec.codecs.vpx.VPXBitstream;
import org.jcodec.codecs.vpx.VPXBooleanEncoder;
import org.jcodec.codecs.vpx.VPXConst;
import org.jcodec.codecs.vpx.VPXDCT;
import org.jcodec.codecs.vpx.VPXQuantizer;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VP8Encoder extends VideoEncoder {
	public static final ColorSpace[] supportedColorSpaces = new ColorSpace[] { ColorSpace.YUV420 };

	final static int INT_TO_BYTE_OFFSET = 128;

	private VPXBitstream bitstream;
	private int[][] leftRow = new int[][] { new int[16], new int[8], new int[8] };
	private int[][] topLine;
	private VPXQuantizer quantizer = new VPXQuantizer();
	private int[] tmp = new int[16];
	private int qp;
	private byte[][] prevPic;

	public static VP8Encoder createVP8Encoder(int qp) {
		return new VP8Encoder(qp);
	}

	public VP8Encoder() {
		this(20);
	}

	public VP8Encoder(int qp) {
		this.qp = qp;
	}

	@Override
	public EncodedFrame encodeFrame(Picture pic, ByteBuffer _buf) {
		boolean isKeyFrame = false;
		if (prevPic == null) {
			isKeyFrame = true;
		} else {
			// Detects if there are no changes to the picture compared to the previous
			byte[][] dataNew = pic.getData();
			outer: for (int i = 0; i < dataNew.length; i++) {
				for (int j = 0; j < dataNew[i].length; j++) {
					if (dataNew[i][j] != prevPic[i][j]) {
						isKeyFrame = true;
						break outer;
					}
				}
			}
		}
		if (isKeyFrame) {
			// Saves the current frame for future reference
			byte[][] keyFrameData=pic.getData();
			prevPic=new byte[keyFrameData.length][];
			for(int i=0;i<keyFrameData.length;i++) {
				prevPic[i]=	Arrays.copyOf(keyFrameData[i],keyFrameData[i].length);
			}
		}
		ByteBuffer out = _buf.duplicate();
		int mbWidth = ((pic.getWidth() + 15) >> 4);
		int mbHeight = ((pic.getHeight() + 15) >> 4);
		bitstream = new VPXBitstream(VPXConst.tokenDefaultBinProbs, mbWidth);
		if (topLine == null) {
			topLine = new int[][] { new int[mbWidth << 4], new int[mbWidth << 3], new int[mbWidth << 3] };
		}
		initValue(leftRow, 129);
		initValue(topLine, 127);
		out.order(ByteOrder.LITTLE_ENDIAN);
		if(isKeyFrame) {
			// The unencoded keyframe header
			writeHeader1(out, pic.getWidth(), pic.getHeight());
		} else {
			// Unencoded frame header spacing for intraframe
			out.put((byte)0);
			out.put((byte)0);
			out.put((byte)0);
		}
		int start = out.position();
		VPXBooleanEncoder boolEnc = new VPXBooleanEncoder(out);
		int headersEnd;

		if(isKeyFrame) {
			writeHeader2(boolEnc, qp);

			// MB modes
			for (int mbY = 0; mbY < mbHeight; mbY++) {
				for (int mbX = 0; mbX < mbWidth; mbX++) {
					// Luma mode DC
					boolEnc.writeBit(145, 1);
					boolEnc.writeBit(156, 0);
					boolEnc.writeBit(163, 0);

					// Chroma mode DC
					boolEnc.writeBit(142, 0);
				}
			}
			boolEnc.stop();
			headersEnd = out.position();
			boolEnc = new VPXBooleanEncoder(out);

			Picture outMB = Picture.create(16, 16, ColorSpace.YUV420);
			// MB residuals
			for (int mbY = 0; mbY < mbHeight; mbY++) {
				initValue(leftRow, 129);

				for (int mbX = 0; mbX < mbWidth; mbX++) {
					luma(pic, mbX, mbY, boolEnc, qp, outMB);
					chroma(pic, mbX, mbY, boolEnc, qp, outMB);

					collectPredictors(outMB, mbX);
				}
			}
		} else { // Inter frame handling

			// FRAME HEADER
			writeBoolean(boolEnc, 0); // segmentation disabled === OK
			writeBoolean(boolEnc, 0); // filter type - NORMAL LOOPFILTER === OK
			writeInt(boolEnc, 15, 6); // filter level === OK
			writeInt(boolEnc, 0, 3); // sharpness level === OK
			writeBoolean(boolEnc,1); // reference frame based loopfilter deltas === OK
			writeBoolean(boolEnc,0); // No need to update the loopfilter deltas === OK
			writeInt(boolEnc, 0, 2); // partition count === OK
			writeInt(boolEnc, 127, 7); // Default quant index === QP=127 
			writeBoolean(boolEnc, 0); // ydc quant index not present === OK
			writeBoolean(boolEnc, 0); // y2dc quant index not present === OK
			writeBoolean(boolEnc, 0); // y2ac quant index not present === OK
			writeBoolean(boolEnc, 0); // uvdc quant index not present === OK
			writeBoolean(boolEnc, 0); // uvac quant index not present === OK
			
			// INTRA FRAME SPECIFIC DETAILS
			writeBoolean(boolEnc, 0); // Do not refresh the golden frame === OK
			writeBoolean(boolEnc, 0); // Do not refresh the alt frame === OK
			writeInt(boolEnc, 0, 2); // Do not copy anything to golden frame === OK
			writeInt(boolEnc, 0, 2); // Do not copy anything to alt frame === OK
			writeBoolean(boolEnc, 0); // golden sign bias === OK
			writeBoolean(boolEnc, 0); // alt sign bias === OK
			writeBoolean(boolEnc, 1); // Do not refresh entryopy probabilities  === this is 1 for the 89 byte one
			writeBoolean(boolEnc, 0); // Refresh the last frame with this  === OK
			for (int i = 0; i < VP8Util.BLOCK_TYPES; i++)
				for (int j = 0; j < VP8Util.COEF_BANDS; j++)
					for (int k = 0; k < VP8Util.PREV_COEF_CONTEXTS; k++)
						for (int l = 0; l < VP8Util.MAX_ENTROPY_TOKENS - 1; l++) {
							boolEnc.writeBit(vp8CoefUpdateProbs[i][j][k][l], 0);// No token prob update necessary
						}
			writeBoolean(boolEnc, 1); // Change skip coefficient
			final int probToSkip = 1;
			writeInt(boolEnc, probToSkip, 8); // Probability to decode skip coefficient
			final int probForIntra = 1;
			writeInt(boolEnc, probForIntra, 8); // Probability to decode intra frame
			final int probForLast = 1;
			writeInt(boolEnc, probForLast, 8); // Probability to decode last frame
			final int probForGolden = 255;
			writeInt(boolEnc, probForGolden, 8); // Probability to decode golden frame
			writeBoolean(boolEnc, 0); // Intra probability update unnecessary
			writeBoolean(boolEnc, 0); // Chroma probability update unnecessary
			for (int[] probs : VPXConst.mv_probability_update) {
				for (int prob : probs) {
					boolEnc.writeBit(prob, 0); // no need to update this motion vector probability
				}
			}

			// MACRO BLOCK Headers!
			for (int mbY = 0; mbY < mbHeight; mbY++) {
				for (int mbX = 0; mbX < mbWidth; mbX++) {
					boolEnc.writeBit(probToSkip, 1); // No coefficients disclosed in this mb, they are skipped
					boolEnc.writeBit(probForIntra, 1); // This is an inter frame
					boolEnc.writeBit(probForLast, 1); // The reference is not the last frame
					boolEnc.writeBit(probForGolden, 0); // The reference is the golden frame (i.e., the previous
														// keyframe)
					boolEnc.writeBit(VPXConst.mode_contexts[mbY==0?(mbX==0?0:2):(mbX==0?2:5)][0], 0); // No motion vector update needed at all
					
				}
			}
			boolEnc.stop();
			headersEnd=out.position();
			// No tokens to encode
			boolEnc = new VPXBooleanEncoder(out);
//			writeInt(boolEnc,0,17); // padding to allow empty macroblock row decoding
		}
		boolEnc.stop();
		out.flip();
		// finalize the header
		writeHeader(out.duplicate(), headersEnd-start, isKeyFrame);

		return new EncodedFrame(out, isKeyFrame);
	}

	private void initValue(int[][] leftRow2, int val) {
		Arrays.fill(leftRow2[0], val);
		Arrays.fill(leftRow2[1], val);
		Arrays.fill(leftRow2[2], val);
	}

	private void writeHeader(ByteBuffer duplicate, int firstPart, boolean isKeyFrame) {
		int version = 0, type = isKeyFrame ? 0 : 1, showFrame = 1;
		int header = (firstPart << 5) | (showFrame << 4) | (version << 1) | type;

		duplicate.put((byte) (header & 0xff));
		duplicate.put((byte) ((header >> 8) & 0xff));
		duplicate.put((byte) ((header >> 16) & 0xff));
	}

	private void writeHeader2(VPXBooleanEncoder boolEnc, int qp) {
		boolEnc.writeBit(128, 0); // clr_type
		boolEnc.writeBit(128, 0); // clamp_type
		boolEnc.writeBit(128, 0); // segmentation enabled
		boolEnc.writeBit(128, 0); // filter type
		writeInt(boolEnc, 1, 6); // filter level
		writeInt(boolEnc, 0, 3); // sharpness level
		boolEnc.writeBit(128, 0); // deltas enabled
		writeInt(boolEnc, 0, 2); // partition type
		writeInt(boolEnc, qp, 7);
		boolEnc.writeBit(128, 0); // y1dc_delta_q
		boolEnc.writeBit(128, 0); // y2dc_delta_q
		boolEnc.writeBit(128, 0); // y2ac_delta_q
		boolEnc.writeBit(128, 0); // uvdc_delta_q
		boolEnc.writeBit(128, 0); // uvac_delta_q
		boolEnc.writeBit(128, 0); // refresh entropy probs

		int[][][][] probFlags = VPXConst.tokenProbUpdateFlagProbs;
		for (int i = 0; i < probFlags.length; i++) {
			for (int j = 0; j < probFlags[i].length; j++) {
				for (int k = 0; k < probFlags[i][j].length; k++) {
					for (int l = 0; l < probFlags[i][j][k].length; l++)
						boolEnc.writeBit(probFlags[i][j][k][l], 0);
				}
			}
		}

		boolEnc.writeBit(128, 0); // mb_no_coeff_skip
	}

	void writeInt(VPXBooleanEncoder boolEnc, int data, int bits) {
		int bit;

		for (bit = bits - 1; bit >= 0; bit--)
			boolEnc.writeBit(128, (1 & (data >> bit)));
	}

	void writeBoolean(VPXBooleanEncoder boolEnc, int bit) {
		boolEnc.writeBit(128, bit);
	}

	private void writeHeader1(ByteBuffer out, int width, int height) {
		NIOUtils.skip(out, 3);

		out.put((byte) 0x9d);
		out.put((byte) 0x01);
		out.put((byte) 0x2a);

		out.putShort((short) width);
		out.putShort((short) height);
	}

	private void collectPredictors(final Picture outMB, final int mbX) {
		final int mbX3 = mbX << 3;
		arrayCopySafe(outMB.getPlaneData(0), 240, topLine[0], mbX << 4, 16);
		arrayCopySafe(outMB.getPlaneData(1), 56, topLine[1], mbX3, 8);
		arrayCopySafe(outMB.getPlaneData(2), 56, topLine[2], mbX3, 8);

		copyCol(outMB.getPlaneData(0), 15, 16, leftRow[0]);
		copyCol(outMB.getPlaneData(1), 7, 8, leftRow[1]);
		copyCol(outMB.getPlaneData(2), 7, 8, leftRow[2]);
	}

	private void arrayCopySafe(byte[] src, int srcIndex, int[] dest, int destIndex, int length) {
		for (int i = 0; i < length; i++) {
			dest[destIndex + i] = src[srcIndex + i] + INT_TO_BYTE_OFFSET;
		}
	}

	private void copyCol(byte[] planeData, int off, int stride, int[] out) {

		for (int i = 0; i < out.length; i++) {
			out[i] = planeData[off] + INT_TO_BYTE_OFFSET;
			off += stride;
		}
	}

	private void luma(Picture pic, int mbX, int mbY, VPXBooleanEncoder out, int qp, Picture outMB) {
		int x = mbX << 4;
		int y = mbY << 4;
		int[][] ac = transform(pic, 0, qp, x, y);
		int[] dc = extractDC(ac);
		writeLumaDC(mbX, mbY, out, qp, dc);
		writeLumaAC(mbX, mbY, out, ac, qp);

		restorePlaneLuma(dc, ac, qp);
		putLuma(outMB.getPlaneData(0), lumaDCPred(x, y), ac, 4);
	}

	private void writeLumaAC(int mbX, int mbY, VPXBooleanEncoder out, int[][] ac, int qp) {
		for (int i = 0; i < 16; i++) {
			quantizer.quantizeY(ac[i], qp);
			bitstream.encodeCoeffsDCT15(out, zigzag(ac[i], tmp), mbX, i & 3, i >> 2);
		}
	}

	private void writeLumaDC(int mbX, int mbY, VPXBooleanEncoder out, int qp, int[] dc) {
		VPXDCT.walsh4x4(dc);
		quantizer.quantizeY2(dc, qp);
		bitstream.encodeCoeffsWHT(out, zigzag(dc, tmp), mbX);
	}

	private void writeChroma(int comp, int mbX, int mbY, VPXBooleanEncoder boolEnc, int[][] ac, int qp) {
		for (int i = 0; i < 4; i++) {
			quantizer.quantizeUV(ac[i], qp);
			bitstream.encodeCoeffsDCTUV(boolEnc, zigzag(ac[i], tmp), comp, mbX, i & 1, i >> 1);
		}
	}

	private int[] zigzag(int[] zz, int[] tmp2) {
		for (int i = 0; i < 16; i++)
			tmp2[i] = zz[VPXConst.zigzag[i]];
		return tmp2;
	}

	private void chroma(Picture pic, int mbX, int mbY, VPXBooleanEncoder boolEnc, int qp, Picture outMB) {
		final int x = mbX << 3;
		final int y = mbY << 3;
		int chromaPred1 = chromaPredBlk(1, x, y);
		int chromaPred2 = chromaPredBlk(2, x, y);

		int[][] ac1 = transformChroma(pic, 1, qp, x, y, outMB, chromaPred1);
		int[][] ac2 = transformChroma(pic, 2, qp, x, y, outMB, chromaPred2);

		writeChroma(1, mbX, mbY, boolEnc, ac1, qp);
		writeChroma(2, mbX, mbY, boolEnc, ac2, qp);

		restorePlaneChroma(ac1, qp);
		putChroma(outMB.getData()[1], 1, x, y, ac1, chromaPred1);
		restorePlaneChroma(ac2, qp);
		putChroma(outMB.getData()[2], 2, x, y, ac2, chromaPred2);
	}

	private int[][] transformChroma(Picture pic, int comp, int qp, int x, int y, Picture outMB, int chromaPred) {
		int[][] ac = new int[4][16];

		for (int blk = 0; blk < ac.length; blk++) {

			int blkOffX = (blk & 1) << 2;
			int blkOffY = (blk >> 1) << 2;
			takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + blkOffX,
					y + blkOffY, ac[blk], chromaPred);
			VPXDCT.fdct4x4(ac[blk]);
		}

		return ac;
	}

	private void putChroma(byte[] mb, int comp, int x, int y, int[][] ac, int chromaPred) {
		for (int blk = 0; blk < 4; blk++)
			putBlk(mb, chromaPred, ac[blk], 3, (blk & 1) << 2, (blk >> 1) << 2);
	}

	private final int chromaPredOne(int[] pix, int x) {
		return (pix[x] + pix[x + 1] + pix[x + 2] + pix[x + 3] + pix[x + 4] + pix[x + 5] + pix[x + 6] + pix[x + 7]
				+ 4) >> 3;
	}

	private final int chromaPredTwo(int[] pix1, int[] pix2, int x, int y) {
		return (pix1[x] + pix1[x + 1] + pix1[x + 2] + pix1[x + 3] + pix1[x + 4] + pix1[x + 5] + pix1[x + 6]
				+ pix1[x + 7] + pix2[y] + pix2[y + 1] + pix2[y + 2] + pix2[y + 3] + pix2[y + 4] + pix2[y + 5]
				+ pix2[y + 6] + pix2[y + 7] + 8) >> 4;
	}

	private int chromaPredBlk(int comp, int x, int y) {
		int predY = y & 0x7;
		if (x != 0 && y != 0)
			return chromaPredTwo(leftRow[comp], topLine[comp], predY, x);
		else if (x != 0)
			return chromaPredOne(leftRow[comp], predY);
		else if (y != 0)
			return chromaPredOne(topLine[comp], x);
		else
			return 128;
	}

	private void putLuma(byte[] planeData, int pred, int[][] ac, int log2stride) {
		for (int blk = 0; blk < ac.length; blk++) {
			int blkOffX = (blk & 3) << 2;
			int blkOffY = blk & ~3;
			putBlk(planeData, pred, ac[blk], log2stride, blkOffX, blkOffY);
		}
	}

	private void putBlk(byte[] planeData, int pred, int[] block, int log2stride, int blkX, int blkY) {
		int stride = 1 << log2stride;

		for (int line = 0, srcOff = 0, dstOff = (blkY << log2stride) + blkX; line < 4; line++) {
			planeData[dstOff] = (byte) (clip(block[srcOff] + pred, 0, 255) - INT_TO_BYTE_OFFSET);
			planeData[dstOff + 1] = (byte) (clip(block[srcOff + 1] + pred, 0, 255) - INT_TO_BYTE_OFFSET);
			planeData[dstOff + 2] = (byte) (clip(block[srcOff + 2] + pred, 0, 255) - INT_TO_BYTE_OFFSET);
			planeData[dstOff + 3] = (byte) (clip(block[srcOff + 3] + pred, 0, 255) - INT_TO_BYTE_OFFSET);
			srcOff += 4;
			dstOff += stride;
		}
	}

	private void restorePlaneChroma(int[][] ac, int qp) {
		for (int i = 0; i < 4; i++) {
			quantizer.dequantizeUV(ac[i], qp);
			VPXDCT.idct4x4(ac[i]);
		}
	}

	private void restorePlaneLuma(int[] dc, int[][] ac, int qp) {
		quantizer.dequantizeY2(dc, qp);
		VPXDCT.iwalsh4x4(dc);
		for (int i = 0; i < 16; i++) {
			quantizer.dequantizeY(ac[i], qp);
			ac[i][0] = dc[i];
			VPXDCT.idct4x4(ac[i]);
		}
	}

	private int[] extractDC(int[][] ac) {
		int[] dc = new int[ac.length];
		for (int i = 0; i < ac.length; i++) {
			dc[i] = ac[i][0];
		}
		return dc;
	}

	private int lumaDCPred(int x, int y) {
		if (x == 0 && y == 0)
			return 128;

		if (y == 0)
			return (ArrayUtil.sumInt(leftRow[0]) + 8) >> 4;
		if (x == 0)
			return (ArrayUtil.sumInt3(topLine[0], x, 16) + 8) >> 4;

		return (ArrayUtil.sumInt(leftRow[0]) + ArrayUtil.sumInt3(topLine[0], x, 16) + 16) >> 5;
	}

	private int[][] transform(Picture pic, int comp, int qp, int x, int y) {
		int dcc = lumaDCPred(x, y);

		int[][] ac = new int[16][16];
		for (int i = 0; i < ac.length; i++) {
			int[] coeff = ac[i];
			int blkOffX = (i & 3) << 2;
			int blkOffY = i & ~3;
			takeSubtract(pic.getPlaneData(comp), pic.getPlaneWidth(comp), pic.getPlaneHeight(comp), x + blkOffX,
					y + blkOffY, coeff, dcc);
			VPXDCT.fdct4x4(coeff);
		}
		return ac;
	}

	private final void takeSubtract(byte[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
			int dc) {
		final int adj = INT_TO_BYTE_OFFSET - dc;
		if (x + 4 < planeWidth && y + 4 < planeHeight)
			takeSubtractSafe(planeData, planeWidth, planeHeight, x, y, coeff, adj);
		else
			takeSubtractUnsafe(planeData, planeWidth, planeHeight, x, y, coeff, adj);

	}

	private final void takeSubtractSafe(byte[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
			final int adj) {
		final int planeWadj = planeWidth - 4;
		for (int i = 0, srcOff = y * planeWidth + x, dstOff = 0; i < 4; i++, srcOff += planeWadj) {
			for (int j = 0; j < 4; j++) {
				coeff[dstOff++] = planeData[srcOff++] + adj;
			}
		}
	}

	private final void takeSubtractUnsafe(byte[] planeData, int planeWidth, int planeHeight, int x, int y, int[] coeff,
			final int adj) {
		int outOff = 0;
		int i;
		final int yShift = y + 4;
		final int xShift = x + 4;
		for (i = y; i < Math.min(yShift, planeHeight); i++) {
			int off = i * planeWidth + Math.min(x, planeWidth);
			int j;
			for (j = x; j < Math.min(xShift, planeWidth); j++)
				coeff[outOff++] = planeData[off++] + adj;
			--off;
			for (; j < xShift; j++)
				coeff[outOff++] = planeData[off] + adj;
		}
		for (; i < yShift; i++) {
			int off = planeHeight * planeWidth - planeWidth + Math.min(x, planeWidth);
			int j;
			for (j = x; j < Math.min(xShift, planeWidth); j++)
				coeff[outOff++] = planeData[off++] + adj;
			--off;
			for (; j < xShift; j++)
				coeff[outOff++] = planeData[off] + adj;
		}
	}

	@Override
	public ColorSpace[] getSupportedColorSpaces() {
		return supportedColorSpaces;
	}

	@Override
	public int estimateBufferSize(Picture frame) {
		return (frame.getWidth() * frame.getHeight()) >> 1;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}
}
