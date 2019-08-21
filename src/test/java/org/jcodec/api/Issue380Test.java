package org.jcodec.api;

import static org.jcodec.api.FrameGrab.createFrameGrab;
import static org.jcodec.common.io.NIOUtils.readableChannel;

import java.io.File;

import org.jcodec.common.model.Picture;
import org.junit.Test;

public class Issue380Test {
    @Test (expected = RuntimeException.class)
    public void testFrameGrabCanExtractOrientation() throws Exception {
        FrameGrab fg = createFrameGrab(readableChannel(new File("src/test/resources/issue380/squirrel-720x576-444P.mp4")));
        Picture picture = fg.getNativeFrame();
    }
}
