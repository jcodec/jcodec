package org.jcodec.common;

import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import android.graphics.Bitmap;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.scale.BitmapUtil;
import org.junit.Assert;

public class AndroidUtilTest {

    @Test
    @Deprecated
    public void testToBitmap() {
        BitmapUtil bitmapUtil = mock(BitmapUtil.class);
        Picture expected = Picture.create(16, 16, ColorSpace.RGB);
        Picture src = Picture.create(16, 16, ColorSpace.YUV420J);
        Arrays.fill(src.getPlaneData(0), 169);
        Arrays.fill(src.getPlaneData(1), 45);
        Arrays.fill(src.getPlaneData(2), 103);
        for (int i = 0; i < 256 * 3; i += 3) {
            expected.getPlaneData(0)[i] = 134;
            expected.getPlaneData(0)[i + 1] = 215;
            expected.getPlaneData(0)[i + 2] = 22;
        }
        AndroidUtil util = new AndroidUtil(bitmapUtil);
        util.toBitmapImpl(src);
        verify(bitmapUtil).toBitmapImpl(eq(expected));
    }

    @Test
    public void testToBitmap8Bit() {
        BitmapUtil bitmapUtil = mock(BitmapUtil.class);
        Picture8Bit expected = Picture8Bit.create(16, 16, ColorSpace.RGB);
        Picture8Bit src = Picture8Bit.create(16, 16, ColorSpace.YUV420J);
        Arrays.fill(src.getPlaneData(0), (byte) (169 - 128));
        Arrays.fill(src.getPlaneData(1), (byte) (45 - 128));
        Arrays.fill(src.getPlaneData(2), (byte) (103 - 128));
        for (int i = 0; i < 256 * 3; i += 3) {
            expected.getPlaneData(0)[i] = (byte) (134 - 128);
            expected.getPlaneData(0)[i + 1] = (byte) (215 - 128);
            expected.getPlaneData(0)[i + 2] = (byte) (22 - 128);
        }
        AndroidUtil util = new AndroidUtil(bitmapUtil);
        util.toBitmap8BitImpl(src);
        verify(bitmapUtil).toBitmap8BitImpl(eq(expected));
    }

    @Test
    @Deprecated
    public void testFromBitmap() {
        BitmapUtil bitmapUtil = mock(BitmapUtil.class);
        Picture src = Picture.create(16, 16, ColorSpace.RGB);
        for (int i = 0; i < 256 * 3; i += 3) {
            src.getPlaneData(0)[i] = 134;
            src.getPlaneData(0)[i + 1] = 215;
            src.getPlaneData(0)[i + 2] = 22;
        }

        when(bitmapUtil.fromBitmapImpl(any(Bitmap.class))).thenReturn(src);

        Picture expected = Picture.create(16, 16, ColorSpace.YUV420J);
        Arrays.fill(expected.getPlaneData(0), 168);
        Arrays.fill(expected.getPlaneData(1), 45);
        Arrays.fill(expected.getPlaneData(2), 103);
        AndroidUtil util = new AndroidUtil(bitmapUtil);
        Bitmap mock = mock(Bitmap.class);
        when(mock.getConfig()).thenReturn(Bitmap.Config.ARGB_8888);
        when(mock.getWidth()).thenReturn(16);
        when(mock.getHeight()).thenReturn(16);
        Picture res = util.fromBitmapImpl(mock, ColorSpace.YUV420J);
        Assert.assertEquals(expected, res);
    }

    @Test
    public void testFromBitmap8Bit() {
        BitmapUtil bitmapUtil = mock(BitmapUtil.class);
        Picture8Bit src = Picture8Bit.create(16, 16, ColorSpace.RGB);
        for (int i = 0; i < 256 * 3; i += 3) {
            src.getPlaneData(0)[i] = (byte) (134 - 128);
            src.getPlaneData(0)[i + 1] = (byte) (215 - 128);
            src.getPlaneData(0)[i + 2] = (byte) (22 - 128);
        }

        when(bitmapUtil.fromBitmap8BitImpl(any(Bitmap.class))).thenReturn(src);

        Picture8Bit expected = Picture8Bit.create(16, 16, ColorSpace.YUV420J);
        Arrays.fill(expected.getPlaneData(0), (byte) (168 - 128));
        Arrays.fill(expected.getPlaneData(1), (byte) (45 - 128));
        Arrays.fill(expected.getPlaneData(2), (byte) (103 - 128));
        AndroidUtil util = new AndroidUtil(bitmapUtil);
        Bitmap mock = mock(Bitmap.class);
        when(mock.getConfig()).thenReturn(Bitmap.Config.ARGB_8888);
        when(mock.getWidth()).thenReturn(16);
        when(mock.getHeight()).thenReturn(16);
        Picture8Bit res = util.fromBitmap8BitImpl(mock, ColorSpace.YUV420J);
        Assert.assertEquals(expected, res);
    }
}
