package org.jcodec.codecs.h264.encode;

import org.jcodec.codecs.h264.io.model.SliceType;
import org.jcodec.common.model.Size;
import org.junit.Assert;
import org.junit.Test;

public class H264FixedRateControlTest {

    @Test
    public void testRateControl() {
        int[] initQp = { 26, 30, 26 };
        int[][] qpDelta = { { 0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0 },
                { 0, 2, 1, 0, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0 },
                { 0, 0, 1, 1, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1 } };
        int[][] bits = { { 1222, 1485, 1566, 810, 1110, 720, 1020, 660, 930, 870, 330, 510, 1102, 1073, 435 },
                { 1710, 416, 132, 627, 384, 1116, 1620, 1102, 784, 1026, 1378, 1014, 1066, 988, 1092 },
                { 910, 1144, 1053, 644, 504, 336, 837, 1326, 625, 744, 598, 1452, 525, 320, 969, } };
        for (int i = 0; i < 3; i++) {
            H264FixedRateControl rc = new H264FixedRateControl(1024);
            rc.reset();
            Assert.assertEquals(initQp[i],
                    rc.startPicture(new Size(0, 0), 0, i == 0 ? SliceType.I : (i == 1 ? SliceType.P : SliceType.B)));
            for (int j = 0; j < qpDelta[i].length; j++) {
                Assert.assertEquals(qpDelta[i][j], rc.initialQpDelta());
                Assert.assertEquals(0, rc.accept(bits[i][j]));
            }
        }
    }
}
