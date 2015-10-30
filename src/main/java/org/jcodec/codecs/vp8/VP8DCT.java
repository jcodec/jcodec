package org.jcodec.codecs.vp8;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @see http://static.googleusercontent.com/external_content/untrusted_dlcp/research.google.com/de//pubs/archive/37073.pdf
 * @see http://jpegclub.org/jidctred/
 * @see http://www3.matapp.unimib.it/corsi-2007-2008/matematica/istituzioni-di-analisi-numerica/jpeg/papers/11-multiplications.pdf
 * @see http://static.googleusercontent.com/external_content/untrusted_dlcp/research.google.com/de//pubs/archive/37073.pdf
 * <pre>
 * </pre>
 * 
 * @author The JCodec project
 */
public class VP8DCT {
    
    private static final int cospi8sqrt2minus1 = 20091;

    private static final int sinpi8sqrt2 = 35468;
    
    public static int[] decodeDCT(int input[]) {

        int i;
        int a1, b1, c1, d1;
        int offset = 0;

        int[] output = new int[16];
        int temp1, temp2;

        for (i = 0; i < 4; i++) {
            a1 = input[offset + 0] + input[offset + 8];
            b1 = input[offset + 0] - input[offset + 8];

            temp1 = (input[offset + 4] * sinpi8sqrt2) >> 16;
            temp2 = input[offset + 12]
                    + ((input[offset + 12] * cospi8sqrt2minus1) >> 16);

            c1 = temp1 - temp2;

            temp1 = input[offset + 4]
                    + ((input[offset + 4] * cospi8sqrt2minus1) >> 16);
            temp2 = (input[offset + 12] * sinpi8sqrt2) >> 16;
            d1 = temp1 + temp2;

            output[offset + (0 * 4)] = a1 + d1;
            output[offset + (3 * 4)] = a1 - d1;
            output[offset + (1 * 4)] = b1 + c1;
            output[offset + (2 * 4)] = b1 - c1;

            offset++;
        }

        offset = 0;
        for (i = 0; i < 4; i++) {
            a1 = output[(offset * 4) + 0] + output[(offset * 4) + 2];
            b1 = output[(offset * 4) + 0] - output[(offset * 4) + 2];

            temp1 = (output[(offset * 4) + 1] * sinpi8sqrt2) >> 16;
            temp2 = output[(offset * 4) + 3]
                    + ((output[(offset * 4) + 3] * cospi8sqrt2minus1) >> 16);
            c1 = temp1 - temp2;

            temp1 = output[(offset * 4) + 1]
                    + ((output[(offset * 4) + 1] * cospi8sqrt2minus1) >> 16);
            temp2 = (output[(offset * 4) + 3] * sinpi8sqrt2) >> 16;
            d1 = temp1 + temp2;

            output[(offset * 4) + 0] = (a1 + d1 + 4) >> 3;
            output[(offset * 4) + 3] = (a1 - d1 + 4) >> 3;
            output[(offset * 4) + 1] = (b1 + c1 + 4) >> 3;
            output[(offset * 4) + 2] = (b1 - c1 + 4) >> 3;

            offset++;
        }

        return output;

    }
    
    public static int[] encodeDCT(int[] input) {
        int i;
        int a1, b1, c1, d1;
        int ip = 0;
        int[]output = new int[input.length];
        int op = 0;

        for (i = 0; i < 4; i++) {
            a1 = ((input[ip+0] + input[ip+3])<<3);
            b1 = ((input[ip+1] + input[ip+2])<<3);
            c1 = ((input[ip+1] - input[ip+2])<<3);
            d1 = ((input[ip+0] - input[ip+3])<<3);

            output[op+0] = a1 + b1;
            output[op+2] = a1 - b1;

            output[op+1] = (c1 * 2217 + d1 * 5352 +  14500)>>12;
            output[op+3] = (d1 * 2217 - c1 * 5352 +   7500)>>12;

            ip += 4;
            op += 4;

        }
        ip = 0;
        op = 0;
        for (i = 0; i < 4; i++) {
            a1 = output[ip+0] + output[ip+12];
            b1 = output[ip+4] + output[ip+8];
            c1 = output[ip+4] - output[ip+8];
            d1 = output[ip+0] - output[ip+12];

            output[op+0]  = ( a1 + b1 + 7)>>4;
            output[op+8]  = ( a1 - b1 + 7)>>4;

            output[op+4]  = ((c1 * 2217 + d1 * 5352 +  12000)>>16) + (d1!=0?1:0);
            output[op+12] = (d1 * 2217 - c1 * 5352 +  51000)>>16;

            ip++;
            op++;
        }
        return output;
    }

