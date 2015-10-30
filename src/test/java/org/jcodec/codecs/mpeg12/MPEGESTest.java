package org.jcodec.codecs.mpeg12;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Packet;
import org.junit.Test;

public class MPEGESTest {

    @Test
    public void testES() throws IOException {

        byte[] mpeg = MPEGTestConst.mpeg();

        byte[] frame1 = MPEGTestConst.toBB(MPEGTestConst._mpegHeader, MPEGTestConst._mpegFrame);
        byte[] frame2 = MPEGTestConst.toBB(MPEGTestConst._mpegFrame);
        MPEGES mpeges = new MPEGES(Channels.newChannel(new ByteArrayInputStream(mpeg)), 32);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        Packet f1 = mpeges.getFrame(buf);
        assertArrayEquals(frame1, NIOUtils.toArray(f1.getData()));

        Packet f2 = mpeges.getFrame(buf);
        assertArrayEquals(frame1, NIOUtils.toArray(f2.getData()));

        Packet f3 = mpeges.getFrame(buf);
        assertArrayEquals(frame2, NIOUtils.toArray(f3.getData()));
    }
}
