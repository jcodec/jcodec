package org.jcodec.codecs.h264;
import static org.jcodec.common.ArrayUtil.toByteArrayShifted;
import static org.junit.Assert.assertArrayEquals;

import org.jcodec.codecs.h264.decode.ChromaPredictionBuilder;
import org.junit.Test;

public class ChromaPredictionBuilderTest {
    private static int[][] emptyResidual = new int[4][16];

    @Test
    public void testVertical() {
        byte[] expectedCb = toByteArrayShifted(new int[]{129, 127, 122, 119, 116, 116, 116, 116, 129, 127, 122, 119, 116, 116,
                116, 116, 129, 127, 122, 119, 116, 116, 116, 116, 129, 127, 122, 119, 116, 116, 116, 116, 129, 127,
                122, 119, 116, 116, 116, 116, 129, 127, 122, 119, 116, 116, 116, 116, 129, 127, 122, 119, 116, 116,
                116, 116, 129, 127, 122, 119, 116, 116, 116, 116});
        byte[] expectedCr = toByteArrayShifted(new int[]{128, 128, 128, 128, 132, 132, 132, 132, 128, 128, 128, 128, 132, 132,
                132, 132, 128, 128, 128, 128, 132, 132, 132, 132, 128, 128, 128, 128, 132, 132, 132, 132, 128, 128,
                128, 128, 132, 132, 132, 132, 128, 128, 128, 128, 132, 132, 132, 132, 128, 128, 128, 128, 132, 132,
                132, 132, 128, 128, 128, 128, 132, 132, 132, 132});

        byte[] topCb = toByteArrayShifted(new int[]{129, 127, 122, 119, 116, 116, 116, 116});
        byte[] topCr = toByteArrayShifted(new int[]{128, 128, 128, 128, 132, 132, 132, 132});

        byte[] actualCb = new byte[64];
        byte[] actualCr = new byte[64];

        ChromaPredictionBuilder.predictVertical(emptyResidual, 0, true, topCb, actualCb);
        ChromaPredictionBuilder.predictVertical(emptyResidual, 0, true, topCr, actualCr);

        assertArrayEquals(expectedCb, actualCb);
        assertArrayEquals(expectedCr, actualCr);
    }

    @Test
    public void testHorizontal() {
        byte[] expectedCb = toByteArrayShifted(new int[]{115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115,
                115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 115, 118, 118,
                118, 118, 118, 118, 118, 118, 116, 116, 116, 116, 116, 116, 116, 116, 111, 111, 111, 111, 111, 111,
                111, 111, 108, 108, 108, 108, 108, 108, 108, 108});
        byte[] expectedCr = toByteArrayShifted(new int[]{137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137,
                137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 137, 131, 131,
                131, 131, 131, 131, 131, 131, 134, 134, 134, 134, 134, 134, 134, 134, 139, 139, 139, 139, 139, 139,
                139, 139, 141, 141, 141, 141, 141, 141, 141, 141});

        byte[] leftCb = toByteArrayShifted(new int[]{115, 115, 115, 115, 118, 116, 111, 108});
        byte[] leftCr = toByteArrayShifted(new int[]{137, 137, 137, 137, 131, 134, 139, 141});

        byte[] actualCb = new byte[64];
        byte[] actualCr = new byte[64];

        ChromaPredictionBuilder.predictHorizontal(emptyResidual, 0, true, leftCb, actualCb);
        ChromaPredictionBuilder.predictHorizontal(emptyResidual, 0, true, leftCr, actualCr);

        assertArrayEquals(expectedCb, actualCb);
        assertArrayEquals(expectedCr, actualCr);
    }

