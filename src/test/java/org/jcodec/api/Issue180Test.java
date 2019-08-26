package org.jcodec.api;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.Assert;
import org.junit.Test;

//https://github.com/jcodec/jcodec/issues/180
public class Issue180Test {

    @Test
    public void testFrameGrabDoesNotThrowException() throws Exception {
        SeekableByteChannel _in = NIOUtils.readableChannel(new File("src/test/resources/issue180/big_buck_bunny.mp4"));
        FrameGrab fg = FrameGrab.createFrameGrab(_in);
        int totalFrames = fg.getVideoTrack().getMeta().getTotalFrames();
        assertEquals(1440, totalFrames);

        fg.seekToFramePrecise(386);
        RawComparator cmp = new RawComparator("src/test/resources/issue180/frame_00000387.yuv", 2);
        Picture picture = fg.getNativeFrame();
        Assert.assertTrue(cmp.nextFrame(picture));

        _in.close();
    }
}
