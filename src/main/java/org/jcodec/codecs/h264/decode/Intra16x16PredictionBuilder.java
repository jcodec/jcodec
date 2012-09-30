package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Prediction builder class for intra 16x16 coded macroblocks
 * 
 * 
 * @author Jay Codec
 * 
 */
public class Intra16x16PredictionBuilder {
    private int bitDepthLuma;

    public Intra16x16PredictionBuilder(int bitDepthLuma) {
        this.bitDepthLuma = bitDepthLuma;
    }

    public void predictWithMode(int mode, BlockBorder border, PixelBuffer pixels) {
        switch (mode) {
        case 0:
            predictVertical(border, pixels);
            break;
        case 1:
            predictHorizontal(border, pixels);
            break;
        case 2:
            predictDC(border, pixels);
            break;
        case 3:
            predictPlane(border, pixels);
            break;
        }

    }

    public void predictVertical(BlockBorder border, PixelBuffer pixels) {
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++)
                pixels.put(i, j, border.getTop()[i]);
        }
    }

    public void predictHorizontal(BlockBorder border, PixelBuffer pixels) {
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++)
                pixels.put(i, j, border.getLeft()[j]);
        }
    }

    public void predictDC(BlockBorder border, PixelBuffer pixels) {
        int s0;
        if (border.getLeft() != null && border.getTop() != null) {
            s0 = 0;
            for (int i = 0; i < 16; i++)
                s0 += border.getLeft()[i];
            for (int i = 0; i < 16; i++)
                s0 += border.getTop()[i];

            s0 = (s0 + 16) >> 5;
        } else if (border.getLeft() != null) {
            s0 = 0;
            for (int i = 0; i < 16; i++)
                s0 += border.getLeft()[i];
            s0 = (s0 + 8) >> 4;
        } else if (border.getTop() != null) {
            s0 = 0;
            for (int i = 0; i < 16; i++)
                s0 += border.getTop()[i];
            s0 = (s0 + 8) >> 4;
        } else {
            s0 = 1 << (bitDepthLuma - 1);
        }

        for (int j = 0; j < 16; j++)
            for (int i = 0; i < 16; i++)
                pixels.put(i, j, s0);
    }

    public void predictPlane(BlockBorder border, PixelBuffer pixels) {
        int H = 0;

        for (int i = 0; i < 7; i++) {
            H += (i + 1) * (border.getTop()[8 + i] - border.getTop()[6 - i]);
        }
        H += 8 * (border.getTop()[15] - border.getTopLeft());

        int V = 0;
        for (int j = 0; j < 7; j++) {
            V += (j + 1) * (border.getLeft()[8 + j] - border.getLeft()[6 - j]);
        }
        V += 8 * (border.getLeft()[15] - border.getTopLeft());

        int c = (5 * V + 32) >> 6;
        int b = (5 * H + 32) >> 6;
        int a = 16 * (border.getLeft()[15] + border.getTop()[15]);

        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < 16; i++) {
                int val = (a + b * (i - 7) + c * (j - 7) + 16) >> 5;
                pixels.put(i, j, val > 255 ? 255 : (val < 0 ? 0 : val));
            }
        }
    }
}
