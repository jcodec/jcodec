package org.jcodec.common.model;

import static org.jcodec.common.ArrayUtil.randomIntArray;

import org.junit.Assert;
import org.junit.Test;

public class PictureTest {
    
    @Test
    public void testToAndFrom() throws Exception {
        int[] Y = randomIntArray(4096, 4, 1019);
        int[] U = randomIntArray(2048, 4, 1019);
        int[] V = randomIntArray(2048, 4, 1019);
        PictureHiBD pictureHbd = PictureHiBD.createPictureWithDepth(64, 64, new int[][] { Y, U, V }, ColorSpace.YUV422, 10);
        Picture picture = Picture.fromPictureHiBD(pictureHbd);

        PictureHiBD resultHbd = picture.toPictureHiBD();
        
        Assert.assertArrayEquals(Y, resultHbd.getPlaneData(0));
        Assert.assertArrayEquals(U, resultHbd.getPlaneData(1));
        Assert.assertArrayEquals(V, resultHbd.getPlaneData(2));
    }

}
