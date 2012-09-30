package org.jcodec.codecs.h264.decode;

import static org.jcodec.codecs.h264.io.model.SubMBType.L0_4x8;
import static org.jcodec.codecs.h264.io.model.SubMBType.L0_8x4;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.JAVCTestCase;
import org.jcodec.codecs.h264.decode.model.DecodedMBlock;
import org.jcodec.codecs.h264.decode.model.NearbyMotionVectors;
import org.jcodec.codecs.h264.io.model.CodedChroma;
import org.jcodec.codecs.h264.io.model.Inter8x8Prediction;
import org.jcodec.codecs.h264.io.model.InterPrediction;
import org.jcodec.codecs.h264.io.model.MBlockInter;
import org.jcodec.codecs.h264.io.model.MBlockInter8x8;
import org.jcodec.codecs.h264.io.model.ResidualBlock;
import org.jcodec.codecs.h264.io.model.SubMBType;
import org.jcodec.codecs.h264.io.model.Vector;
import org.jcodec.codecs.h264.io.model.MBlockInter.Type;
import org.jcodec.codecs.util.PGMIO;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Point;
import org.junit.Test;

public class TestMBlockDecoderInter extends JAVCTestCase {
	
	private int[] ref2 = new int[] {
		140, 112, 118, 178, 224, 227, 232, 233, 232, 232, 232, 231, 232, 233, 233, 234, 235, 235, 235, 236, 235, 235, 234, 234, 233, 233, 232, 232, 209, 202, 202, 209, 
		123, 153, 191, 225, 224, 227, 232, 233, 232, 232, 232, 233, 233, 234, 235, 235, 235, 235, 234, 234, 234, 233, 233, 233, 233, 233, 233, 233, 225, 213, 204, 207, 
		190, 221, 221, 223, 223, 227, 232, 233, 232, 232, 232, 233, 233, 234, 235, 235, 235, 235, 234, 234, 234, 233, 234, 235, 235, 236, 237, 237, 225, 213, 204, 207, 
		213, 219, 212, 222, 225, 227, 232, 233, 232, 232, 231, 231, 207, 207, 207, 203, 194, 189, 188, 188, 188, 187, 188, 189, 191, 191, 193, 193, 206, 199, 199, 206, 
		219, 218, 219, 222, 226, 236, 233, 222, 200, 192, 176, 168, 144, 136, 119, 111, 108, 108, 108, 109, 108, 107, 109, 118, 131, 142, 159, 166, 173, 174, 174, 175, 
		219, 219, 221, 220, 223, 222, 203, 189, 174, 162, 139, 127,  99,  96,  94,  93,  91,  91,  88,  88,  84,  79,  82,  91, 102, 107, 122, 132, 158, 177, 186, 177, 
		219, 217, 219, 219, 214, 193, 162, 151, 132, 124, 107,  99, 103, 105, 107, 107, 107, 107, 104, 102,  98,  91,  93, 103,  85,  84, 105, 127, 130, 157, 184, 183, 
		219, 216, 219, 219, 177, 153, 126, 121, 119, 116, 114, 114, 122, 121, 120, 119, 117, 117, 114, 114, 110, 106, 108, 117, 102,  86,  87, 103, 115, 133, 168, 186, 
		217, 212, 214, 170, 117, 127, 127, 117, 110, 113, 118, 118, 116, 111, 106, 107, 111, 113, 112, 112, 110, 107, 102, 100, 113, 103,  93,  93, 119, 129, 168, 168, 
		203, 205, 185, 127, 127, 127, 117, 107, 112, 110, 105, 100,  85,  85,  90,  93, 114, 114, 114, 114, 112, 105,  91,  85,  79,  93, 104, 100, 116, 125, 167, 185, 
		212, 205, 163, 118, 127, 117, 107, 109, 112, 112, 105, 101,  99, 100, 105, 108, 113, 114, 113, 115, 112, 106,  93,  86,  74,  74,  90, 107, 128, 155, 185, 203, 
		210, 189, 144, 127, 117, 107, 109, 117, 114, 116, 118, 120, 119, 116, 111, 111, 112, 115, 113, 113, 112, 107, 103, 101, 102, 107, 108, 105, 141, 186, 202, 202, 
		167, 187, 156, 130, 103,  87, 110, 128, 119, 125, 118, 103, 109, 102, 103, 112, 111, 115, 113, 106, 113, 107, 102, 105, 101, 109, 114, 132, 193, 202, 207, 201, 
		145, 165, 174, 138, 103,  87, 112, 128, 121, 123, 113, 103,  97,  88,  87,  94, 105, 114, 117, 112, 119, 101,  85,  86,  89,  98, 111, 139, 194, 202, 208, 203, 
		108, 104, 145, 165, 103,  86, 113, 128, 122, 113, 103, 103,  99,  87,  78,  82,  95, 109, 124, 125, 101,  78,  67,  80,  86,  86,  99, 136, 195, 204, 209, 204, 
		100, 106, 107, 127, 103,  86, 113, 128, 122, 113,  98, 103,  87,  73,  62,  64,  89, 107, 126, 129, 106,  72,  50,  62,  93,  84,  88, 125, 195, 204, 208, 204, 
		111, 109, 111, 111,  86,  84, 114, 129, 123, 114,  92,  82,  74,  67,  67,  74, 103, 110, 126, 132, 118,  81,  62,  57,  62,  79,  92, 141, 195, 198, 208, 201, 
		111, 110, 111, 111, 100,  83, 115, 138, 124, 116, 100,  92,  92,  85,  85,  92, 110, 116, 128, 134, 131, 100,  84,  76,  81,  95, 108, 145, 193, 208, 206, 211, 
		111, 110, 110, 111, 115,  95, 120, 139, 132, 127, 116, 111, 106,  99,  99, 106, 125, 128, 134, 136, 144, 125, 114, 100,  95, 104, 117, 130, 139, 183, 204, 202, 
		111, 111, 111, 112, 115, 108, 129, 139, 138, 135, 124, 121, 124, 117, 117, 124, 133, 134, 137, 139, 143, 130, 122, 105, 114, 119, 133, 135, 111, 125, 184, 207, 
		104, 106, 109, 111, 114, 113, 127, 138, 141, 140, 141, 140, 138, 133, 128, 127, 124, 124, 146, 164, 173, 144, 117, 119, 121, 126, 135, 138, 109, 102, 132, 171, 
		 94,  97, 104, 109, 114, 113, 127, 140, 141, 141, 141, 140, 138, 133, 128, 129, 132, 135, 158, 176, 185, 155, 119, 113, 121, 126, 135, 138,  97,  85,  98, 123, 
		 75,  81,  93,  99, 114, 113, 129, 141, 141, 141, 139, 138, 136, 131, 126, 127, 129, 131, 153, 172, 176, 155, 119, 104, 120, 124, 133, 137, 105,  94,  92,  97, 
		 66,  73,  88,  95, 114, 113, 130, 141, 141, 141, 138, 136, 134, 130, 125, 122, 116, 115, 137, 155, 155, 144, 117, 101, 120, 124, 132, 136, 100,  95,  93,  96, 
		 50,  51,  67,  82, 111, 122, 131, 137, 140, 140, 137, 134, 132, 128, 123, 121,  97,  87,  98, 120, 113, 103, 102, 111, 119, 122, 129, 132,  98,  96,  95,  95, 
		 74,  63,  55,  58, 104, 134, 130, 143, 139, 139, 136, 133, 131, 127, 123, 120, 114, 101,  97, 107, 103, 104, 115, 125, 120, 122, 126, 126,  97,  95,  94,  95, 
		 99,  82,  63,  60,  84, 127, 130, 137, 139, 139, 136, 134, 132, 132, 127, 125, 130, 120, 105, 100, 102, 115, 131, 132, 129, 126, 122, 120,  96,  95,  95,  95, 
		102,  90,  83,  86,  72, 107, 131, 120, 139, 138, 136, 135, 134, 134, 129, 128, 127, 125, 114, 106, 111, 125, 135, 131, 129, 126, 120, 118,  96,  96,  96,  96, 
		110, 108,  99,  94,  99,  80,  76, 113, 140, 139, 137, 137, 145, 135, 130, 129, 126, 124, 113, 108, 113, 122, 125, 120, 124, 136, 141, 111,  95,  95,  95,  95, 
		115, 110, 101,  97,  99,  80,  76, 113, 139, 139, 138, 137, 135, 120, 103, 101,  96,  94,  92,  92,  93,  85,  82,  88, 113, 129, 143, 117,  94,  94,  94,  94, 
		119, 115, 106, 101,  99,  80,  76, 113, 139, 139, 139, 139, 129, 108,  79,  72,  87,  92, 103, 108,  83,  78,  80,  89, 108, 125, 138, 113,  90,  90,  90,  90, 
		121, 117, 108, 103,  99,  80,  76, 113, 139, 139, 139, 139, 125, 118, 116, 122, 144, 136, 120, 111, 115, 130, 144, 145, 115, 127, 132, 102,  87,  87,  87,  87 
	};
	
