package org.jcodec.common;
import org.jcodec.codecs.vp8.BooleanArithmeticDecoder;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.StringBuilder;
import java.lang.System;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * See for theoretical details: http://www.youtube.com/playlist?list=PLE125425EC837021F
 */
public class ArithmeticCoderTest {

    private static final int PRECISSION = 8;
    private static final int[] rs = new int[]{ 10, 25, 11, 15, 10 };
    
    public static String printArrayAsHex(byte[] b){
        StringBuilder sb = new StringBuilder("{");
        if (b.length > 0){
            sb.append("0x").append(Integer.toHexString(b[0]&0xff).toUpperCase());
            for (int i=1;i<b.length;i++)
                sb.append(", 0x").append(Integer.toHexString(b[i]&0xff).toUpperCase());
        }
        sb.append("}");
        return sb.toString();
    }
    
    @Test
    public void testPrinting() throws Exception {
        Assert.assertEquals("{0xD8}", printArrayAsHex(new byte[]{(byte)0xD8}));
        Assert.assertEquals("{0xD8, 0x44}", printArrayAsHex(new byte[]{(byte)0xD8, 0x44}));
    }
    

    @Test
    public void testEncoder() throws IOException {
        ArithmeticCoder ac = new ArithmeticCoder(PRECISSION, rs);
        
        ac.encode(Arrays.asList(new Integer[] { 1, 2, 3, 4, 0 }));
        Assert.assertArrayEquals(new byte[]{0x5C, 0x18}, ac.e.getArray());
        
        ac.encode(Arrays.asList(new Integer[] { 4, 0 }));        
        Assert.assertArrayEquals(new byte[]{(byte)0xDC}, ac.e.getArray());
        
        ac.encode(Arrays.asList(new Integer[] { 1, 1, 1, 2, 0 }));
        Assert.assertArrayEquals(new byte[]{0x3A, (byte)0x80}, ac.e.getArray());

    }

    @Test
    public void testDecoder() throws Exception {
        ArithmeticDecoder ad = new ArithmeticDecoder(PRECISSION, rs);
        ad.decode(new byte[] { 0x5C, 0x18 });
        System.out.println(ad.data);
        ad.decode(new byte[] { (byte) 0xDC });
        System.out.println(ad.data);
        ad.decode(new byte[] { 0x3A, (byte)0x80 });
        System.out.println(ad.data);
    }
    
    @Test
    public void testCodingAndDecoding() throws Exception {
        int[] smallRs = new int[]{ 2, 5, 1, 3, 2 };
        ArithmeticCoder ac = new ArithmeticCoder(PRECISSION, smallRs);
        ArithmeticDecoder ad = new ArithmeticDecoder(PRECISSION, smallRs);
        
        List<Integer> asList = Arrays.asList(new Integer[] { 1, 2, 3, 0 });
        ac.encode(asList);
        ad.decode(ac.e.getArray());
        Assert.assertEquals(asList, ad.data);
        
        asList = Arrays.asList(new Integer[] { 2, 3, 4, 0 });
        ac.encode(asList);
        ad.decode(ac.e.getArray());
        Assert.assertEquals(asList, ad.data);
        
        asList = Arrays.asList(new Integer[] { 1, 2, 4, 0 });
        ac.encode(asList);
        ad.decode(ac.e.getArray());
        Assert.assertEquals(asList, ad.data);
        
        asList = Arrays.asList(new Integer[] { 1, 3, 4, 0 });
        ac.encode(asList);
        ad.decode(ac.e.getArray());
        Assert.assertEquals(asList, ad.data);
        
        asList = Arrays.asList(new Integer[] { 4, 3, 4, 0 });
        ac.encode(asList);
        ad.decode(ac.e.getArray());
        Assert.assertEquals(asList, ad.data);
        
        asList = Arrays.asList(new Integer[] { 1, 2, 3, 4, 0 });
        ac.encode(asList);
        ad.decode(ac.e.getArray());
        Assert.assertEquals(asList, ad.data);
    }

    public static class ArithmeticCoder {
        public final long precission;
        public final long whole;
        public final long half;
        public final long quater;
        public final int[] r; // R = 13
        public final int[] c; // probability borders, c[0]=0, c[i] = r[0]+...+r[i-1]
        public final int[] d; // probablility gap sizes, d[i] = c[i]+r[i]
        public final int R;
        
        public Emitter e;
        
        public ArithmeticCoder(int precission, int[] r){
            this.precission = precission;
            this.whole = (1L << precission);
            this.half = whole >> 1;
            this.quater = whole >> 2;
            this.r = r;
            this.c = new int[r.length];
            this.d = new int[r.length];
            this.d[0] = this.r[0];
            this.c[0] = 0;
            int bigR = r[0];
            for(int i=1;i<r.length;i++){
                for(int k=0;k<i;k++){
                    c[i] += r[k];
                }
                d[i] = c[i]+r[i];
                bigR += r[i];
            }
           this.R = bigR;
        }

        public static long sOnes(long s) {
            return (1 << s) - 1;
        }

        public void emitZeroAndSOnes(long s) throws IOException {
            
//            System.out.print(0);
            e.emit(0);
            while (s > 0) {
//                System.out.print(1);
                e.emit(1);
                s--;
            }
        }

        private void emitOneAndSZeros(long s) throws IOException {
//            System.out.print(1);
            e.emit(1);
            while (s > 0) {
//                System.out.print(0);
                e.emit(0);
                s--;
            }
        }

