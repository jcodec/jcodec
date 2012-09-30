package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.h264.decode.model.BlockBorder;
import org.jcodec.codecs.h264.decode.model.PixelBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Builds intra prediction for intra 4x4 coded macroblocks
 * 
 * @author Jay Codec
 * 
 */
public class Intra4x4PredictionBuilder {
    private int bitDepthLuma;

    public Intra4x4PredictionBuilder(int bitDepthLuma) {
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
            predictDiagonalDownLeft(border, pixels);
            break;
        case 4:
            predictDiagonalDownRight(border, pixels);
            break;
        case 5:
            predictVerticalRight(border, pixels);
            break;
        case 6:
            predictHorizontalDown(border, pixels);
            break;
        case 7:
            predictVerticalLeft(border, pixels);
            break;
        case 8:
            predictHorizontalUp(border, pixels);
            break;
        }
    }

    public void predictVertical(BlockBorder border, PixelBuffer pixels) {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                pixels.put(x, y, border.getTop()[x]);
            }
        }
    }

    public void predictHorizontal(BlockBorder border, PixelBuffer pixels) {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                pixels.put(x, y, border.getLeft()[y]);
            }
        }
    }

    public void predictDC(BlockBorder border, PixelBuffer pixels) {
        boolean leftAvail = border.getLeft()[0] != -1 && border.getLeft()[1] != -1 && border.getLeft()[2] != -1
                && border.getLeft()[3] != -1;
        boolean topAvail = border.getTop()[0] != -1 && border.getTop()[1] != -1 && border.getTop()[2] != -1
                && border.getTop()[3] != -1;

        int val;
        if (leftAvail && topAvail) {
            val = (border.getLeft()[0] + border.getLeft()[1] + border.getLeft()[2] + border.getLeft()[3]
                    + border.getTop()[0] + border.getTop()[1] + border.getTop()[2] + border.getTop()[3] + 4) >> 3;
        } else if (leftAvail) {
            val = (border.getLeft()[0] + border.getLeft()[1] + border.getLeft()[2] + border.getLeft()[3] + 2) >> 2;
        } else if (topAvail) {
            val = (border.getTop()[0] + border.getTop()[1] + border.getTop()[2] + border.getTop()[3] + 2) >> 2;
        } else {
            val = 1 << (bitDepthLuma - 1);
        }

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                pixels.put(x, y, val);
            }
        }

    }

    public void predictDiagonalDownLeft(BlockBorder border, PixelBuffer pixels) {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {

                if (x == 3 && y == 3)
                    pixels.put(x, y, (border.getTop()[6] + 3 * border.getTop()[7] + 2) >> 2);
                else
                    pixels.put(x, y, (border.getTop()[x + y] + 2 * border.getTop()[x + y + 1]
                            + border.getTop()[x + y + 2] + 2) >> 2);
            }
        }
    }

    public void predictDiagonalDownRight(BlockBorder border, PixelBuffer pixels) {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                if (x > y) {
                    int t1;
                    if (x - y - 2 == -1)
                        t1 = border.getTopLeft();
                    else
                        t1 = border.getTop()[x - y - 2];

                    int t2;
                    if (x - y - 1 == -1)
                        t2 = border.getTopLeft();
                    else
                        t2 = border.getTop()[x - y - 1];

                    int t3;
                    if (x - y == -1)
                        t3 = border.getTopLeft();
                    else
                        t3 = border.getTop()[x - y];

                    pixels.put(x, y, (t1 + 2 * t2 + t3 + 2) >> 2);
                } else if (x < y) {
                    int l1;
                    if (y - x - 2 == -1)
                        l1 = border.getTopLeft();
                    else
                        l1 = border.getLeft()[y - x - 2];

                    int l2;
                    if (y - x - 1 == -1)
                        l2 = border.getTopLeft();
                    else
                        l2 = border.getLeft()[y - x - 1];

                    int l3;
                    if (y - x == -1)
                        l3 = border.getTopLeft();
                    else
                        l3 = border.getLeft()[y - x];

                    pixels.put(x, y, (l1 + 2 * l2 + l3 + 2) >> 2);
                } else
                    pixels.put(x, y, (border.getTop()[0] + 2 * border.getTopLeft() + border.getLeft()[0] + 2) >> 2);
            }
        }
    }

    public void predictVerticalRight(BlockBorder border, PixelBuffer pixels) {

        int v1 = (border.getTopLeft() + border.getTop()[0] + 1) >> 1;
        int v2 = (border.getTop()[0] + border.getTop()[1] + 1) >> 1;
        int v3 = (border.getTop()[1] + border.getTop()[2] + 1) >> 1;
        int v4 = (border.getTop()[2] + border.getTop()[3] + 1) >> 1;
        int v5 = (border.getLeft()[0] + 2 * border.getTopLeft() + border.getTop()[0] + 2) >> 2;
        int v6 = (border.getTopLeft() + 2 * border.getTop()[0] + border.getTop()[1] + 2) >> 2;
        int v7 = (border.getTop()[0] + 2 * border.getTop()[1] + border.getTop()[2] + 2) >> 2;
        int v8 = (border.getTop()[1] + 2 * border.getTop()[2] + border.getTop()[3] + 2) >> 2;
        int v9 = (border.getTopLeft() + 2 * border.getLeft()[0] + border.getLeft()[1] + 2) >> 2;
        int v10 = (border.getLeft()[0] + 2 * border.getLeft()[1] + border.getLeft()[2] + 2) >> 2;

        pixels.put(0, 0, v1);
        pixels.put(1, 0, v2);
        pixels.put(2, 0, v3);
        pixels.put(3, 0, v4);
        pixels.put(0, 1, v5);
        pixels.put(1, 1, v6);
        pixels.put(2, 1, v7);
        pixels.put(3, 1, v8);
        pixels.put(0, 2, v9);
        pixels.put(1, 2, v1);
        pixels.put(2, 2, v2);
        pixels.put(3, 2, v3);
        pixels.put(0, 3, v10);
        pixels.put(1, 3, v5);
        pixels.put(2, 3, v6);
        pixels.put(3, 3, v7);
    }

    public void predictHorizontalDown(BlockBorder border, PixelBuffer pixels) {

        int v1 = (border.getTopLeft() + border.getLeft()[0] + 1) >> 1;
        int v2 = (border.getLeft()[0] + 2 * border.getTopLeft() + border.getTop()[0] + 2) >> 2;

        int v3 = (border.getTopLeft() + 2 * border.getTop()[0] + border.getTop()[1] + 2) >> 2;
        int v4 = (border.getTop()[0] + 2 * border.getTop()[1] + border.getTop()[2] + 2) >> 2;
        int v5 = (border.getLeft()[0] + border.getLeft()[1] + 1) >> 1;
        int v6 = (border.getTopLeft() + 2 * border.getLeft()[0] + border.getLeft()[1] + 2) >> 2;
        int v7 = (border.getLeft()[1] + border.getLeft()[2] + 1) >> 1;
        int v8 = (border.getLeft()[0] + 2 * border.getLeft()[1] + border.getLeft()[2] + 2) >> 2;
        int v9 = (border.getLeft()[2] + border.getLeft()[3] + 1) >> 1;
        int v10 = (border.getLeft()[1] + 2 * border.getLeft()[2] + border.getLeft()[3] + 2) >> 2;

        pixels.put(0, 0, v1);
        pixels.put(1, 0, v2);
        pixels.put(2, 0, v3);
        pixels.put(3, 0, v4);
        pixels.put(0, 1, v5);
        pixels.put(1, 1, v6);
        pixels.put(2, 1, v1);
        pixels.put(3, 1, v2);
        pixels.put(0, 2, v7);
        pixels.put(1, 2, v8);
        pixels.put(2, 2, v5);
        pixels.put(3, 2, v6);
        pixels.put(0, 3, v9);
        pixels.put(1, 3, v10);
        pixels.put(2, 3, v7);
        pixels.put(3, 3, v8);
    }

    public void predictVerticalLeft(BlockBorder border, PixelBuffer pixels) {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                if (y % 2 == 0)
                    pixels.put(x, y, (border.getTop()[x + (y >> 1)] + border.getTop()[x + (y >> 1) + 1] + 1) >> 1);
                else
                    pixels.put(x, y,
                            (border.getTop()[x + (y >> 1)] + 2 * border.getTop()[x + (y >> 1) + 1]
                                    + border.getTop()[x + (y >> 1) + 2] + 2) >> 2);
            }
        }
    }

    public void predictHorizontalUp(BlockBorder border, PixelBuffer pixels) {

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int zHU = x + 2 * y;

                if (zHU < 5) {
                    if (zHU % 2 == 0)
                        pixels.put(x, y, (border.getLeft()[y + (x >> 1)] + border.getLeft()[y + (x >> 1) + 1] + 1) >> 1);
                    else
                        pixels.put(x, y, (border.getLeft()[y + (x >> 1)] + 2 * border.getLeft()[y + (x >> 1) + 1]
                                + border.getLeft()[y + (x >> 1) + 2] + 2) >> 2);
                } else if (zHU == 5)
                    pixels.put(x, y, (border.getLeft()[2] + 3 * border.getLeft()[3] + 2) >> 2);
                else
                    pixels.put(x, y, border.getLeft()[3]);

            }
        }
    }
}
