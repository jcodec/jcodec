package org.jcodec.api.awt;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

import org.jcodec.api.JCodecException;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4Packet;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

public class FrameGrab8BitTest {

    private static final Class<? extends byte[][]> BYTE_DOUBLE_ARRAY_CLASS = new byte[0][0].getClass();

    @Test
    public void testGetFrame() throws IOException {

        ContainerAdaptor adaptor = mock(ContainerAdaptor.class);
        SeekableDemuxerTrack videoTrack = mock(SeekableDemuxerTrack.class);

        AWTFrameGrab8Bit grab = new AWTFrameGrab8Bit(videoTrack, adaptor);
        Picture8Bit pic = Picture8Bit.create(16, 16, ColorSpace.YUV420J);

        Arrays.fill(pic.getPlaneData(0), (byte) (169 - 128));
        Arrays.fill(pic.getPlaneData(1), (byte) (45 - 128));
        Arrays.fill(pic.getPlaneData(2), (byte) (103 - 128));

        MP4Packet packet = new MP4Packet(null, 0, 25, 1, 0, true, null, 0, 0, 0, 0, 42, true);
        when(adaptor.decodeFrame8Bit(any(Packet.class), any(BYTE_DOUBLE_ARRAY_CLASS))).thenReturn(pic);
        when(videoTrack.nextFrame()).thenReturn(packet);

        BufferedImage res = grab.getFrame();

        verify(videoTrack).nextFrame();
        verify(adaptor).decodeFrame8Bit(eq(packet), any(BYTE_DOUBLE_ARRAY_CLASS));

        for (int i = 0; i < 256; i++) {
            int rgb = res.getRGB(i % 16, i / 16);
            Assert.assertEquals(134, (rgb >> 16) & 0xff);
            Assert.assertEquals(215, (rgb >> 8) & 0xff);
            Assert.assertEquals(22, (rgb) & 0xff);
        }
    }

    @Test
    public void testSeekSloppy() throws IOException, JCodecException {
        ContainerAdaptor adaptor = mock(ContainerAdaptor.class);
        SeekableDemuxerTrack videoTrack = mock(SeekableDemuxerTrack.class);

        AWTFrameGrab8Bit grab = new AWTFrameGrab8Bit(videoTrack, adaptor);

        int[] keyFrames = new int[] { 0, 11, 25, 34, 48, 59, 100 };
        DemuxerTrackMeta meta = new DemuxerTrackMeta(TrackType.VIDEO, Codec.H264, 120, keyFrames, 120, null,
                new VideoCodecMeta(new Size(320, 240)), null);

        when(videoTrack.getMeta()).thenReturn(meta);
        when(videoTrack.getCurFrame()).thenReturn(42L);

        grab.seekToFrameSloppy(42);

        verify(videoTrack).gotoFrame(42);
        verify(videoTrack).gotoFrame(34);
    }

    @Test
    public void testSeekPrecise() throws IOException, JCodecException {
        ContainerAdaptor adaptor = mock(ContainerAdaptor.class);
        SeekableDemuxerTrack videoTrack = mock(SeekableDemuxerTrack.class);

        AWTFrameGrab8Bit grab = new AWTFrameGrab8Bit(videoTrack, adaptor);

        int[] keyFrames = new int[] { 0, 11, 25, 40, 48, 59, 100 };
        DemuxerTrackMeta meta = new DemuxerTrackMeta(TrackType.VIDEO, Codec.H264, 120, keyFrames, 120, null,
                new VideoCodecMeta(new Size(320, 240)), null);

        MP4Packet frame40 = new MP4Packet(null, 40, 25, 1, 40, true, null, 0, 0, 0, 0, 42, true);
        MP4Packet frame41 = new MP4Packet(null, 41, 25, 1, 41, true, null, 0, 0, 0, 0, 42, true);
        MP4Packet frame42 = new MP4Packet(null, 42, 25, 1, 42, true, null, 0, 0, 0, 0, 42, true);

        when(videoTrack.getMeta()).thenReturn(meta);
        when(videoTrack.getCurFrame()).thenReturn(42L);
        when(videoTrack.nextFrame()).thenReturn(frame40).thenReturn(frame41).thenReturn(frame42);

        grab.seekToFramePrecise(42);

        InOrder o = inOrder(adaptor, videoTrack);
        o.verify(adaptor).decodeFrame8Bit(eq(frame40), any(BYTE_DOUBLE_ARRAY_CLASS));
        o.verify(adaptor).decodeFrame8Bit(eq(frame41), any(BYTE_DOUBLE_ARRAY_CLASS));
    }
}
