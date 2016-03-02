package org.jcodec.codecs.mjpeg;
import org.jcodec.common.dct.IDCT4x4;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.VLC;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Rect;
import org.jcodec.common.tools.MathUtil;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decodes JPEG in low res taking only first 4 coefficients from each DCT block
 * ( DCT 4x4 )
 * 
 * @author The JCodec project
 * 
 */
public class JpegToThumb4x4 extends JpegDecoder {

    public static JpegToThumb4x4 createJpegToThumb4x4() {
        return new JpegToThumb4x4(false, false);
    }

    public JpegToThumb4x4(boolean interlace, boolean topFieldFirst) {
        super(interlace, topFieldFirst);
    }

    private static final int mapping4x4[] = new int[] { 0, 1, 4, 8, 5, 2, 3, 6, 9, 12, 16, 13, 10, 7, 16, 16, 16, 11,
            14, 16, 16, 16, 16, 16, 15, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
            16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 };

    @Override
    void decodeBlock(BitReader bits, int[] dcPredictor, int[][] quant, VLC[] huff, Picture8Bit result, int[] buf, int blkX,
            int blkY, int plane, int chroma, int field, int step) {
        buf[1] = buf[2] = buf[3] = buf[4] = buf[5] = buf[6] = buf[7] = buf[8] = buf[9] = buf[10] = buf[11] = buf[12] = buf[13] = buf[14] = buf[15] = 0;

        dcPredictor[plane] = buf[0] = readDCValue(bits, huff[chroma]) * quant[chroma][0] + dcPredictor[plane];
        readACValues(bits, buf, huff[chroma + 2], quant[chroma]);
        IDCT4x4.idct(buf, 0);

        putBlock4x4(result.getPlaneData(plane), result.getPlaneWidth(plane), buf, blkX, blkY, field, step);
    }

    private void putBlock4x4(byte[] plane, int stride, int[] patch, int x, int y, int field, int step) {
        stride >>= 1;
        int dstride = step * stride;
        int off = field * stride + (y >> 1) * dstride + (x >> 1);
        for (int i = 0; i < 16; i += 4) {
            plane[off] = (byte)(MathUtil.clip(patch[i], 0, 255) - 128);
            plane[off + 1] = (byte)(MathUtil.clip(patch[i + 1], 0, 255) - 128);
            plane[off + 2] = (byte)(MathUtil.clip(patch[i + 2], 0, 255) - 128);
            plane[off + 3] = (byte)(MathUtil.clip(patch[i + 3], 0, 255) - 128);
            off += dstride;
        }
    }

    @Override
    void readACValues(BitReader _in, int[] target, VLC table, int[] quantTable) {
        int code;
        int curOff = 1;
        do {
            code = table.readVLC16(_in);
            if (code == 0xF0) {
                curOff += 16;
            } else if (code > 0) {
                int rle = code >> 4;
                curOff += rle;
                int len = code & 0xf;
                target[mapping4x4[curOff]] = toValue(_in.readNBit(len), len) * quantTable[curOff];
                curOff++;
            }
        } while (code != 0 && curOff < 19);

        if (code != 0) {
            do {
                code = table.readVLC16(_in);
                if (code == 0xF0) {
                    curOff += 16;
                } else if (code > 0) {
                    int rle = code >> 4;
                    curOff += rle;
                    int len = code & 0xf;
                    _in.skip(len);
                    curOff++;
                }
            } while (code != 0 && curOff < 64);
        }
    }

    @Override
    public Picture8Bit decodeField(ByteBuffer data, byte[][] data2, int field, int step) {
        Picture8Bit res = super.decodeField(data, data2, field, step);

        return new Picture8Bit(res.getWidth() >> 1, res.getHeight() >> 1, res.getData(), res.getColor(), new Rect(0, 0,
                res.getCroppedWidth() >> 1, res.getCroppedHeight() >> 1));
    }
}