package org.jcodec.codecs.vp8;

import static org.jcodec.codecs.vp8.VP8EncoderTest.LinearAlgebraUtil.substractScalar;

import org.junit.Assert;
import org.junit.Test;

/**
 * @see http://multimedia.cx/eggs/category/vp8/
 * @see https://ritdml.rit.edu/bitstream/handle/1850/14525/SCassidyThesis11-2011.pdf?sequence=1
 * @see http://www-ee.uta.edu/Dip/Courses/EE5359/2011SpringFinalReportPPT/Shah_EE5359Spring2011FinalPPT.pdf
 * 
 */
public class VP8EncoderTest {
    
    public static final int[] naturalToZigzag4x4Index = {
        0,  1,  4,  8, 
        5,  2,  3,  6,  
        9, 12, 13, 10,  
        7, 11, 14, 15
    };
    
    public static final int[] zigzagToNatural4x4Index = {
        0,  1,  5,  6, 
        2,  4,  7, 12,  
        3,  8, 11, 13,  
        9, 10, 14, 15
    };

    private static final int MBSIZE = 16;

    @Test
    public void testBOr() throws Exception {
        boolean skipInnerLoopFilter = false;
        skipInnerLoopFilter = skipInnerLoopFilter | d(false);
        System.out.println(skipInnerLoopFilter);
        skipInnerLoopFilter = skipInnerLoopFilter | d(true);
        skipInnerLoopFilter = skipInnerLoopFilter | d(false);
        System.out.println(skipInnerLoopFilter);
    }
    
    public static boolean d(boolean b){
        System.out.println("executed d");
        return b;
    }
    
//    public void testName() throws Exception {
//        BufferedImage in = ImageIO.read(MKVMuxerTest.tildeExpand("src/test/resources/olezha422.jpg"));
//        Transform t = ColorUtil.getTransform(ColorSpace.RGB, ColorSpace.YUV420);
//        Picture yuv = Picture.create(in.getWidth(), in.getHeight(), ColorSpace.YUV420);
//        t.transform(AWTUtil.fromBufferedImage(in), yuv);
//        int mbCols = VP8Util.getMacroblockCount(in.getWidth());
//        int mbRows = VP8Util.getMacroblockCount(in.getHeight());
//        System.out.println("image: "+in.getHeight()+"x"+in.getWidth()+" macroblocks: "+mbRows+"x"+mbCols);
//        getMbData(yuv.getData()[0], in.getHeight(), in.getWidth(), mbRows, mbCols);
//        
//    }
    
    int[] yPlane = {
            
            /*---------------------Macrobloc[0,0]---------------------+-----------Macroblock[1,0]---------*/
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            /*---------------------Macrobloc[0,1]---------------------+-----------Macroblock[1,1]---------*/
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, /* | */ 17, 18, 19, 20, 21, 22, 23, 24,
            /*--------------------------------------------------------+-----------------------------------*/
            
    };
    
    @Test
    public void testMacroblocking() {
        Assert.assertEquals(256, countNonZero(getMacroblock(0, 0, yPlane, 24, 23)));
        Assert.assertEquals(128, countNonZero(getMacroblock(1, 0, yPlane, 24, 23)));
        Assert.assertEquals(112, countNonZero(getMacroblock(0, 1, yPlane, 24, 23)));
        int[] mb11 = getMacroblock(1, 1, yPlane, 24, 23);
        Assert.assertEquals(56,  countNonZero(mb11));
        Assert.assertArrayEquals(new int[]{
                17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 0,
                17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 0,
                17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 0,
                17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 0,
                17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 0,
                17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 0,
                17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
                 0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0, 0, 0,
        }, mb11);
    }
    
    @Test
    public void testSubblocking() {
        int[] mb11 = getMacroblock(1, 1, yPlane, 24, 23);
        /**
         * <pre>
         *  ------sb[0,0]----+------sb[1,0]----+---sb[2,0]---+---sb[3,0]----
         *   17, 18, 19, 20, | 21, 22, 23, 24, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *   17, 18, 19, 20, | 21, 22, 23, 24, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *   17, 18, 19, 20, | 21, 22, 23, 24, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *   17, 18, 19, 20, | 21, 22, 23, 24, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *  ------sb[1,0]----+------sb[1,1]----+---sb[2,1]--+----sb[3,1]----
         *   17, 18, 19, 20, | 21, 22, 23, 24, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *   17, 18, 19, 20, | 21, 22, 23, 24, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *   17, 18, 19, 20, | 21, 22, 23, 24, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         * -------sb[2,0]----+------sb[2,1]----+---sb[2,2]---+---sb[2,3]----
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         * -------sb[3,0]----+------sb[3,1]----+---sb[3,2]---+---sb[3,3]----
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         *    0,  0,  0,  0, |  0,  0,  0,  0, | 0, 0, 0, 0, | 0, 0, 0, 0,
         * </pre>
         */
        Assert.assertArrayEquals(new int[]{
                17, 18, 19, 20, 
                17, 18, 19, 20,
                17, 18, 19, 20,
                0,  0,  0,  0, 
        }, getSubblock(0, 1, mb11));
        Assert.assertArrayEquals(new int[]{
                21, 22, 23, 24, 
                21, 22, 23, 24,
                21, 22, 23, 24,
                21, 22, 23, 24,
        }, getSubblock(1, 0, mb11));
        Assert.assertArrayEquals(new int[]{
                0,  0,  0,  0, 
                0,  0,  0,  0,
                0,  0,  0,  0,
                0,  0,  0,  0, 
        }, getSubblock(2, 3, mb11));
        Assert.assertArrayEquals(new int[]{
                0,  0,  0,  0, 
                0,  0,  0,  0,
                0,  0,  0,  0,
                0,  0,  0,  0, 
        }, getSubblock(3, 3, mb11));
    }

