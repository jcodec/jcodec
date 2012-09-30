package org.jcodec.codecs.h264.decode;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Quarter pixel image interpolator for inter prediction
 * 
 * @author Jay Codec
 * 
 */
public class Interpolator {

    private static int PADDING = 16;

    public int[] interpolateChroma(int[] src, int width, int height) {
        int refWidth = (width << 3);
        int refHeight = (height << 3);

        int[] result = new int[refWidth * refHeight];

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int resultOff = ((j << 3) + y) * refWidth + (i << 3) + x;
                        int w00 = j * width + i;
                        int w01 = w00 + (j < height - 1 ? width : 0);
                        int w10 = w00 + (i < width - 1 ? 1 : 0);
                        int w11 = w10 + w01 - w00;
                        int eMx = 8 - x;
                        int eMy = 8 - y;

                        result[resultOff] = (eMx * eMy * src[w00] + x * eMy * src[w10] + eMx * y * src[w01] + x * y
                                * src[w11] + 32) >> 6;
                    }
                }
            }
        }

        return result;
    }

    public int[] interpolateLuma(int[] src, int width, int height) {
        int refWidth = (width + PADDING * 2) * 4;
        int refHeight = (height + PADDING * 2) * 4;

        int[] result = new int[refWidth * refHeight];

        fillFullPel(src, width, height, result);

        scanHPelHorizontalWithRound(refWidth, refHeight, result);

        scanHPelVertical(refWidth, refHeight, result);

        scanHPelCenterWidhRound(refWidth, refHeight, result);

        roundHPelVertical(refWidth, refHeight, result);

        scanQPel(refWidth, refHeight, result);

        return result;
    }

    protected void scanQPel(int width, int height, int[] result) {
        for (int j = 0; j < height; j += 2) {
            for (int i = 0; i < width; i += 2) {
                int pos = j * width + i;

                int bottomHpel = j < height - 2 ? result[pos + 2 * width] : result[pos];
                int rightHpel = i < width - 2 ? result[pos + 2] : result[pos];
                int rightBottomHpel;

                if (j < height - 2 && i < width - 2)
                    rightBottomHpel = result[pos + 2 * width + 2];
                else if (j < height - 2)
                    rightBottomHpel = result[pos + 2 * width];
                else if (i < width - 2)
                    rightBottomHpel = result[pos + 2];
                else
                    rightBottomHpel = result[pos];

                result[pos + width] = (result[pos] + bottomHpel + 1) >> 1;
                result[pos + 1] = (result[pos] + rightHpel + 1) >> 1;
                if ((i % 4) == (j % 4)) {
                    result[pos + width + 1] = (rightHpel + bottomHpel + 1) >> 1;
                } else {
                    result[pos + width + 1] = (result[pos] + rightBottomHpel + 1) >> 1;
                }
            }
        }
    }

    protected void fillFullPel(int[] src, int width, int height, int[] result) {
        int stride = (width + PADDING * 2) * 4;

        for (int j = 0; j < height; j++) {
            int y = (j + PADDING) * 4;

            for (int i = 0; i < width; i++) {
                int x = (i + PADDING) * 4;
                result[y * stride + x] = src[j * width + i];
            }
            for (int i = 0; i < PADDING; i++) {
                int x = i * 4;
                result[y * stride + x] = src[j * width];
            }
            for (int i = width + PADDING; i < width + PADDING * 2; i++) {
                int x = i * 4;
                result[y * stride + x] = src[j * width + width - 1];
            }
        }
        for (int j = 0; j < PADDING; j++) {
            int y = j * 4;

            for (int i = 0; i < width; i++) {
                int x = (i + PADDING) * 4;
                result[y * stride + x] = src[i];
            }
            for (int i = 0; i < PADDING; i++) {
                int x = i * 4;
                result[y * stride + x] = src[0];
            }
            for (int i = width + PADDING; i < width + PADDING * 2; i++) {
                int x = i * 4;
                result[y * stride + x] = src[width - 1];
            }
        }
        for (int j = height + PADDING; j < height + PADDING * 2; j++) {
            int y = j * 4;

            for (int i = 0; i < width; i++) {
                int x = (i + PADDING) * 4;
                result[y * stride + x] = src[(height - 1) * width + i];
            }
            for (int i = 0; i < PADDING; i++) {
                int x = i * 4;
                result[y * stride + x] = src[(height - 1) * width];
            }
            for (int i = width + PADDING; i < width + PADDING * 2; i++) {
                int x = i * 4;
                result[y * stride + x] = src[(height - 1) * width + width - 1];
            }
        }
    }

    protected void scanHPelVertical(int width, int height, int[] result) {
        for (int i = 0; i < width; i += 4) {
            int E = result[i];
            int F = result[i];
            int G = result[i];
            int H = result[i + 4 * width];
            int I = result[i + 8 * width];
            int J = result[i + 12 * width];

            for (int j = 0; j < height; j += 4) {
                int val = E - 5 * F + 20 * G + 20 * H - 5 * I + J;
                result[(j + 2) * width + i] = val;
                E = F;
                F = G;
                G = H;
                H = I;
                I = J;
                int nextPix = j + 16;
                if (nextPix < height) {
                    J = result[nextPix * width + i];
                }
            }
        }
    }

    protected void roundHPelVertical(int width, int height, int[] result) {
        for (int i = 0; i < width; i += 4) {

            for (int j = 0; j < height; j += 4) {

                result[(j + 2) * width + i] = roundAndClip32(result[(j + 2) * width + i]);
            }
        }
    }

    protected void scanHPelHorizontalWithRound(int width, int height, int[] result) {

        for (int j = 0; j < height; j += 4) {
            int lineStart = j * width;
            int E = result[lineStart];
            int F = result[lineStart];
            int G = result[lineStart];
            int H = result[lineStart + 4];
            int I = result[lineStart + 8];
            int J = result[lineStart + 12];

            for (int i = 0; i < width; i += 4) {

                int val = E - 5 * F + 20 * G + 20 * H - 5 * I + J;
                result[lineStart + i + 2] = roundAndClip32(val);
                E = F;
                F = G;
                G = H;
                H = I;
                I = J;
                int nextPix = i + 16;
                if (nextPix < width) {
                    J = result[lineStart + nextPix];
                }
            }
        }
    }

    protected void scanHPelCenterWidhRound(int width, int height, int[] result) {

        for (int j = 0; j < height; j += 4) {
            int lineStart = (j + 2) * width;
            int E = result[lineStart];
            int F = result[lineStart];
            int G = result[lineStart];
            int H = result[lineStart + 4];
            int I = result[lineStart + 8];
            int J = result[lineStart + 12];

            for (int i = 0; i < width; i += 4) {

                int val = E - 5 * F + 20 * G + 20 * H - 5 * I + J;
                result[lineStart + i + 2] = roundAndClip1024(val);
                E = F;
                F = G;
                G = H;
                H = I;
                I = J;
                int nextPix = i + 16;
                if (nextPix < width) {
                    J = result[lineStart + nextPix];
                }
            }
        }
    }

    private int roundAndClip32(int val) {
        val = (val + 16) >> 5;
        val = val < 0 ? 0 : (val > 255 ? 255 : val);
        return val;
    }

    private int roundAndClip1024(int val) {
        val = (val + 512) >> 10;
        val = val < 0 ? 0 : (val > 255 ? 255 : val);
        return val;
    }
}
