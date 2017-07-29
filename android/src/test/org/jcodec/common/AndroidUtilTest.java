package org.jcodec.common;

import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import android.graphics.Bitmap;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.BitmapUtil;
import org.junit.Assert;

public class AndroidUtilTest {

    @Test
    public void testToBitmap() {
        BitmapUtil bitmapUtil = mock(BitmapUtil.class);
        Picture expected = Picture.create(16, 16, ColorSpace.RGB);
        Picture src = Picture.create(16, 16, ColorSpace.YUV420J);
        Arrays.fill(src.getPlaneData(0), (byte) (169 - 128));
        Arrays.fill(src.getPlaneData(1), (byte) (45 - 128));
        Arrays.fill(src.getPlaneData(2), (byte) (103 - 128));
        for (int i = 0; i < 256 * 3; i += 3) {
            expected.getPlaneData(0)[i] = (byte) (134 - 128);
            expected.getPlaneData(0)[i + 1] = (byte) (215 - 128);
            expected.getPlaneData(0)[i + 2] = (byte) (22 - 128);
        }
        AndroidUtil util = new AndroidUtil(bitmapUtil);
        util.toBitmapImpl(src);
        verify(bitmapUtil).toBitmapImpl(eq(expected));
    }

    @Test
    public void testFromBitmap() {
        BitmapUtil bitmapUtil = mock(BitmapUtil.class);
        Picture src = Picture.create(16, 16, ColorSpace.RGB);
        for (int i = 0; i < 256 * 3; i += 3) {
            src.getPlaneData(0)[i] = (byte) (134 - 128);
            src.getPlaneData(0)[i + 1] = (byte) (215 - 128);
            src.getPlaneData(0)[i + 2] = (byte) (22 - 128);
        }

        when(bitmapUtil.fromBitmapImpl(any(Bitmap.class))).thenReturn(src);

        Picture expected = Picture.create(16, 16, ColorSpace.YUV420J);
        Arrays.fill(expected.getPlaneData(0), (byte) (168 - 128));
        Arrays.fill(expected.getPlaneData(1), (byte) (45 - 128));
        Arrays.fill(expected.getPlaneData(2), (byte) (103 - 128));
        AndroidUtil util = new AndroidUtil(bitmapUtil);
        Bitmap mock = mock(Bitmap.class);
        when(mock.getConfig()).thenReturn(Bitmap.Config.ARGB_8888);
        when(mock.getWidth()).thenReturn(16);
        when(mock.getHeight()).thenReturn(16);
        Picture res = util.fromBitmapImpl(mock, ColorSpace.YUV420J);
        Assert.assertEquals(expected, res);
    }
}
