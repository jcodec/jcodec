package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.JAVCTestCase;
import org.jcodec.codecs.h264.decode.aso.FlatMBlockMapper;
import org.jcodec.codecs.h264.decode.model.DecodedChroma;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.NearbyMotionVectors;
import org.jcodec.codecs.h264.decode.model.NearbyPixels;
import org.jcodec.codecs.h264.io.model.CodedSlice;
import org.jcodec.codecs.h264.io.model.InterPrediction;
import org.jcodec.codecs.h264.io.model.MBlockInter;
import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;
import org.jcodec.codecs.h264.io.model.Macroblock;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.codecs.h264.io.model.MBlockInter.Type;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;

public class TestSliceDecoder extends JAVCTestCase {

	public void testQp() throws Exception {

		final int[] actual = new int[3];

		MBlockDecoderI4x4 mBlockDecoderI4x4 = new MBlockDecoderI4x4(new int[] {
				0, 0 }, 8, 8) {
			private int i;

			public DecodedMBlock decodeINxN(MBlockIntraNxN coded, int qp,
					NearbyPixels neighbours) {
				actual[i++] = qp;

				return new DecodedMBlock(null, new DecodedChroma(null, null, 0,
						0), actual[i - 1], null, null, null);
			}

		};

		SliceDecoder sliceDecoder = new SliceDecoder(26, 3, mBlockDecoderI4x4,
				null, null);

		Macroblock[] mblocks = new Macroblock[] {
				new MBlockIntraNxN(-2, null, null, null, null),
				new MBlockIntraNxN(2, null, null, null, null),
				new MBlockIntraNxN(0, null, null, null, null) };

		SliceHeader sliceHeader = new SliceHeader();
		sliceHeader.slice_qp_delta = 2;

		CodedSlice slice = new CodedSlice(sliceHeader, mblocks);

		sliceDecoder.decodeSlice(slice, null, new FlatMBlockMapper(3, 0));

		assertArrayEquals(new int[] { 26, 28, 28 }, actual);
	}

	public void testMVPredWithLeftPSkip() throws Exception {
		final Vector[] actual = new Vector[1];

		MBlockDecoderInter mBlockDecoderInter = new MBlockDecoderInter(
				new int[] { 0, 0 }) {
			private int i = 0;

			@Override
			public DecodedMBlock decodeP(Picture[] reference, MBlockInter mb,
					NearbyMotionVectors nearMV, Point origin, int qp) {

				InterPrediction prediction = mb.getPrediction();
				Vector[] decodedMVsL0;
				if (prediction != null)
					decodedMVsL0 = prediction.getDecodedMVsL0();
				else
					decodedMVsL0 = new Vector[] { new Vector(0, 0, 0) };

				++i;

				return new DecodedMBlock(null, new DecodedChroma(null, null, 0,
						0), qp, calcForInter16x16(decodedMVsL0), reference, mb);
			}

			@Override
			public DecodedMBlock decodePSkip(Picture[] reference,
					NearbyMotionVectors nearMV, Point origin, int qp) {
				if (i == 48) {
					actual[0] = nearMV.getA()[0];
				}
				++i;
				return super.decodePSkip(reference, nearMV, origin, qp);
			}

		};

		SliceDecoder sliceDecoder = new SliceDecoder(26, 40, null, null,
				mBlockDecoderInter);

		Macroblock[] mblocks = new Macroblock[49];

		for (int i = 0; i < 7; i++) {
			mblocks[i] = new MBlockInter(0, null, null, null, null,
					Type.MB_16x16);
		}
		mblocks[7] = new MBlockInter(0, null, null, null, new InterPrediction(
				new int[] { 0 }, null, new Vector[] { new Vector(38, 14, 0) },
				null), Type.MB_16x16);
		mblocks[8] = new MBlockInter(0, null, null, null, new InterPrediction(
				new int[] { 0 }, null, new Vector[] { new Vector(38, 17, 0) },
				null), Type.MB_16x16);
		mblocks[9] = new MBlockInter(0, null, null, null, new InterPrediction(
				new int[] { 0 }, null, new Vector[] { new Vector(38, 17, 0) },
				null), Type.MB_16x16);
		for (int i = 10; i < 47; i++) {
			mblocks[i] = mblocks[i] = new MBlockInter(0, null, null, null,
					null, Type.MB_16x16);
		}
		mblocks[46] = new MBlockInter(0, null, null, null, new InterPrediction(
				new int[] { 0 }, null, new Vector[] { new Vector(38, 18, 0) },
				null), Type.MB_16x16);
		mblocks[47] = null;
		mblocks[48] = null;

		SliceHeader sliceHeader = new SliceHeader();
		sliceHeader.slice_qp_delta = 2;
		CodedSlice slice = new CodedSlice(sliceHeader, mblocks);

		int lumaW = 640;
		int lumaH = 32;
		int chromaW = lumaW / 2;
		int chromaH = lumaH / 2;

		int lumaRefW = (lumaW + 32) * 4;
		int lumaRefH = (lumaH + 32) * 4;
		int chromaRefW = chromaW * 8;
		int chromaRefH = chromaH * 8;

		int[] chroma = new int[chromaRefW * chromaRefH];
		Picture ref = new Picture(lumaRefW, lumaRefH, new int[][] {new int[lumaRefW
				* lumaRefH], chroma, chroma}, ColorSpace.YUV420);

		Picture[] refList = new Picture[] { ref };
		sliceDecoder.decodeSlice(slice, refList, new FlatMBlockMapper(40, 0));

		assertEquals(new Vector(38, 17, 0), actual[0]);
	}
}