        public void encode(List<Integer> input) throws IOException {
            e = new Emitter();
            long a = 0L;
            long b = whole;
            long s = 0;
            for (int index = 0; index < input.size(); index++) {
                long omega = b - a;
                b = a + Math.round((omega * d[input.get(index)]) / (R*1.0));
                a = a + Math.round((omega * c[input.get(index)]) / (R*1.0));
                while (b < half || a > half) {
                    if (b < half) {
                        // emit 0 and s 1's
                        // result = result << (s+1) | sOnes(result);
                        emitZeroAndSOnes(s);
                        s = 0;
                        a = 2*a; // a=2a
                        b = 2*b; // b=2b
                    } else if (a > half) {
                        // emit 1 and s 0's
                        // result = (result<<1 | 0x01)<<s;
                        emitOneAndSZeros(s);
                        s = 0;
                        a = 2*(a - half);
                        b = 2*(b - half);
                    }

                }
                while (a > quater && b < 3 * quater) {
                    s++;
                    a = 2*(a - quater);
                    b = 2*(b - quater);
                }
            }
            s++;
            if (a <= quater) {
                // emit 0 and s 1's
//                result = result << (s + 1) | sOnes(result);
                emitZeroAndSOnes(s);
            } else {
                // emit 1 and s 0's
                emitOneAndSZeros(s);
//                result = (result << 1 | 0x01) << s;
            }
        }
    }

    public static class ArithmeticDecoder {        
        public final long precission;
        public final long whole;
        public final long half;
        public final long quater;
        public final int[] r; // R = 13
        public final int[] c; // probability borders, c[0]=0, c[i] = r[0]+...+r[i-1]
        public final int[] d; // probablility gap sizes, d[i] = c[i]+r[i]
        public final int R;
        public List<Integer> data;
        
        public ArithmeticDecoder(int precission, int[] r){
            this.precission = precission;
            this.whole = (1L << precission);
            this.half = whole >> 1;
            this.quater = whole >> 2;
            this.r = r;
            this.c = new int[r.length];
            this.d = new int[r.length];
            this.d[0] = this.r[0];
            this.c[0] = 0;
            int bigR = r[0];
            for(int i=1;i<r.length;i++){
                for(int k=0;k<i;k++){
                    c[i] += r[k];
                }
                d[i] = c[i]+r[i];
                bigR += r[i];
            }
           this.R = bigR;
        }

        public void decode(byte[] bs) {
            data = new ArrayList<Integer>();
            long a = 0;
            long b = whole;
            long z = 0;
            long i = 0;
            while (i < precission && i < bs.length * 8) {
                if (BooleanArithmeticDecoder.getBitInBytes(bs, (int)i) != 0x00) {
                    z += (1L << (precission - i - 1));
                }
                i++;
            }
            while (true) {
                for (int j = 0; j < 5; j++) {
                    long omega = b - a;
                    long bzero = a + Math.round((omega * d[j]) / (R*1.0));
                    long azero = a + Math.round((omega * c[j]) / (R*1.0));
                    if (azero <= z && z < bzero) {
                        data.add(j);
                        a = azero;
                        b = bzero;
                        if (j == 0) {
                            return;
                        }
                        break;
                    }
                }
                while (b < half || a > half) {
                    if (b < half) {
                        a = 2*a;
                        b = 2*b;
                        z = 2*z;
                    } else if (a > half) {
                        a = 2*(a - half);
                        b = 2*(b - half);
                        z = 2*(z - half);
                    }
                    if (i < (bs.length * 8)){
                        if (BooleanArithmeticDecoder.getBitInBytes(bs, (int)i) == 0x01)
                            z++;
                        i++;
                    }

                }
                while (a > quater && b < 3 * quater) {
                    a = (a - quater) << 1;
                    b = (b - quater) << 1;
                    z = (z - quater) << 1;
                    if (i < (bs.length * 8) ){
                        if (BooleanArithmeticDecoder.getBitInBytes(bs, (int)i) == 0x01)
                            z++;
                        i++;
                    }
                }
            }
        }
    }
    
    @Test
    public void testEmiter() throws Exception {
        Emitter p = new Emitter();
        Assert.assertArrayEquals(new byte[]{}, p.getArray());
        
        p.emit(1);p.emit(1);p.emit(0);p.emit(1);p.emit(1);p.emit(0);
        Assert.assertArrayEquals(new byte[]{(byte)0xD8}, p.getArray());
        
        p = new Emitter();
        // 01000011001
        p.emit(0);p.emit(1);p.emit(0);p.emit(0);p.emit(0);p.emit(0);p.emit(1);p.emit(1);p.emit(0);p.emit(0);p.emit(1);
        Assert.assertArrayEquals(new byte[]{0x43, 0x20}, p.getArray());
    }
    
    public static class Emitter{
        private int i=0;
        private byte buffer=0;
        private ByteArrayOutputStream baos;
        
        public Emitter() {
            this.baos = new ByteArrayOutputStream();
        }
        public void emit(int b) throws IOException {
            if (b != 1 && b != 0)
                throw new IllegalArgumentException("Only 0's and 1's are accepted");
                
            buffer |= b<<(7-i); 
            i++;
            if (i>7){
                i=0;
                baos.write(new byte[]{buffer});
                buffer=0;
            }
        }
        
        public byte[] getArray() throws IOException {
            if (i!=0){
                i=0;
                baos.write(new byte[]{buffer});
                buffer=0;
            }
            return baos.toByteArray();
        }
    }

}
