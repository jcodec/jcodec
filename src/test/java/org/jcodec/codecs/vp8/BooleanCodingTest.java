package org.jcodec.codecs.vp8;

import static java.nio.ByteBuffer.wrap;
import static org.jcodec.codecs.vp8.BooleanArithmeticDecoder.leadingZeroCountInByte;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.ArithmeticCoderTest;
import org.jcodec.common.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class BooleanCodingTest {
    
    @Test
    public void testLeadingZero() throws Exception {
        Assert.assertEquals(7, leadingZeroCountInByte((byte)1));
        Assert.assertEquals(0, leadingZeroCountInByte((byte)129));
        Assert.assertEquals(0, leadingZeroCountInByte((byte)256));
        Assert.assertEquals(7, leadingZeroCountInByte((byte)257));
        Assert.assertEquals(1, leadingZeroCountInByte((byte)383));
    }
    
    @Test
    public void testBitInByte() throws Exception {
        Assert.assertEquals(1, BooleanArithmeticDecoder.getBitInBytes(new byte[] { (byte) 0x80 }, 0));
        Assert.assertEquals(0, BooleanArithmeticDecoder.getBitInBytes(new byte[] { (byte) 0x80 }, 1));
        Assert.assertEquals(1, BooleanArithmeticDecoder.getBitInBytes(new byte[] { (byte) 0x90 }, 3));
        Assert.assertEquals(1, BooleanArithmeticDecoder.getBitInBytes(new byte[] { (byte) 0x91 }, 7));
        Assert.assertEquals(1, BooleanArithmeticDecoder.getBitInBytes(new byte[] { 0x00, (byte) 0x91 }, 15));
    }

    @Test
    public void test() throws IOException {
        byte[] b = IOUtils.toByteArray(new FileInputStream(new File("src/test/resources/part1.vp8.mb")));
        BooleanArithmeticDecoder bac = new BooleanArithmeticDecoder(ByteBuffer.wrap(b), 0);
         Assert.assertEquals("clear type is expected to be 0", 0, bac.decodeBit());
         Assert.assertEquals("clamp type is expected to be 0", 0, bac.decodeBit());
         Assert.assertEquals("segmentation is expected to be disabled", 0, bac.decodeBit());
         Assert.assertEquals("simple filter disabled", 0, bac.decodeBit());
         Assert.assertEquals("filter level is 8", 8, bac.decodeInt(6));
         Assert.assertEquals("sharpness level is 0", 0, bac.decodeInt(3));
//        System.out.println("clear type is expected to be 0" + "  " + 0 + "  " + bac.decodeBit());
//        System.out.println("clamp type is expected to be 0" + "  " + 0 + "  " + bac.decodeBit());
//        System.out.println("segmentation is expected to be disabled" + "  " + 0 + "  " + bac.decodeBit());
//        System.out.println("simple filter disabled" + "  " + 0 + "  " + bac.decodeBit());
//        System.out.println("filter level is 8" + "  " + 8 + "  " + bac.decodeInt(6));
//        System.out.println("Sharpness level: " + bac.decodeInt(3));
//        System.out.println("mode_ref_lf_delta_update: " + bac.decodeBit());
         Assert.assertEquals(1, bac.decodeBit());
    }

    public static class BooleanArithmeticEncoder {
        private ByteBuffer output;
        int range;      /* 128 <= range <= 255 */
        int bottom;     /* minimum value of remaining output */
        int bitCount;  /* # of shifts before an output byte
                           is available */

        public BooleanArithmeticEncoder(ByteBuffer output) {
            this.output = output;
            range = 255;
            bottom = 0;
            bitCount = 24;
        }
        
        /**
         * copy-paste from http://tools.ietf.org/html/rfc6386
         * @param probability
         * @param value
         */
        public void encode(int probability, int value) {
            int split = 1 + (((range - 1) * probability) >> 8);

            if (value != 0) {
                bottom += split; /* move up bottom of interval */
                range -= split; /* with corresponding decrease in range */
            } else
                range = split; /* decrease range, leaving bottom alone */

            while (range < 128) { // This should also get replaced with BooleanArithmeticDecoder.leadingZeroCountInByte
                range <<= 1;

                if ((bottom & (1 << 31)) != 0) /* detect carry */
                    add_one_to_output(output);

                bottom <<= 1; /* before shifting bottom */

                --bitCount;
                if (bitCount == 0) { /* write out high byte of bottom ... */

                    output.put((byte) (bottom >> 24));

                    bottom &= (1 << 24) - 1; /* ... keeping low 3 bytes */

                    bitCount = 8; /* 8 shifts until next output */
                }
            }
        }
        
        /* Call this function (exactly once) after encoding the last
            bool value for the partition being written, copy-paste from http://tools.ietf.org/html/rfc6386 */
        public void flushRemaining(){

           int c = bitCount;
           int v = bottom;

           if ((v & (1 << (32 - c))) != 0)   /* propagate (unlikely) carry */
             add_one_to_output(output);
           
           v <<= c & 7;               /* before shifting remaining output */
           c >>= 3;                   /* to top of internal buffer */
           while (--c >= 0)
             v <<= 8;
           c = 4;
           while (--c >= 0) {    /* write remaining data, possibly padded */
             output.put((byte) (v >>> 24));
             v <<= 8;
           }
        }

        public static void add_one_to_output(ByteBuffer buffer) {
            byte[] ar = buffer.array();
            int idx = buffer.position();
            
            while( idx > 0 && (ar[--idx] == (byte)255)){
                ar[idx] = 0;
            }
            ar[idx]++;
        }
    }
    
    @Test
    public void testAdd() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(128);
        bb.put((byte)24);
        bb.put((byte)255);
        BooleanArithmeticEncoder.add_one_to_output(bb);
        Assert.assertEquals(2, bb.position());
        Assert.assertEquals((byte)0,  bb.get(bb.position()-1));
        Assert.assertEquals((byte)25, bb.get(bb.position()-2));        
    }
    
    @Test
    public void testOut() throws Exception {
        ByteBuffer bb = ByteBuffer.allocate(128);
        bb.put((byte)24);
        bb.put((byte)255);
        Assert.assertEquals(2, bb.position());
        Assert.assertEquals((byte)255, bb.get(bb.position()-1));
        Assert.assertEquals((byte)24, bb.get(bb.position()-2));        
    }
    
    @Test
    public void testDecodeEncodedSequence() throws Exception {
        BooleanArithmeticEncoder bae = new BooleanArithmeticEncoder(ByteBuffer.allocate(16));
        int probability = 155;
        int[] data = new int[]{1,0,0,0,0,1,1,0,0,0,0,1};
        for (int d : data)
            bae.encode(probability, d);

        bae.flushRemaining();
        byte[] array = bae.output.array();
        System.out.println(ArithmeticCoderTest.printArrayAsHex(array));
        BooleanArithmeticDecoder bac = new BooleanArithmeticDecoder(wrap(array), 0);
        for (int d : data)
            Assert.assertTrue(d == bac.decodeBool(probability));
        
    }
}