	private int[] ref1 = new int[] {
		148, 117, 119, 176, 225, 228, 233, 233, 233, 232, 232, 231, 232, 232, 232, 232, 232, 232, 233, 234, 235, 236, 235, 235, 234, 233, 232, 232, 212, 204, 204, 212, 
		123, 150, 184, 215, 225, 228, 233, 233, 233, 232, 232, 233, 233, 234, 234, 234, 234, 234, 234, 234, 235, 235, 235, 235, 235, 234, 234, 234, 222, 209, 199, 202, 
		190, 219, 215, 208, 225, 228, 233, 233, 233, 232, 232, 233, 233, 235, 236, 236, 237, 237, 237, 237, 237, 236, 237, 237, 238, 238, 239, 239, 222, 209, 197, 202, 
		221, 221, 214, 223, 226, 228, 233, 233, 233, 232, 231, 231, 207, 207, 207, 203, 196, 192, 191, 190, 189, 188, 189, 189, 190, 191, 192, 192, 212, 204, 200, 212, 
		216, 219, 214, 216, 226, 236, 233, 222, 206, 191, 178, 163, 149, 137, 125, 124, 103, 106, 113, 113, 105,  99, 102, 112, 135, 138, 155, 171, 171, 184, 189, 185, 
		216, 218, 216, 219, 223, 222, 203, 189, 178, 158, 137, 127, 101, 100,  99,  96,  90,  87,  88,  88,  85,  77,  80,  91,  90, 110, 124, 140, 151, 174, 187, 188, 
		216, 215, 216, 216, 214, 193, 162, 151, 140, 129, 117, 112, 102, 102, 102, 103, 106, 107, 103, 101,  97,  92,  95, 106,  79,  84,  96, 124, 131, 156, 179, 192, 
		216, 214, 216, 216, 177, 153, 126, 120, 114, 115, 120, 123, 125, 125, 125, 124, 122, 120, 113, 110, 106, 103, 106, 116, 110,  91,  87, 105, 112, 133, 158, 181, 
		221, 212, 208, 172, 117, 127, 127, 117, 112, 115, 120, 120, 111, 107, 105, 106, 108, 110, 111, 112, 111, 110, 105, 103, 101,  97,  97, 102, 109, 123, 163, 169, 
		211, 208, 184, 131, 117, 122, 120, 117, 114, 112, 107, 104,  84,  89,  93,  99, 107, 111, 111, 111, 114, 107,  92,  84,  82,  97, 107, 102, 116, 131, 171, 194, 
		215, 204, 155, 117, 115, 112, 114, 118, 115, 114, 107, 102,  95,  98, 102, 104, 110, 111, 111, 111, 114, 107,  92,  84,  82,  77,  87, 102, 124, 161, 189, 208, 
		212, 184, 136, 128, 113, 107, 107, 121, 115, 118, 120, 120, 120, 119, 118, 116, 113, 111, 110, 111, 114, 110, 105, 102, 102, 105, 107, 102, 136, 188, 198, 197, 
		170, 186, 146, 123, 106,  86, 111, 123, 117, 121, 121, 111, 109, 102, 103, 112, 114, 112, 111, 111, 116, 109,  99, 101, 104, 110, 110, 126, 182, 199, 201, 201, 
		138, 160, 170, 132, 104,  86, 112, 125, 120, 120, 111, 106,  97,  88,  87,  94, 111, 115, 116, 113, 114,  99,  91,  93,  95, 100, 107, 136, 195, 201, 201, 201, 
		114, 105, 148, 157, 102,  86, 112, 127, 121, 112, 100,  99, 102,  90,  81,  85, 103, 116, 119, 120, 104,  79,  69,  84,  88,  88,  95, 133, 199, 201, 202, 201, 
		103, 105, 110, 116, 102,  84, 112, 127, 121, 107, 100, 100,  96,  82,  71,  73,  93, 105, 120, 125,  99,  69,  59,  79,  90,  82,  83, 121, 198, 203, 202, 201, 
		111, 110, 111, 111,  81,  80, 113, 128, 122, 106,  91,  84,  73,  65,  65,  73,  95, 106, 124, 129, 123,  84,  55,  61,  59,  66,  90, 131, 194, 208, 201, 192, 
		111, 111, 111, 111,  97,  77, 113, 138, 123, 115, 101,  94,  93,  85,  85,  93, 102, 112, 126, 131, 137, 107,  81,  71,  83,  93, 114, 140, 189, 209, 200, 213, 
		111, 111, 109, 109, 113,  90, 118, 140, 132, 126, 120, 112, 106,  98,  98, 106, 114, 122, 132, 134, 146, 123, 107, 100,  96, 110, 119, 129, 133, 185, 214, 206, 
		111, 111, 109, 109, 115, 105, 128, 138, 141, 135, 131, 124, 128, 120, 120, 128, 130, 133, 138, 140, 150, 130, 121, 122, 121, 119, 127, 136, 106, 122, 188, 205, 
		106, 106, 106, 106, 115, 109, 132, 138, 142, 139, 140, 138, 134, 130, 128, 128, 128, 130, 148, 169, 182, 147, 117, 119, 125, 124, 131, 138, 105, 105, 127, 170, 
		 98,  99, 101, 103, 116, 109, 132, 139, 144, 141, 142, 142, 139, 132, 128, 129, 134, 136, 156, 173, 185, 149, 122, 115, 125, 129, 132, 136,  94,  93, 107, 113, 
		 82,  86,  94,  98, 117, 109, 131, 140, 143, 142, 141, 140, 138, 132, 127, 128, 131, 133, 154, 165, 172, 152, 119, 105, 120, 126, 136, 136, 106,  95,  93,  99, 
		 74,  79,  91,  96, 117, 109, 131, 140, 143, 143, 141, 140, 137, 133, 127, 126, 125, 126, 142, 149, 153, 154, 127, 102, 114, 123, 133, 138, 106,  95,  92,  97, 
		 54,  54,  72,  89, 109, 132, 130, 142, 141, 141, 139, 138, 136, 132, 127, 121, 100,  91,  99, 119, 124, 117, 114, 112, 114, 121, 131, 140, 104,  95,  92,  94, 
		 77,  64,  56,  59,  94, 130, 132, 135, 139, 140, 138, 137, 135, 131, 126, 118, 107,  99,  99, 109, 109, 104, 115, 120, 120, 119, 121, 131,  99,  95,  93,  93, 
		 99,  81,  59,  57,  82, 128, 133, 135, 138, 139, 138, 138, 134, 131, 127, 125, 120, 117, 109, 101, 104, 115, 126, 128, 128, 119, 115, 121,  98,  96,  94,  93, 
		101,  89,  79,  84,  85, 118, 125, 128, 137, 138, 137, 138, 133, 130, 129, 128, 128, 128, 114, 100, 111, 129, 136, 133, 136, 128, 119, 112,  97,  98,  96,  94, 
		105, 103, 100,  94,  87,  91,  92, 114, 138, 138, 138, 139, 142, 130, 126, 125, 123, 121, 109, 103, 114, 124, 128, 122, 121, 130, 127, 115,  96,  97,  96,  94, 
		110, 107, 102, 100,  93,  80,  72, 108, 137, 136, 139, 141, 133, 113,  98,  97,  96,  95,  94,  92,  92,  82,  79,  85, 112, 136, 134, 116,  91,  93,  95,  95, 
		115, 112, 107, 105,  98,  80,  65, 100, 136, 135, 141, 143, 131, 105,  76,  79,  87,  93, 104, 110,  90,  84,  87,  97, 105, 137, 136, 109,  86,  88,  90,  90, 
		117, 115, 110, 107, 100,  82,  72, 101, 134, 135, 141, 141, 130, 119, 120, 130, 143, 134, 116, 107, 112, 128, 144, 144, 121, 126, 123, 101,  83,  86,  88,  87 
	};

