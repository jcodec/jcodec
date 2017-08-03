package org.jcodec.api;

import static org.jcodec.api.FrameGrab.createFrameGrab;
import static org.jcodec.common.DemuxerTrackMeta.Orientation.D_90;
import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jcodec.common.DemuxerTrackMeta.Orientation;
import org.junit.Test;

public class Issue49Test {
    @Test
    public void testFrameGrabCanExtractOrientation() throws Exception {
        FrameGrab fg = createFrameGrab(readableChannel(new File("src/test/resources/issue49/orientation.mp4")));
        Orientation orientation = fg.getVideoTrack().getMeta().getOrientation();
        assertEquals(D_90, orientation);
    }
}