    public static int[] decodeWHT(int[] input) {
        int i;
        int a1, b1, c1, d1;
        int a2, b2, c2, d2;

        int[] output = new int[16];
        int diff[][] = new int[4][4];
        int offset = 0;
        for (i = 0; i < 4; i++) {
            a1 = input[offset + 0] + input[offset + 12];
            b1 = input[offset + 4] + input[offset + 8];
            c1 = input[offset + 4] - input[offset + 8];
            d1 = input[offset + 0] - input[offset + 12];

            output[offset + 0] = a1 + b1;
            output[offset + 4] = c1 + d1;
            output[offset + 8] = a1 - b1;
            output[offset + 12] = d1 - c1;
            offset++;
        }

        offset = 0;

        for (i = 0; i < 4; i++) {
            a1 = output[offset + 0] + output[offset + 3];
            b1 = output[offset + 1] + output[offset + 2];
            c1 = output[offset + 1] - output[offset + 2];
            d1 = output[offset + 0] - output[offset + 3];

            a2 = a1 + b1;
            b2 = c1 + d1;
            c2 = a1 - b1;
            d2 = d1 - c1;
            output[offset + 0] = (a2 + 3) >> 3;
            output[offset + 1] = (b2 + 3) >> 3;
            output[offset + 2] = (c2 + 3) >> 3;
            output[offset + 3] = (d2 + 3) >> 3;
            diff[0][i] = (a2 + 3) >> 3;
            diff[1][i] = (b2 + 3) >> 3;
            diff[2][i] = (c2 + 3) >> 3;
            diff[3][i] = (d2 + 3) >> 3;
            offset += 4;
        }

        return output;

    }

    public static int[] encodeWHT(int[] input){
        int i;
        int a1, b1, c1, d1;
        int a2, b2, c2, d2;
        int inputOffset = 0;
        int outputOffset = 0;
        int[] output = new int[input.length];


        for (i = 0; i < 4; i++) {
            /**
             * 
             */
            a1 = ((input[inputOffset+0] + input[inputOffset+2]))<<2;
            d1 = ((input[inputOffset+1] + input[inputOffset+3]))<<2;
            c1 = ((input[inputOffset+1] - input[inputOffset+3]))<<2;
            b1 = ((input[inputOffset+0] - input[inputOffset+2]))<<2;

            output[outputOffset+0] = a1 + d1 + (a1!=0?1:0);
            output[outputOffset+1] = b1 + c1;
            output[outputOffset+2] = b1 - c1;
            output[outputOffset+3] = a1 - d1;
            inputOffset += 4;
            outputOffset += 4;
        }

        inputOffset = 0;
        outputOffset = 0;

        for (i = 0; i < 4; i++) {
            a1 = output[inputOffset+0] + output[inputOffset+8];
            d1 = output[inputOffset+4] + output[inputOffset+12];
            c1 = output[inputOffset+4] - output[inputOffset+12];
            b1 = output[inputOffset+0] - output[inputOffset+8];

            a2 = a1 + d1;
            b2 = b1 + c1;
            c2 = b1 - c1;
            d2 = a1 - d1;

            a2 += (a2<0?1:0);
            b2 += (b2<0?1:0);
            c2 += (c2<0?1:0);
            d2 += (d2<0?1:0);

            output[outputOffset+0] = (a2+3) >> 3;
            output[outputOffset+4] = (b2+3) >> 3;
            output[outputOffset+8] = (c2+3) >> 3;
            output[outputOffset+12]= (d2+3) >> 3;

            inputOffset++;
            outputOffset++;
        }
        return output;
    }
}