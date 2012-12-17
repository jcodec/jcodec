package org.jcodec.codecs.prores;

import static org.jcodec.codecs.mpeg12.MPEGConst.BLOCK_TO_CC;
import static org.jcodec.common.JCodecUtil.bufin;
import static org.jcodec.common.model.ColorSpace.YUV422_10;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.mpeg12.MPEGConst;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.codecs.prores.ProresEncoder.Profile;
import org.jcodec.common.dct.DCTRef;
import org.jcodec.common.dct.SimpleIDCT10Bit;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.FileRAOutputStream;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Muxer;
import org.jcodec.containers.mp4.MP4Muxer.CompressedTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MPSDemuxer.PES;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Mpeg2Prores extends MPEGDecoder {

    private DCT2Prores dct2Prores;

    public Mpeg2Prores(SequenceHeader sh, GOPHeader gh, Profile profile) {
        super(sh, gh);
        dct2Prores = new DCT2Prores(profile);
    }

    protected void idctPut(int[] block, int[][] buf, int stride, int chromaFormat, int blkNo, int mbX, int mbY,
            int dctType) {
        int mbAddr = mbY * (stride >> 4) + mbX;

        int off = blkNo < 4 ? ((mbAddr << 8) + (blkNo << 6))
                : ((mbAddr << (5 + chromaFormat)) + (((blkNo - 4) >> 1) << 6));

        System.arraycopy(block, 0, buf[BLOCK_TO_CC[blkNo]], off, 64);
        buf[3][mbAddr] = dctType;
    }

    public void transcode(Buffer data, OutputStream out) throws IOException {
        int width = (sh.horizontal_size + 15) & ~0xf;
        int height = (sh.vertical_size + 15) & ~0xf;

        int[][] buffer = new int[][] { new int[width * height], new int[width * height], new int[width * height],
                new int[(width >> 4) * (height >> 4)] };
        Picture dct = decodeFrame(data, buffer);

        Picture[] pic = convert(dct);

        if (pic.length == 1)
            dct2Prores.encodeFrame(out, pic[0]);
        else
            dct2Prores.encodeFrame(out, pic[0], pic[1]);
    }

    private Picture[] convert(Picture dct) {
        int nInterlaced = 0;
        for (int i : dct.getPlaneData(3)) {
            nInterlaced += i;
        }

        Picture[] result;
        if (nInterlaced == 0) {
            upShift(dct);
            result = new Picture[] { colorCvt(dct) };
        } /*
           * else if (nInterlaced < dct.getDctTypes().length / 10) { result =
           * new Picture[] { extend(progressive(dct)) }; }
           */else {
            Picture[] field = interlaced(dct);
            result = new Picture[] { colorCvt(field[0]), colorCvt(field[1]) };
        }
        System.out.println(nInterlaced);

        return result;
    }

    private void upShift(Picture dct) {
        for (int[] is : dct.getData()) {
            upShift(is, 0, is.length);
        }
    }

    private Picture[] interlaced(Picture dct) {
        int mbWidth = (dct.getWidth() + 15) >> 4;
        int mbHeight = (dct.getHeight() + 15) >> 4;

        Picture field1 = Picture.create(dct.getWidth(), dct.getHeight() >> 1, dct.getColor());
        Picture field2 = Picture.create(dct.getWidth(), dct.getHeight() >> 1, dct.getColor());

        splitY(mbWidth, mbHeight, dct.getPlaneData(0), field1.getPlaneData(0), field2.getPlaneData(0),
                dct.getPlaneData(3));
        splitCbCr(mbWidth, mbHeight, dct.getPlaneData(1), field1.getPlaneData(1), field2.getPlaneData(1),
                dct.getPlaneData(3));
        splitCbCr(mbWidth, mbHeight, dct.getPlaneData(2), field1.getPlaneData(2), field2.getPlaneData(2),
                dct.getPlaneData(3));

        return new Picture[] { field1, field2 };
    }

    private final void splitY(int mbWidth, int mbHeight, int[] y, int[] y1, int[] y2, int[] dctTypes) {
        int dstOff = 0, srcOff = 0, i = 0;
        for (int mbY = 0; mbY < mbHeight; mbY++) {
            for (int mbX = 0; mbX < mbWidth; mbX++, i++, dstOff += 256, srcOff += 256) {
                if (dctTypes[i] == 0) {
                    SimpleIDCT10Bit.idct10(y, srcOff);
                    SimpleIDCT10Bit.idct10(y, srcOff + 64);
                    SimpleIDCT10Bit.idct10(y, srcOff + 128);
                    SimpleIDCT10Bit.idct10(y, srcOff + 192);
                    deinterleave(y, srcOff, srcOff + 128, y1, y2, dstOff);
                    deinterleave(y, srcOff + 64, srcOff + 192, y1, y2, dstOff + 64);
                    DCTRef.fdct(y1, dstOff);
                    DCTRef.fdct(y1, dstOff + 64);
                    DCTRef.fdct(y2, dstOff);
                    DCTRef.fdct(y2, dstOff + 64);
                } else {
                    copyShift(y, srcOff, y1, dstOff, 128);
                    copyShift(y, srcOff + 128, y2, dstOff, 128);
                }
            }
            if ((mbY & 0x1) == 0)
                dstOff -= (mbWidth << 8) - 128;
            else
                dstOff -= 128;
        }
    }

    private final void copyShift(int[] src, int srcOff, int[] dst, int dstOff, int len) {
        for (int i = 0; i < len; i++)
            src[srcOff++] = dst[dstOff++] << 2;
    }

    private final void splitCbCr(int mbWidth, int mbHeight, int[] y, int[] y1, int[] y2, int[] dctTypes) {
        int dstOff = 0, srcOff = 0, i = 0;
        for (int mbY = 0; mbY < mbHeight; mbY++) {
            for (int mbX = 0; mbX < mbWidth; mbX++, i++, dstOff += 128, srcOff += 128) {
                if (dctTypes[i] == 0) {
                    SimpleIDCT10Bit.idct10(y, srcOff);
                    SimpleIDCT10Bit.idct10(y, srcOff + 64);
                    deinterleave(y, srcOff, srcOff + 64, y1, y2, dstOff);
                    DCTRef.fdct(y1, dstOff);
                    DCTRef.fdct(y2, dstOff);
                } else {
                    copyShift(y, srcOff, y1, dstOff, 64);
                    copyShift(y, srcOff + 64, y2, dstOff, 64);
                }
            }
            if ((mbY & 0x1) == 0)
                dstOff -= (mbWidth << 7) - 64;
            else
                dstOff -= 64;
        }
    }

    private void deinterleave(int[] y, int topOff, int botOff, int[] y1, int[] y2, int blkOff) {
        copyLine(y, y1, topOff + 0, blkOff + 0);
        copyLine(y, y1, topOff + 16, blkOff + 8);
        copyLine(y, y1, topOff + 32, blkOff + 16);
        copyLine(y, y1, topOff + 48, blkOff + 24);
        copyLine(y, y1, botOff + 0, blkOff + 32);
        copyLine(y, y1, botOff + 16, blkOff + 40);
        copyLine(y, y1, botOff + 32, blkOff + 48);
        copyLine(y, y1, botOff + 48, blkOff + 56);

        copyLine(y, y2, topOff + 8, blkOff + 0);
        copyLine(y, y2, topOff + 24, blkOff + 8);
        copyLine(y, y2, topOff + 40, blkOff + 16);
        copyLine(y, y2, topOff + 56, blkOff + 24);
        copyLine(y, y2, botOff + 8, blkOff + 32);
        copyLine(y, y2, botOff + 24, blkOff + 40);
        copyLine(y, y2, botOff + 40, blkOff + 48);
        copyLine(y, y2, botOff + 56, blkOff + 56);
    }

    private Picture progressive(Picture dct) {
        progressiveY(dct.getPlaneData(0), dct.getPlaneData(3));
        progressiveCbCr(dct.getPlaneData(0), dct.getPlaneData(3));
        progressiveCbCr(dct.getPlaneData(0), dct.getPlaneData(3));

        return dct;
    }

    private void progressiveY(int[] y, int[] dctTypes) {
        for (int i = 0; i < dctTypes.length; i++) {
            if (dctTypes[i] == 1) {
                SimpleIDCT10Bit.idct10(y, (i << 8) + 0);
                SimpleIDCT10Bit.idct10(y, (i << 8) + 64);
                SimpleIDCT10Bit.idct10(y, (i << 8) + 128);
                SimpleIDCT10Bit.idct10(y, (i << 8) + 192);
                interleave(y, (i << 8) + 0, (i << 8) + 128);
                interleave(y, (i << 8) + 64, (i << 8) + 192);
                DCTRef.fdct(y, (i << 8) + 0);
                DCTRef.fdct(y, (i << 8) + 64);
                DCTRef.fdct(y, (i << 8) + 128);
                DCTRef.fdct(y, (i << 8) + 192);
            } else {
                upShift(y, i << 8, 256);
            }
        }
    }

    private void upShift(int[] y, int off, int len) {
        for (int i = 0; i < len; i++)
            y[off++] <<= 2;
    }

    private void progressiveCbCr(int[] y, int[] dctTypes) {
        for (int i = 0; i < dctTypes.length; i++) {
            if (dctTypes[i] == 1) {
                SimpleIDCT10Bit.idct10(y, (i << 7) + 0);
                SimpleIDCT10Bit.idct10(y, (i << 7) + 64);
                interleave(y, (i << 7) + 0, (i << 7) + 64);
                DCTRef.fdct(y, (i << 7) + 0);
                DCTRef.fdct(y, (i << 7) + 64);
            } else {
                upShift(y, i << 7, 128);
            }
        }
    }

    private void interleave(int[] y, int off1, int off2) {
        int[] tmp = new int[64];
        for (int i = 0; i < 64; i++)
            tmp[i] = y[off2 + i];
        copyLine(y, y, off1 + 56, off2 + 48);
        copyLine(y, y, off1 + 48, off2 + 32);
        copyLine(y, y, off1 + 40, off2 + 16);
        copyLine(y, y, off1 + 32, off2);
        copyLine(y, y, off1 + 24, off1 + 48);
        copyLine(y, y, off1 + 16, off1 + 32);
        copyLine(y, y, off1 + 8, off1 + 16);

        copyLine(tmp, y, 0, off1 + 8);
        copyLine(tmp, y, 8, off1 + 24);
        copyLine(tmp, y, 16, off1 + 40);
        copyLine(tmp, y, 24, off1 + 56);
        copyLine(tmp, y, 32, off2 + 8);
        copyLine(tmp, y, 40, off2 + 24);
        copyLine(tmp, y, 48, off2 + 40);
    }

    private final void copyLine(int[] from, int[] to, int offFrom, int offTo) {
        for (int i = 0; i < 8; i++)
            to[offTo++] = from[offFrom++];
    }

    private Picture colorCvt(Picture in) {

        Picture out;
        if (in.getColor() == YUV422_10) {
            out = in;
        } else {
            Transform trans = ColorUtil.getTransform(in.getColor(), YUV422_10);
            out = Picture.create(in.getWidth(), in.getHeight(), YUV422_10);
            trans.transform(in, out);
        }

        return out;
    }
}