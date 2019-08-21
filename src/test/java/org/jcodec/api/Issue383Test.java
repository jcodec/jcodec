package org.jcodec.api;

import static org.jcodec.api.FrameGrab.createFrameGrab;
import static org.jcodec.common.io.NIOUtils.readableChannel;

import java.io.File;

import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

public class Issue383Test {
    @Test
    public void testFrameGrabCanExtractOrientation() throws Exception {
        FrameGrab fg = createFrameGrab(readableChannel(new File("src/test/resources/issue383/sample.mp4")));
        Picture picture = fg.getNativeFrame();
        Assert.assertNotNull(picture);
    }
}