    private int[] getSubblock(int i, int j, int[] mb) {
        int X = i*4;
        int Y = j*64;
        int firstRow = 0*16+Y, secondRow = 1*16+Y, thirdRow = 2*16+Y, fourthRow = 3*16+Y;
        
        int firstColumn = 0+X;
        int secondColumn = 1+X;
        int thridColumn = 2+X;
        int fourthColumn = 3+X;
        return new int[]{
                mb[firstColumn+firstRow],  mb[secondColumn+firstRow],  mb[thridColumn+firstRow],  mb[fourthColumn+firstRow],
                mb[firstColumn+secondRow], mb[secondColumn+secondRow], mb[thridColumn+secondRow], mb[fourthColumn+secondRow],
                mb[firstColumn+thirdRow],  mb[secondColumn+thirdRow],  mb[thridColumn+thirdRow],  mb[fourthColumn+thirdRow],
                mb[firstColumn+fourthRow], mb[secondColumn+fourthRow], mb[thridColumn+fourthRow], mb[fourthColumn+fourthRow]
        };
    }

    private static int[] getMacroblock(int x, int y, int[] p, int width, int height) {
        int[] result =  new int[256];
        for(int j=0; j<MBSIZE; j++)
            for (int i=0; i<MBSIZE; i++){
                int k = j+y*MBSIZE;
                int l = i+x*MBSIZE;
                if (l < width && k < height)
                    result[j*MBSIZE+i] = p[k*width+l];
            }
        return result;
    }
    
    private static int countNonZero(int[] a){
        int result = 0;
        for(int el: a)
            if (el!=0)
                result++;
        return result;
    }

    private void getMbData(int[] is, int height, int width, int mbRows, int mbCols) {
        
    }

    @Test
    public void test() {
        int[] input = {92,  91,  89,  86,
                91,  90,  88,  86,
                89,  89,  89,  88,
                89,  87,  88,  93};
        int[] predictorRemoved = substractScalar(input, 128);
        Assert.assertArrayEquals(new int[]{
                -36, -37, -39, -42,
                -37, -38, -40, -42,
                -39, -39, -39, -40,
                -39, -41, -40, -35}, predictorRemoved);
        
        int[] transformed = VP8DCT.encodeDCT(predictorRemoved);
        Assert.assertArrayEquals(new int[]{ 
                -312,   7,   1,   0,
                   1,  12,  -5,   2,
                   2,  -3,   3,  -1,
                   1,   0,  -2,   1}, transformed);
    }
    
    public static class LinearAlgebraUtil {
        
        public static int[] multiplyByScalar(int[] vector, int scalar){
            int[] result = new int[vector.length];
            for (int i=0;i < vector.length;i++)
                result[i]=vector[i]*scalar;
            
            return result;
        }
        
        public static int[] divideByScalar(int[] vector, int scalar){
            int[] result = new int[vector.length];
            for (int i=0;i < vector.length;i++)
                result[i]=vector[i]/scalar;
            
            return result;
        }
        
        public static int[] substractScalar(int[] vector, int scalar){
            int[] result = new int[vector.length];
            for (int i=0;i < vector.length;i++)
                result[i]=vector[i]-scalar;
            
            return result;
        }
        
        public static int[] substractVector(int[] vector1, int[] vector2){
            if (vector1.length != vector2.length)
                throw new RuntimeException("vector1.length ("+vector1.length+") != vector2.length ("+vector2.length+")");
            
            int[] result = new int[vector1.length];
            for (int i=0;i < vector1.length;i++)
                result[i]=vector1[i]-vector2[i];
            
            return result;
        }
        
        public static int[] serialize2DTo1DArray(int[][] in){
            if (in.length == 0 )
                throw new RuntimeException("in.length == 0. Two-dimentional array required.");
            
            int width = in[0].length;
            int[] result = new int[in.length * width];
            for(int i=0;i<in.length;i++){
                if (width != in[i].length)
                    throw new RuntimeException("in["+i+"].length != in[0].length. A rectangular two-dimentional array required.");
                
                for(int j=0;j<in[i].length;j++){
                    result[i*width+j] = in[i][j];
                }
            }
            return result;
        }
        
        public static short[] intArrayToShortArray(int[] array){
            short[] result = new short[array.length];
            for (int i=0;i<array.length;i++)
                result[i] = (short) array[i];
            return result;
        }
    }

}