	@Test
	public void testP8x8() throws Exception {
		int[] expected = new int[] {
			89, 84, 87, 102, 111, 117, 129, 135, 102, 68, 63, 70, 104, 191, 204, 205,
			102, 98, 100, 116, 123, 127, 133, 139, 126, 99, 93, 95, 122, 191, 198, 205,
			121, 115, 120, 129, 133, 135, 136, 139, 135, 119, 109, 112, 138, 176, 208, 208,
			136, 130, 127, 126, 125, 131, 148, 162, 156, 121, 115, 128, 136, 124, 172, 204,
			136, 130, 128, 131, 122, 129, 153, 171, 178, 136, 117, 123, 137, 117, 117, 169,
			134, 128, 126, 129, 131, 138, 161, 177, 176, 141, 111, 117, 140, 117, 101, 122,
			132, 127, 124, 120, 119, 125, 149, 162, 163, 139, 108, 113, 140, 109, 85, 93,
			130, 125, 124, 111, 88, 86, 106, 121, 122, 112, 109, 118, 139, 114, 95, 92,
			129, 125, 122, 115, 101, 92, 103, 109, 102, 109, 111, 110, 132, 107, 95, 96,
			132, 129, 123, 127, 124, 109, 101, 101, 112, 124, 119, 115, 126, 104, 94, 95,
			134, 132, 127, 128, 128, 119, 107, 104, 120, 128, 129, 127, 122, 102, 95, 96,
			141, 132, 131, 129, 128, 122, 111, 110, 123, 127, 127, 131, 118, 102, 95, 97,
			131, 115, 114, 119, 121, 112, 103, 103, 94, 115, 110, 136, 121, 97, 95, 96,
			110, 82, 78, 83, 84, 88, 85, 72, 68, 61, 94, 130, 126, 97, 92, 94,
			109, 91, 87, 100, 79, 64, 60, 56, 69, 108, 101, 123, 118, 93, 88, 90,
			128, 138, 145, 157, 153, 146, 131, 121, 130, 144, 127, 125, 110, 89, 87, 87
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] {0, 0});