    @Test
    public void testDC() {
        byte[] expectedCb = toByteArrayShifted(new int[]{119, 119, 119, 119, 120, 120, 120, 120, 119, 119, 119, 119, 120, 120,
                120, 120, 119, 119, 119, 119, 120, 120, 120, 120, 119, 119, 119, 119, 120, 120, 120, 120, 118, 118,
                118, 118, 119, 119, 119, 119, 118, 118, 118, 118, 119, 119, 119, 119, 118, 118, 118, 118, 119, 119,
                119, 119, 118, 118, 118, 118, 119, 119, 119, 119});

        byte[] expectedCr = toByteArrayShifted(new int[]{131, 131, 131, 131, 132, 132, 132, 132, 131, 131, 131, 131, 132, 132,
                132, 132, 131, 131, 131, 131, 132, 132, 132, 132, 131, 131, 131, 131, 132, 132, 132, 132, 132, 132,
                132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132, 132,
                132, 132, 132, 132, 132, 132, 132, 132, 132, 132});

        byte[] leftCb = toByteArrayShifted(new int[]{118, 118, 118, 118, 118, 118, 118, 118});
        byte[] topCb = toByteArrayShifted(new int[]{120, 120, 120, 120, 120, 120, 120, 120});

        byte[] leftCr = toByteArrayShifted(new int[]{131, 131, 131, 131, 132, 132, 132, 132});

        byte[] topCr = toByteArrayShifted(new int[]{131, 131, 131, 131, 132, 132, 132, 132});

        byte[] actualCb = new byte[64];
        byte[] actualCr = new byte[64];

        ChromaPredictionBuilder.predictDC(emptyResidual, 0, true, true, leftCb, topCb, actualCb);
        ChromaPredictionBuilder.predictDC(emptyResidual, 0, true, true, leftCr, topCr, actualCr);

        assertArrayEquals(expectedCb, actualCb);
        assertArrayEquals(expectedCr, actualCr);
    }

    @Test
    public void testPlane() {

        byte[] expectedCb = toByteArrayShifted(new int[]{115, 116, 116, 117, 117, 118, 118, 119, 116, 117, 117, 118, 118, 119,
                119, 120, 117, 117, 118, 118, 119, 119, 120, 120, 118, 118, 119, 119, 120, 120, 121, 121, 118, 119,
                119, 120, 120, 121, 121, 122, 119, 119, 120, 120, 121, 121, 122, 122, 120, 120, 121, 121, 122, 122,
                123, 123, 120, 121, 121, 122, 122, 123, 123, 124});
        byte[] expectedCr = toByteArrayShifted(new int[]{137, 136, 136, 135, 135, 134, 134, 133, 137, 136, 136, 135, 135, 134,
                133, 133, 137, 136, 136, 135, 135, 134, 133, 133, 137, 136, 136, 135, 134, 134, 133, 133, 137, 136,
                136, 135, 134, 134, 133, 133, 137, 136, 135, 135, 134, 134, 133, 132, 137, 136, 135, 135, 134, 134,
                133, 132, 136, 136, 135, 135, 134, 133, 133, 132});

        byte[] leftCb = toByteArrayShifted(new int[]{116, 116, 116, 116, 119, 119, 119, 119});
        byte[] tlCb = toByteArrayShifted(new int[]{113});
        byte[] topCb = toByteArrayShifted(new int[]{118, 118, 118, 118, 119, 119, 119, 119});

        byte[] leftCr = toByteArrayShifted(new int[]{137, 137, 137, 137, 138, 138, 138, 138});
        byte[] tlCr = toByteArrayShifted(new int[]{141});
        byte[] topCr = toByteArrayShifted(new int[]{132, 132, 132, 132, 132, 132, 132, 132});

        byte[] actualCb = new byte[64];
        byte[] actualCr = new byte[64];
        ChromaPredictionBuilder.predictPlane(emptyResidual, 0, true, true, leftCb, topCb, tlCb, actualCb);
        ChromaPredictionBuilder.predictPlane(emptyResidual, 0, true, true, leftCr, topCr, tlCr, actualCr);

        assertArrayEquals(expectedCb, actualCb);
        assertArrayEquals(expectedCr, actualCr);
    }
}