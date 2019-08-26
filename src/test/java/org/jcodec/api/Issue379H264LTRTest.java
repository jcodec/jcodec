package org.jcodec.api;

import static org.jcodec.api.FrameGrab.createFrameGrab;
import static org.jcodec.common.io.NIOUtils.readableChannel;

import java.io.File;

import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class Issue379H264LTRTest {
    @Test
    public void testFrameGrabCanExtractOrientation() throws Exception {
        FrameGrab fg = createFrameGrab(readableChannel(new File("src/test/resources/issue379/big_buck_bunny_160x90_ltr.mp4")));
        RawComparator cmp = new RawComparator("src/test/resources/issue379/big_buck_bunny_160x90_ltr.yuv", 2);
        for (int i = 0; i < 20; i++) {
            Picture picture = fg.getNativeFrame();
            Assert.assertTrue(cmp.nextFrame(picture));
        }
    }
}
