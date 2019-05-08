package org.jcodec.containers.mkv;

import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class MkvExtractSubsTest {
    @Test
    public void testExampleSubs() throws IOException {
        long expectedPts[] = {1549L, 3757L, 4715L};
        String expectedText[] = {"...the colossus of Rhodes!", "No!", "The colossus of Rhodes\r\nand it is here just for you Proog."};

        MKVDemuxer demuxer = new MKVDemuxer(new AutoFileChannelWrapper(new File("src/test/resources/mkv/subs.mkv")));
        DemuxerTrack track = demuxer.getSubtitleTracks().get(0);
        Packet packet;
        while (null != (packet = track.nextFrame())) {
            String text = takeString(packet.getData());
            int fn = (int) packet.frameNo;
            System.out.println(fn + " " + packet.pts + " " + text);
            assertEquals(expectedPts[fn], packet.pts);
            assertEquals(expectedText[fn], text);
        }
    }

    private String takeString(ByteBuffer data) throws UnsupportedEncodingException {
        int remaining = data.remaining();
        byte b[] = new byte[remaining];
        data.get(b);
        return new String(b, "UTF-8");
    }
}
