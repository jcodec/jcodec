package org.jcodec.codecs.vp8;
import static org.jcodec.codecs.vp8.VP8EncoderTest.LinearAlgebraUtil.substractVector;

import org.jcodec.codecs.vp8.VP8EncoderTest.LinearAlgebraUtil;
import org.jcodec.codecs.vpx.VP8DCT;
import org.junit.Test;

import java.lang.System;

public class DCTTest {

    @Test
    public void testDCT() {
        short[] input = new short[]{
                -36, -37, -39, -42,
                -37, -38, -40, -42,
                -39, -39, -39, -40,
                -39, -41, -40, -35};
        short[] out = VP8DCT.encodeDCT(input);
        
        System.out.println("dct encoded: ");
        System.out.println(formatAsSquare(out));
        
        out = LinearAlgebraUtil.divideByScalar(out, (short)4);
        System.out.println("quantized: ");
        System.out.println(formatAsSquare(out));
        
        out = LinearAlgebraUtil.multiplyByScalar(out, (short) 4);
        System.out.println("dequantized: ");
        System.out.println(formatAsSquare(out));
        
        short[] restored = VP8DCT.decodeDCT(out);
        System.out.println("restored: ");
        System.out.println(formatAsSquare(restored));
        
        System.out.println("error: ");
        System.out.println(formatAsSquare(substractVector(input, restored)));
    }
    
    @Test
    public void testWHT(){
        short[] input = new short[]{
            -312, -333, -269, -242,
            -289, -287, -322, -239,
            -330, -334, -332, -329,
            -347, -339, -273, -312
        };
        short[] out = VP8DCT.encodeWHT(input);
        System.out.println("wht encoded: ");
        System.out.println(formatAsSquare(out));
        
        out = LinearAlgebraUtil.divideByScalar(out, (short) 4);
        System.out.println("quantized: ");
        System.out.println(formatAsSquare(out));
        
        out = LinearAlgebraUtil.multiplyByScalar(out, (short) 4);
        System.out.println("dequantized: ");
        System.out.println(formatAsSquare(out));
        
        short[] restored = VP8DCT.decodeWHT(out);
        System.out.println("restored: ");
        System.out.println(formatAsSquare(restored));
        
        System.out.println("error: ");
        System.out.println(formatAsSquare(substractVector(input, restored)));
    }
    
    public static String formatAsSquare(short[] a){
        int size = (a.length == 4) ? 2 : (a.length == 16) ? 4 : (a.length==64) ? 8 : 0;
        String str = "";
        for(int i=0;i<a.length;i++){
            str += String.valueOf(a[i]);
            str += (((i+1)%size)==0) ? "\n" : "\t";
        }
        return str;
    }
    
}
