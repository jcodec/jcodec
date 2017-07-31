package org.jcodec.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Random;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.junit.Test;

//https://github.com/jcodec/jcodec/issues/180
public class Issue180Test {

    @Test
    public void testFrameGrabDoesNotThrowException() throws Exception {
        SeekableByteChannel _in = NIOUtils.readableChannel(new File("src/test/resources/issue180/big_buck_bunny.mp4"));
        FrameGrab fg = FrameGrab.createFrameGrab(_in);
        int totalFrames = fg.getVideoTrack().getMeta().getTotalFrames();
        assertEquals(1440, totalFrames);

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
