package org.jcodec.scale;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.IntBuffer;

public class BitmapUtilTest {
    public static final Class<? extends int[]> CLASS_INT_ARRAY = new int[0].getClass();

    @Test
    public void testToBitmap() {
        Picture src = Picture.create(16, 16, ColorSpace.RGB);
        for (int i = 0; i < 256; i++) {
            src.getPlaneData(0)[i
                    * 3] = src.getPlaneData(0)[i * 3 + 1] = src.getPlaneData(0)[i * 3 + 2] = (byte) (i - 128);
        }

        int[] pix = new int[256];
        for (int i = 0; i < 256; i++)
            pix[i] = (255 << 24) | (i << 16) | (i << 8) | i;
        Bitmap dst = mock(Bitmap.class);
        BitmapUtil.toBitmap(src, dst);

        verify(dst).copyPixelsFromBuffer(eq(IntBuffer.wrap(pix)));
    }

    @Test
    public void testFromBitmap() {
        Bitmap src = mock(Bitmap.class);
        when(src.getWidth()).thenReturn(16);
        when(src.getHeight()).thenReturn(16);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                int[] pix = invocation.getArgumentAt(0, CLASS_INT_ARRAY);
                for (int i = 0; i < 256; i++)
                    pix[i] = (255 << 24) | (i << 16) | (i << 8) | i;
                return null;
            }
        }).when(src).getPixels(any(CLASS_INT_ARRAY), eq(0), eq(16), eq(0), eq(0), eq(16), eq(16));

        Picture dst = Picture.create(16, 16, ColorSpace.RGB);
        BitmapUtil.fromBitmap(src, dst);
        for (int i = 0; i < 256; i++) {
            Assert.assertEquals(dst.getPlaneData(0)[i * 3], i - 128);
            Assert.assertEquals(dst.getPlaneData(0)[i * 3 + 1], i - 128);
            Assert.assertEquals(dst.getPlaneData(0)[i * 3 + 2], i - 128);
        }
    }
}