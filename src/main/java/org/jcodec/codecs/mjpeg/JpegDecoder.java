package org.jcodec.codecs.mjpeg;

import static org.jcodec.codecs.mjpeg.JpegUtils.zigzagDecodeAll;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import org.apache.commons.io.FileUtils;
import org.jcodec.common.dct.DCT;
import org.jcodec.common.dct.IntDCT;
import org.junit.Test;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class JpegDecoder {

    private static final byte[] JFIF = "JFIF".getBytes();

    public static void main(String[] args) throws Exception {
        new JpegDecoder().testPerformance();
    }

    public static DecodedImage doIt(InputStream is) throws IOException {
        JpegDecoder decoder = new JpegDecoder();
        return decoder.decode(new JpegParser().parse(is));
    }

    private final static int align(int val, int align) {
        return align + ((val - 1) & ~(align - 1));
    }

    public DecodedImage decode(CodedImage coded) throws IOException {
        return decode(coded, new DecodedImage(coded.getWidth(), coded
                .getHeight(), new int[coded.getWidth() * coded.getHeight()]));
    }

    public DecodedImage decode(CodedImage coded, DecodedImage decoded)
            throws IOException {
        IntBuffer result = IntBuffer.wrap(decoded.getPixels());

        JPEGBitStream jbs = new JPEGBitStream(coded);
        int blockH = coded.frame.getHmax() << 3;
        int blockV = coded.frame.getVmax() << 3;
        int width = coded.getWidth();
        int height = coded.getHeight();
        int alignedWidth = align(width, blockH);
        int alignedHeight = align(height, blockV);
        int xBlocks = alignedWidth / blockH;
        int xxBlocks = width / blockH;
        int yBlocks = alignedHeight / blockV;
        int yyBlocks = height / blockV;

        int dWidth = alignedWidth - width;
        int dHeight = alignedHeight - height;

        MCU block = MCU.create(coded.frame);
        for (int by = 0; by < yyBlocks; by++) {
            for (int bx = 0; bx < xxBlocks; bx++) {
                readAndDecode(coded, jbs, block);
                putBlock(result, width, block, bx, by);
            }

            for (int bx = xxBlocks; bx < xBlocks; bx++) {
                readAndDecode(coded, jbs, block);
                putBlock(result, width, block, bx, by, blockH - dWidth, blockV);
            }
        }

        for (int by = yyBlocks; by < yBlocks; by++) {
            for (int bx = 0; bx < xxBlocks; bx++) {
                readAndDecode(coded, jbs, block);
                putBlock(result, width, block, bx, by, blockH, blockV - dHeight);
            }

            for (int bx = xxBlocks; bx < xBlocks; bx++) {
                readAndDecode(coded, jbs, block);
                putBlock(result, width, block, bx, by, blockH - dWidth, blockV
                        - dHeight);
            }
        }

        return decoded;
    }

    private static void putBlock(IntBuffer result, int width, MCU block,
            int bx, int by) {
        int blockH = block.h;
        int blockV = block.v;
        IntBuffer rgb = block.getRgb24();
        int bY = by * blockV * width;
        int bX = bx * blockH;
        int blockPtr = bY + bX;
        for (int j = 0; j < blockV; j++) {
            result.position(blockPtr);
            for (int i = 0; i < blockH; i++) {
                result.put(rgb.get());
            }
            blockPtr += width;
        }
    }

    /** Corner case of putBlock */
    private static void putBlock(IntBuffer result, int width, MCU block,
            int bx, int by, int blockH, int blockV) {
        IntBuffer rgb = block.getRgb24();
        int bY = by * block.v * width;
        int bX = bx * block.h;
        int blockPtr = bY + bX;
        for (int j = 0; j < blockV; j++) {
            result.position(blockPtr);
            for (int i = 0; i < blockH; i++) {
                result.put(rgb.get());
            }
            rgb.position(rgb.position() + (block.h - blockH));
            blockPtr += width;
        }
    }

    private static void readAndDecode(CodedImage coded, JPEGBitStream jbs,
            MCU block) throws IOException {
        jbs.readBlock(block);
        decodeBlock(block, coded.getQuantLum(), coded.getQuantChrom());
    }

    @Test
    public void testPerformance() throws IOException {
        byte[] jpg = FileUtils.readFileToByteArray(new File(
                "src/test/resources/fr.jpg"));
        JpegParser parser = new JpegParser();
        JpegDecoder decoder = new JpegDecoder();
        CodedImage image = parser.parse(new ByteArrayInputStream(jpg));
        DecodedImage decoded = new DecodedImage(image.getWidth(), image
                .getHeight(), new int[image.getWidth() * image.getHeight()]);
        long start = System.currentTimeMillis();
        int count = 1000;
        for (int i = 0; i < count; i++) {
            decoder
                    .decode(parser.parse(new ByteArrayInputStream(jpg)),
                            decoded);
        }
        long time = System.currentTimeMillis() - start;
        System.out.println(count * 1000 / time + " imgs/sec");
        System.out.println(time / count + " msec/img");

    }

    protected static DCT dct = new IntDCT();

    // private final static SlowDCT dct = new SlowDCT();

    private static void decodeBlock(MCU block, QuantTable qLum,
            QuantTable qChrom) {

        zigzagDecodeAll(block.lum.data);
        zigzagDecodeAll(block.cb.data);
        zigzagDecodeAll(block.cr.data);

        dequantAll(block.lum.data, qLum);
        dequantAll(block.cb.data, qChrom);
        dequantAll(block.cr.data, qChrom);

        dct.decodeAll(block.lum.data);
        dct.decodeAll(block.cb.data);
        dct.decodeAll(block.cr.data);

        IntBuffer rgb = block.getRgb24();
        IntBuffer Cb = IntBuffer.wrap(block.cb.data[0]);
        IntBuffer Cr = IntBuffer.wrap(block.cr.data[0]);
        if (block.is420()) {
            IntBuffer y00 = IntBuffer.wrap(block.lum.data[0]);
            IntBuffer y01 = IntBuffer.wrap(block.lum.data[1]);
            IntBuffer y10 = IntBuffer.wrap(block.lum.data[2]);
            IntBuffer y11 = IntBuffer.wrap(block.lum.data[3]);

            for (int j = 0; j < 8; j++) {
                Cb.position((j & ~1) << 2);
                Cr.position((j & ~1) << 2);
                lineToRgb(y00, Cb, Cr, rgb);
                lineToRgb(y01, Cb, Cr, rgb);
            }
            for (int j = 8; j < 16; j++) {
                Cb.position((j & ~1) << 2);
                Cr.position((j & ~1) << 2);
                lineToRgb(y10, Cb, Cr, rgb);
                lineToRgb(y11, Cb, Cr, rgb);
            }
        } else if (block.is422()) {
            IntBuffer y00 = IntBuffer.wrap(block.lum.data[0]);
            IntBuffer y01 = IntBuffer.wrap(block.lum.data[1]);
            for (int j = 0; j < 8; j++) {
                Cb.position(j << 3);
                Cr.position(j << 3);
                lineToRgb(y00, Cb, Cr, rgb);
                lineToRgb(y01, Cb, Cr, rgb);
            }
        } else if (block.is444()) {
            IntBuffer Y = IntBuffer.wrap(block.lum.data[0]);
            while (rgb.hasRemaining()) {
                rgb.put(ImageConvert
                        .ycbcr_to_rgb24(Y.get(), Cb.get(), Cr.get()));
            }
        } else {
            throw new IllegalStateException("unsupported MCU");
        }

    }

    private static void lineToRgb(IntBuffer Y, IntBuffer Cb, IntBuffer Cr,
            IntBuffer rgb) {
        for (int i = 0; i < 8; i++) {
            Cb.position(Cb.position() - (i & 1));
            Cr.position(Cr.position() - (i & 1));
            int y = Y.get();
            int cb = Cb.get();
            int cr = Cr.get();
            rgb.put(ImageConvert.ycbcr_to_rgb24(y, cb, cr));
        }
    }

    private static void dequantAll(int[][] src, QuantTable quantTable) {
        for (int i = 0; i < src.length; i++) {
            dequant(src[i], quantTable);
        }
    }

    private static void dequant(int[] src, QuantTable quantTable) {
        for (int i = 0; i < 64; i++) {
            src[i] = (quantTable.getValues()[i] * src[i]);
        }
    }

}