		Inter8x8Prediction prediction = new Inter8x8Prediction(new SubMBType[] {
				L0_4x8, L0_4x8, L0_8x4, L0_4x8 }, new int[] { 0 }, null,
				new Vector[][] {
						new Vector[] { new Vector(0, 0, 0),
								new Vector(-1, 0, 0) },
						new Vector[] { new Vector(-2, 0, 0),
								new Vector(7, -7, 0) },
						new Vector[] { new Vector(0, -1, 0),
								new Vector(1, -3, 0) },
						new Vector[] { new Vector(4, -2, 0),
								new Vector(5, 0, 0) } }, null);

		ResidualBlock cbDC = new ResidualBlock(new int[] { 0, 0, 0, 0 });
		ResidualBlock crDC = new ResidualBlock(new int[] { 0, 1, 0, 0 });

		ResidualBlock[] cbAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] crAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] luma = new ResidualBlock[] {
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-1, -1, -1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0}),
			new ResidualBlock(new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, -2, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-2, 1, 0, 4, 0, 0, 0, 0, 0, -3, 0, -1, 0, 0, 1, 0}),
			new ResidualBlock(new int[] {-1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-1, -2, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, -1, -1}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0})
		};
		
		CodedChroma chroma = new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);

		MBlockInter8x8 mb = new MBlockInter8x8(0, chroma, null, luma,
				prediction);

		int qp = 28;
		Picture reference = buildReferenceImage(ref2);

		NearbyMotionVectors nearMV = new NearbyMotionVectors(new Vector[] {
				new Vector(-15, 4, 0), new Vector(-15, 4, 0),
				new Vector(-13, 4, 0), new Vector(-13, 4, 0) }, new Vector[] {
				new Vector(-13, 4, 0), new Vector(-14, 3, 0),
				new Vector(-12, 1, 0), new Vector(-7, 6, 0) }, null, null,
				true, true, false, true);

		DecodedMBlock actual = decoder.decodeP8x8(
				new Picture[] { reference }, mb, nearMV,
				new Point(16, 16), qp);

		assertArrayEquals(expected, actual.getLuma());
	}

	@Test
	public void testP16x8() throws Exception {
		int[] expected = new int[] {
			125, 117, 119, 164, 212, 224, 226, 229, 232, 232, 232, 232, 233, 234, 236, 237,
			130, 157, 188, 215, 221, 222, 227, 229, 232, 232, 231, 233, 233, 232, 232, 231,
			197, 216, 220, 220, 220, 221, 227, 229, 234, 234, 235, 236, 225, 222, 216, 212,
			216, 218, 215, 220, 222, 225, 229, 228, 225, 224, 219, 219, 192, 177, 162, 152,
			215, 215, 216, 217, 226, 229, 232, 233, 216, 200, 179, 166, 156, 140, 123, 110,
			215, 215, 216, 215, 226, 232, 217, 194, 164, 154, 139, 128, 116, 106, 104, 103,
			215, 214, 214, 218, 221, 208, 172, 149, 134, 126, 121, 114, 103, 108, 113, 114,
			216, 212, 214, 208, 185, 153, 128, 122, 120, 116, 112, 109, 110, 113, 116, 118,
			212, 209, 203, 194, 129, 118, 132, 123, 111, 111, 114, 116, 111, 103,  95,  90,
			210, 212, 194, 154, 120, 130, 118, 105, 109, 113, 107, 102, 102,  92,  93,  94,
			213, 206, 157, 120, 123, 121,  98,  93, 112, 113, 111, 107, 123, 119, 115, 115,
			206, 199, 146, 122, 124, 102,  88,  97, 119, 116, 120, 118, 128, 125, 114, 111,
			157, 189, 166, 147, 117,  82,  72,  94, 123, 115, 128, 126, 122, 116,  98, 101,
			131, 135, 166, 170, 121,  77,  73,  94, 124, 119, 125, 124, 107, 100,  94,  97,
			104,  88, 123, 157, 121,  66,  71,  96, 125, 126, 124, 121, 103,  96,  90,  89,
			108, 107, 104, 109,  92,  63,  70,  96, 125, 132, 126, 114,  99,  82,  74,  69
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] { 0, 0 });

		InterPrediction prediction = new InterPrediction(new int[] { 0 }, null,
				new Vector[] { new Vector(-1, 1, 0), new Vector(-1, 0, 0) },
				null);

		ResidualBlock cbDC = new ResidualBlock(new int[] { 0, 1, 0, 1 });
		ResidualBlock crDC = new ResidualBlock(new int[] { 0, 0, 0, 0 });

		ResidualBlock[] cbAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] crAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] luma = new ResidualBlock[] {
			new ResidualBlock(new int[] {-1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {2, 0, 0, -1, -1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-2, 1, 2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {1, 0, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-1, 1, 1, 0, -1, 1, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-1, 1, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, -1, -1, 0, 0}),
			new ResidualBlock(new int[] {-5, 2, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {1, 1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {2, -2, -1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {2, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0})
		};
		
		CodedChroma chroma = new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);

		MBlockInter mb = new MBlockInter(0, chroma, null, luma, prediction,
				Type.MB_16x8);

		int qp = 28;
		Picture reference = buildReferenceImage(ref2);

		NearbyMotionVectors nearMV = new NearbyMotionVectors(new Vector[] {
				null, null, null, null },
				new Vector[] { null, null, null, null }, null, null, true,
				true, false, true);

		DecodedMBlock actual = decoder.decodeP16x8(
				new Picture[] { reference }, mb, nearMV, new Point(0, 0),
				qp);

		assertArrayEquals(expected, actual.getLuma());
	}
	
	@Test
	public void testP8x16() throws Exception {
		int[] expected = new int[] {
			86, 82, 87, 99, 110, 124, 133, 141, 96, 81, 74, 75, 104, 183, 209, 199,
			103, 97, 103, 111, 118, 129, 134, 145, 118, 94, 87, 98, 121, 198, 208, 203,
			124, 117, 123, 130, 127, 133, 134, 142, 133, 107, 105, 107, 132, 167, 205, 212,
			135, 131, 132, 134, 124, 132, 148, 168, 152, 123, 121, 121, 142, 118, 162, 198,
			135, 130, 128, 131, 124, 127, 148, 178, 172, 139, 110, 128, 146, 129, 107, 154,
			136, 129, 127, 131, 132, 136, 154, 175, 162, 137, 115, 125, 142, 121, 98, 117,
			135, 130, 126, 128, 127, 130, 145, 156, 147, 133, 116, 118, 137, 116, 89, 103,
			135, 130, 126, 115, 92, 86, 102, 123, 135, 125, 111, 113, 136, 120, 92, 94,
			134, 129, 124, 111, 92, 88, 99, 109, 111, 117, 121, 120, 141, 115, 96, 93,
			133, 129, 125, 120, 113, 108, 103, 101, 115, 123, 121, 118, 137, 111, 95, 93,
			131, 129, 128, 127, 131, 125, 107, 104, 117, 126, 126, 115, 126, 106, 94, 94,
			136, 128, 128, 127, 132, 125, 110, 112, 121, 135, 134, 119, 116, 102, 96, 96,
			134, 116, 112, 111, 112, 109, 104, 106, 109, 111, 119, 125, 116, 100, 97, 97,
			117, 86, 72, 79, 70, 77, 89, 81, 79, 73, 106, 119, 122, 99, 94, 97,
			110, 97, 98, 111, 82, 72, 64, 60, 63, 100, 98, 127, 121, 93, 89, 93,
			135, 131, 138, 153, 169, 151, 133, 132, 129, 143, 127, 122, 112, 88, 85, 88
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] { 0, 0 });

		InterPrediction prediction = new InterPrediction(new int[] { 0 }, null,
				new Vector[] { new Vector(-9, -2, 0), new Vector(9, -6, 0) },
				null);

		ResidualBlock cbDC = null;
		ResidualBlock crDC = null;

		ResidualBlock[] cbAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] crAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] luma = new ResidualBlock[] {
			new ResidualBlock(new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-2, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {4, 4, 0, -1, 0, 1, 1, 0, -1, -1, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {1, 0, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0}),
			new ResidualBlock(new int[] {2, 5, 1, 0, 0, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {1, 0, 1, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 2, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-2, 0, 0, 6, 0, 0, 0, 0, 0, -3, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-3, 0, -2, 2, -2, 0, -1, 1, 0, 0, 0, -1, 1, 0, 0, -1}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0})
		};
		
		CodedChroma chroma = new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);

		MBlockInter mb = new MBlockInter(0, chroma, null, luma, prediction,
				Type.MB_8x16);

		int qp = 28;
		Picture reference = buildReferenceImage(ref1);

		NearbyMotionVectors nearMV = new NearbyMotionVectors(new Vector[] {
				new Vector(-5, 5, 0), new Vector(-5, 5, 0),
				new Vector(-5, 5, 0), new Vector(-5, 5, 0) }, new Vector[] {
				new Vector(-14, 4, 0), new Vector(-14, 4, 0),
				new Vector(-6, 3, 0), new Vector(-6, 3, 0) }, null, null, true,
				true, false, true);

		DecodedMBlock actual = decoder.decodeP8x16(
				new Picture[] { reference }, mb, nearMV,
				new Point(16, 16), qp);

		assertArrayEquals(expected, actual.getLuma());
	}

	@Test
	public void testP16x16() throws Exception {
		int[] expected = new int[] {
			78, 77, 84, 99, 111, 126, 134, 139, 101, 68, 64, 71, 95, 185, 203, 204,
			98, 95, 101, 109, 117, 129, 135, 145, 123, 98, 88, 91, 129, 180, 190, 200,
			116, 112, 120, 127, 126, 132, 132, 141, 135, 120, 114, 107, 138, 170, 196, 194,
			133, 130, 133, 135, 126, 135, 146, 160, 154, 125, 118, 120, 137, 137, 173, 202,
			138, 134, 133, 135, 124, 132, 155, 181, 177, 137, 122, 127, 137, 118, 119, 154,
			137, 131, 131, 136, 133, 141, 158, 178, 169, 134, 116, 123, 138, 118, 101, 122,
			132, 126, 125, 129, 126, 134, 145, 157, 160, 134, 107, 117, 131, 120, 90, 101,
			129, 124, 120, 110, 90, 91, 105, 121, 132, 123, 111, 115, 125, 121, 100, 105,
			133, 128, 121, 104, 96, 94, 106, 111, 112, 114, 121, 122, 124, 116, 106, 98,
			132, 128, 123, 116, 115, 109, 103, 100, 109, 120, 127, 127, 122, 105, 97, 96,
			130, 129, 127, 126, 131, 120, 100, 98, 117, 129, 129, 132, 122, 103, 90, 99,
			134, 131, 130, 129, 135, 122, 103, 107, 125, 134, 130, 125, 122, 109, 92, 100,
			130, 119, 117, 115, 115, 108, 101, 107, 104, 109, 111, 127, 124, 98, 102, 98,
			114, 85, 78, 82, 72, 80, 87, 77, 64, 64, 103, 130, 126, 105, 101, 92,
			108, 90, 91, 104, 72, 69, 69, 61, 71, 112, 108, 128, 120, 98, 92, 87,
			130, 131, 141, 155, 164, 143, 128, 132, 123, 152, 134, 119, 117, 89, 87, 85
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] {0, 0});
		
		InterPrediction prediction = new InterPrediction(
				new int[] {0},
				null,
				new Vector[] {new Vector(-11, -1, 0)},
				null);
		
		ResidualBlock cbDC = new ResidualBlock(new int[] {0, 0, 0, 0});
		ResidualBlock crDC = new ResidualBlock(new int[] {0, 1, 0, 0});
		
		ResidualBlock[] cbAC = new ResidualBlock[] {
			null, null, null, null
		};
		
		ResidualBlock[] crAC = new ResidualBlock[] {
			null, null, null, null
		};

		ResidualBlock[] luma = new ResidualBlock[] {
			new ResidualBlock(new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-3, 0, 1, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {12, -3, 1, -1, 3, -2, -1, -4, 0, 1, 0, 0, 0, 0, -1, 0}),
			new ResidualBlock(new int[] {1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-2, 1, 2, 1, -2, 4, -2, 1, -1, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 0, 1, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-2, 0, 0, 5, 0, 0, 0, 0, 0, -3, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {-3, 1, 0, 0, 1, 2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
			new ResidualBlock(new int[] {0, -2, 2, 0, -1, 0, -1, 0, 2, -1, 0, 0, 1, 0, 0, -1}),
			new ResidualBlock(new int[] {-4, -1, 0, 0, 0, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0})
		};
		
		CodedChroma chroma = new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);

		MBlockInter mb = new MBlockInter(0, chroma, null, luma, prediction,
				Type.MB_16x16);

		int qp = 28;
		Picture reference = buildReferenceImage(ref1);

		NearbyMotionVectors nearMV = new NearbyMotionVectors(new Vector[] {
				new Vector(-1, 3, 0), new Vector(-1, 3, 0),
				new Vector(-1, 3, 0), new Vector(-1, 3, 0) }, new Vector[] {
				new Vector(-7, 3, 0), new Vector(-7, 3, 0),
				new Vector(-7, 3, 0), new Vector(-7, 3, 0) }, null, new Vector(
				-2, 1, 0), true, true, false, true);

		DecodedMBlock actual = decoder.decodeP16x16(
				new Picture[] { reference }, mb, nearMV,
				new Point(16, 16), qp);

		assertArrayEquals(expected, actual.getLuma());
	}
	
	private Picture buildReferenceImage(int[] luma) {
		Interpolator interpolator = new Interpolator();
	
		int[] interpolated = interpolator.interpolateLuma(luma, 32, 32);
		int[] chroma = new int[256*64];
	
		return new Picture(256, 256, new int[][] {interpolated, chroma, chroma}, ColorSpace.YUV420);
	}
	
	@Test
	public void testInter16x8MB38() throws Exception {
		int[] expected = new int[] {
			47, 48, 48, 49, 51, 53, 55, 56, 58, 59, 59, 61, 62, 63, 64, 65,
			47, 48, 48, 49, 51, 53, 55, 56, 58, 59, 59, 61, 62, 63, 64, 65, 
			47, 48, 49, 49, 51, 53, 55, 56, 58, 60, 60, 61, 63, 65, 67, 68, 
			47, 48, 50, 51, 53, 55, 56, 57, 59, 61, 61, 63, 66, 68, 71, 73, 
			48, 48, 50, 53, 55, 56, 57, 57, 59, 61, 64, 66, 68, 71, 74, 76, 
			49, 50, 52, 55, 58, 60, 61, 61, 63, 66, 68, 69, 72, 74, 76, 79, 
			52, 53, 55, 58, 62, 66, 67, 67, 69, 71, 73, 73, 76, 78, 80, 83, 
			54, 56, 59, 61, 67, 71, 72, 72, 74, 77, 77, 78, 80, 82, 84, 86, 
			66, 70, 71, 72, 74, 76, 77, 77, 79, 81, 83, 85, 87, 87, 89, 89, 
			68, 72, 73, 74, 76, 78, 78, 80, 82, 84, 86, 88, 89, 90, 91, 91, 
			70, 74, 74, 76, 77, 79, 80, 81, 84, 86, 88, 90, 91, 92, 93, 93, 
			73, 76, 77, 78, 80, 82, 82, 84, 87, 89, 91, 93, 93, 95, 95, 95, 
			75, 78, 78, 80, 82, 84, 84, 86, 89, 91, 93, 95, 97, 97, 97, 99, 
			77, 80, 80, 81, 83, 85, 87, 88, 90, 92, 94, 96, 98, 98, 100, 100, 
			78, 79, 81, 82, 84, 85, 85, 87, 90, 92, 94, 96, 97, 99, 101, 101, 
			78, 80, 82, 83, 85, 86, 86, 88, 90, 92, 95, 99, 99, 101, 102, 102
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] {0, 0});

		InterPrediction prediction = new InterPrediction(new int[] { 0 }, null,
				new Vector[] { new Vector(-7, 0, 0), new Vector(0, -2, 0) },
				null);

		ResidualBlock cbDC = null;
		ResidualBlock crDC = null;

		ResidualBlock[] cbAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] crAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] luma = new ResidualBlock[] { null, null, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, null };

		CodedChroma chroma = new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);

		MBlockInter mb = new MBlockInter(0, chroma, null, luma, prediction,
				Type.MB_16x8);

		int qp = 28;
		Picture reference = readReferenceImage("src/test/resources/h264/ref_d0.pnm");

		NearbyMotionVectors nearMV = new NearbyMotionVectors(new Vector[] {
				new Vector(-192, -3, 0), new Vector(-192, -3, 0),
				new Vector(-183, -6, 0), new Vector(-183, -6, 0) }, null, null,
				null, true, false, false, false);

		DecodedMBlock actual = decoder.decodeP16x8(
				new Picture[] { reference }, mb, nearMV,
				new Point(608, 0), qp);

		assertArrayEquals(expected, actual.getLuma());
	}
	
	@Test
	public void testRightTopIsIntra() throws Exception {
		int[] expected = new int[] {
			82, 83, 85, 86, 88, 90, 90, 91, 93, 95, 97, 100, 97, 98, 99, 99,
			85, 87, 88, 90, 92, 94, 94, 95, 97, 98, 100, 101, 99, 99, 100, 100,
			87, 89, 91, 92, 94, 96, 96, 97, 99, 101, 102, 103, 101, 101, 102, 102,
			88, 91, 92, 94, 96, 98, 98, 99, 101, 102, 103, 105, 102, 103, 102, 102,
			89, 92, 93, 95, 97, 99, 99, 100, 102, 103, 104, 106, 103, 104, 104, 104,
			90, 93, 94, 96, 98, 100, 100, 101, 103, 104, 105, 107, 104, 104, 105, 105,
			91, 94, 95, 97, 99, 101, 101, 102, 104, 105, 106, 108, 105, 105, 106, 106,
			91, 94, 96, 97, 99, 101, 101, 102, 104, 106, 106, 108, 105, 106, 106, 106,
			92, 95, 97, 98, 100, 102, 102, 103, 105, 107, 107, 109, 110, 110, 111, 111,
			93, 95, 97, 98, 100, 102, 103, 104, 105, 107, 107, 109, 111, 111, 112, 112,
			94, 96, 98, 99, 101, 103, 104, 105, 106, 108, 108, 110, 111, 111, 112, 112,
			94, 96, 98, 99, 101, 103, 104, 105, 106, 108, 108, 110, 111, 111, 112, 112,
			95, 97, 99, 100, 102, 104, 105, 106, 107, 109, 109, 111, 112, 112, 113, 113,
			95, 97, 99, 100, 102, 104, 105, 106, 107, 109, 109, 111, 112, 113, 113, 113,
			96, 98, 100, 101, 103, 105, 106, 107, 108, 110, 110, 112, 113, 113, 114, 114,
			97, 99, 100, 102, 104, 106, 107, 108, 109, 111, 111, 113, 114, 114, 115, 115
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] {0, 0});

		InterPrediction prediction = new InterPrediction(new int[] { 0 }, null,
				new Vector[] { new Vector(-3, -3, 0) }, null);

		ResidualBlock cbDC = null;
		ResidualBlock crDC = null;

		ResidualBlock[] cbAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] crAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] luma = new ResidualBlock[] {
				null,
				null,
				null,
				null,

				new ResidualBlock(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0 }),
				new ResidualBlock(new int[] { -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0 }),
				new ResidualBlock(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0 }),
				new ResidualBlock(new int[] { -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						0, 0, 0, 0, 0 }),

				null, null, null, null,

				null, null, null, null };

		CodedChroma chroma = new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);

		MBlockInter mb = new MBlockInter(0, chroma, null, luma, prediction,
				Type.MB_16x16);

		int qp = 28;
		Picture reference = readReferenceImage("src/test/resources/h264/ref_d0.pnm");

		NearbyMotionVectors nearMV = new NearbyMotionVectors(new Vector[] {
				new Vector(-181, -6, 0), new Vector(-181, -6, 0),
				new Vector(-181, -6, 0), new Vector(-181, -6, 0) },
				new Vector[] { new Vector(-183, -8, 0),
						new Vector(-183, -8, 0), new Vector(-183, -8, 0),
						new Vector(-183, -8, 0) }, null,
				new Vector(-183, -6, 0), true, true, true, true);

		DecodedMBlock actual = decoder.decodeP16x16(
				new Picture[] { reference }, mb, nearMV,
				new Point(608, 16), qp);

		assertArrayEquals(expected, actual.getLuma());
	}
	
	@Test
	public void testPSkipLeftIntra() throws Exception {
		int[] expected = new int[] {
			85, 85, 85, 84, 82, 82, 82, 81, 79, 79, 79, 79, 79, 79, 79, 79, 
			85, 85, 85, 84, 82, 82, 81, 80, 80, 79, 79, 79, 79, 79, 79, 79, 
			83, 83, 83, 82, 81, 81, 81, 80, 80, 79, 79, 79, 79, 79, 79, 79, 
			82, 82, 82, 81, 81, 81, 80, 80, 80, 79, 79, 79, 79, 79, 79, 79, 
			83, 82, 82, 82, 80, 80, 80, 79, 79, 79, 79, 79, 79, 79, 79, 79, 
			81, 81, 81, 80, 80, 80, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 
			82, 81, 81, 81, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 
			81, 81, 81, 80, 80, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 
			82, 81, 81, 81, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 
			82, 81, 81, 81, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 
			82, 81, 81, 81, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 
			81, 81, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 
			81, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 
			81, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 
			80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 
			80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80, 80
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] {0, 0});

		Picture reference = readReferenceImage("src/test/resources/h264/ref_d0.pnm");

		NearbyMotionVectors nearMV = new NearbyMotionVectors(null,
				new Vector[] { new Vector(39, 17, 0), new Vector(39, 17, 0),
						new Vector(39, 17, 0), new Vector(39, 17, 0) },
				new Vector(39, 17, 0), new Vector(39, 17, 0), true, true, true,
				true);

		DecodedMBlock actual = decoder.decodePSkip(
				new Picture[] { reference }, nearMV, new Point(240, 48),
				28);

		assertArrayEquals(expected, actual.getLuma());
	}
	
	@Test
	public void test16x8TopRightNA() throws Exception {
		int[] expected = new int[] {
			99, 100, 101, 102, 102, 102, 103, 103, 107, 108, 108, 108, 117, 117, 118, 118,
			97, 99, 100, 101, 101, 102, 103, 103, 108, 108, 109, 109, 117, 117, 118, 118, 
			95, 97, 99, 100, 99, 101, 103, 103, 108, 108, 109, 109, 117, 118, 119, 118, 
			94, 96, 97, 99, 99, 101, 102, 102, 107, 108, 108, 109, 117, 119, 119, 119, 
			93, 95, 97, 98, 99, 100, 101, 101, 107, 107, 108, 109, 112, 117, 122, 124, 
			93, 95, 96, 97, 98, 99, 99, 100, 106, 107, 108, 109, 112, 116, 122, 124, 
			93, 95, 95, 97, 97, 99, 99, 100, 106, 107, 107, 108, 111, 115, 120, 123, 
			93, 94, 95, 96, 96, 97, 98, 98, 104, 104, 105, 106, 109, 113, 118, 120, 
			96, 97, 97, 98, 99, 98, 99, 100, 100, 100, 101, 102, 110, 110, 110, 110, 
			94, 94, 94, 94, 94, 94, 95, 96, 96, 96, 97, 97, 105, 105, 105, 105, 
			92, 92, 92, 91, 92, 92, 92, 92, 92, 93, 93, 93, 100, 100, 101, 101, 
			91, 91, 90, 89, 88, 88, 88, 88, 88, 88, 88, 88, 95, 95, 95, 95, 
			90, 87, 87, 85, 83, 84, 84, 84, 84, 84, 84, 85, 89, 87, 86, 86, 
			89, 87, 85, 83, 80, 79, 79, 80, 80, 81, 81, 81, 85, 84, 83, 82, 
			85, 83, 81, 78, 76, 76, 76, 76, 76, 77, 77, 77, 82, 82, 80, 80, 
			78, 75, 72, 72, 72, 72, 71, 70, 70, 70, 71, 71, 75, 75, 77, 78
		};

		MBlockDecoderInter decoder = new MBlockDecoderInter(new int[] {0, 0});

		InterPrediction prediction = new InterPrediction(
				new int[] { 0 },
				null,
				new Vector[] { new Vector(0, -11, 0), new Vector(28, -1, 0)},
				null);

		ResidualBlock cbDC = null;
		ResidualBlock crDC = null;

		ResidualBlock[] cbAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] crAC = new ResidualBlock[] { null, null, null, null };

		ResidualBlock[] luma = new ResidualBlock[] { null, null, null, null,
			new ResidualBlock(new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}), 
			new ResidualBlock(new int[] {3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}), 
			new ResidualBlock(new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}), 
			new ResidualBlock(new int[] {3, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}), 
			null, null, null, null,
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}), 
			new ResidualBlock(new int[] {2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}), 
			new ResidualBlock(new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}), 
			new ResidualBlock(new int[] {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0})
		};
		

		CodedChroma chroma = new CodedChroma(cbDC, cbAC, crDC, crAC, null, null);

		MBlockInter mb = new MBlockInter(0, chroma, null, luma, prediction,
				Type.MB_16x8);

		int qp = 28;
		Picture reference = readReferenceImage("src/test/resources/h264/ref_d0.pnm");

		NearbyMotionVectors nearMV = new NearbyMotionVectors(null, null, null,
				new Vector(-171, -11, 0), true, true, false, true);

		DecodedMBlock actual = decoder.decodeP16x8(
				new Picture[] { reference }, mb, nearMV, new Point(624,
						112), qp);

		assertArrayEquals(expected, actual.getLuma());
	}

	private Picture readReferenceImage(String fileName) throws IOException {
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(fileName));
			Picture frame = PGMIO.readPGM(is);
			int lumaW = frame.getWidth();
			int lumaH = frame.getHeight();
			int lumaRefW = (lumaW + 32)*4;
			int lumaRefH = (lumaH + 32)*4;
			int chromaRefW = (lumaW / 2)*8;
			int chromaRefH = (lumaH / 2)*8;
			
			Interpolator interpolator = new Interpolator();
			
			int[] interpolated = interpolator.interpolateLuma(frame.getPlaneData(0), lumaW, lumaH);
			int[] chroma = new int[chromaRefW * chromaRefH];
		
			return new Picture(lumaRefW, lumaRefH, new int[][] {interpolated, chroma, chroma}, ColorSpace.YUV420);
			
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
}
