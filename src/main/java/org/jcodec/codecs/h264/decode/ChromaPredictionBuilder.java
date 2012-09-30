package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;
import org.jcodec.codecs.h264.io.model.ChromaFormat;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Prediction builder for chroma samples
 * 
 * @author Jay Codec
 * 
 */
public class ChromaPredictionBuilder {
    private int bitDepthLuma;
    private ChromaFormat chromaFormat;

    public ChromaPredictionBuilder(int bitDepthLuma, ChromaFormat chromaFormat) {
        this.bitDepthLuma = bitDepthLuma;
        this.chromaFormat = chromaFormat;
    }

    public void predictWithMode(int mode, int blkX, int blkY, BlockBorder border, PixelBuffer pixels) {

        switch (mode) {
        case 0:
            if (blkX == 0 && blkY != 0) {
                predictDCLeftBorder(border, pixels);
            } else if (blkY == 0 && blkX != 0) {
                predictDCTopBorder(border, pixels);
            } else {
                predictDCInside(border, pixels);
            }
            break;
        case 1:
            predictVertical(border, pixels);
            break;
        case 2:
            predictHorizontal(border, pixels);
            break;
        case 3:
            predictPlane(border, pixels);
            break;
        }

    }

    public void predictVertical(BlockBorder border, PixelBuffer pixels) {
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++)
                pixels.put(i, j, border.getTop()[i]);
        }
    }

    public void predictHorizontal(BlockBorder border, PixelBuffer pixels) {
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++)
                pixels.put(i, j, border.getLeft()[j]);
        }
    }

    public void predictDCInside(BlockBorder border, PixelBuffer pixels) {

        int s0;

        if (border.getLeft() != null && border.getTop() != null) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += border.getLeft()[i];
            for (int i = 0; i < 4; i++)
                s0 += border.getTop()[i];

            s0 = (s0 + 4) >> 3;
        } else if (border.getLeft() != null) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += border.getLeft()[i];
            s0 = (s0 + 2) >> 2;
        } else if (border.getTop() != null) {
            s0 = 0;
            for (int i = 0; i < 4; i++)
                s0 += border.getTop()[i];
            s0 = (s0 + 2) >> 2;
        } else {
            s0 = 1 << (bitDepthLuma - 1);
        }

        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 4; i++)
                pixels.put(i, j, s0);
    }

    public void predictDCTopBorder(BlockBorder border, PixelBuffer pixels) {

        int s1;
        if (border.getTop() != null) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += border.getTop()[i];

            s1 = (s1 + 2) >> 2;
        } else if (border.getLeft() != null) {
            s1 = 0;
            for (int i = 0; i < 4; i++)
                s1 += border.getLeft()[i];
            s1 = (s1 + 2) >> 2;
        } else {
            s1 = 1 << (bitDepthLuma - 1);
        }

        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 4; i++)
                pixels.put(i, j, s1);
    }

    public void predictDCLeftBorder(BlockBorder border, PixelBuffer pixels) {

        int s2;
        if (border.getLeft() != null) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += border.getLeft()[i];
            s2 = (s2 + 2) >> 2;
        } else if (border.getTop() != null) {
            s2 = 0;
            for (int i = 0; i < 4; i++)
                s2 += border.getTop()[i];
            s2 = (s2 + 2) >> 2;
        } else {
            s2 = 1 << (bitDepthLuma - 1);
        }

        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 4; i++)
                pixels.put(i, j, s2);
    }

    public void predictPlane(BlockBorder border, PixelBuffer pixels) {
        int H = 0;

        for (int i = 0; i < 3; i++) {
            H += (i + 1) * (border.getTop()[4 + i] - border.getTop()[2 - i]);
        }
        H += 4 * (border.getTop()[7] - border.getTopLeft());

        int V = 0;
        for (int j = 0; j < 3; j++) {
            V += (j + 1) * (border.getLeft()[4 + j] - border.getLeft()[2 - j]);
        }
        V += 4 * (border.getLeft()[7] - border.getTopLeft());

        int c = (34 * V + 32) >> 6;
        int b = (34 * H + 32) >> 6;
        int a = 16 * (border.getLeft()[7] + border.getTop()[7]);

        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++) {
                int val = (a + b * (i - 3) + c * (j - 3) + 16) >> 5;
                pixels.put(i, j, val > 255 ? 255 : (val < 0 ? 0 : val));
            }
        }
    }

}
