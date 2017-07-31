package org.jcodec.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Random;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.junit.Test;

public class Issue136Test {
    @Test
    public void testFrameGrabDoesNotThrowException() throws Exception {
        SeekableByteChannel _in = NIOUtils.readableChannel(new File("src/test/resources/issue136/20160603_163316.mp4"));
        FrameGrab fg = FrameGrab.createFrameGrab(_in);
        int totalFrames = fg.getVideoTrack().getMeta().getTotalFrames();
        assertEquals(151, totalFrames);

        Random rnd = new Random(4243);

        for (int i = 0; i < 10; i++) {
            int randomFrame = rnd.nextInt(1440);
            fg.seekToFramePrecise(randomFrame);
            Picture decoded = fg.getNativeFrame();
            assertNotNull(decoded);
        }
        _in.close();
    }
}
