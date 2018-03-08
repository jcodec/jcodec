package org.jcodec.platform;

import org.jcodec.api.transcode.filters.ScaleFilter;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlatformTest {
    @Test
    public void testNewInstance() {
        ScaleFilter scaleFilter = Platform.newInstance(ScaleFilter.class, new Object[]{42, 43});
        Size target = scaleFilter.getTarget();
        assertEquals(42, target.getWidth());
        assertEquals(43, target.getHeight());
    }

    static int parse(String str) {
        return Integer.parseInt(str);
    }

    static int parseBuf(ByteBuffer str) {
        byte[] dst = new byte[str.remaining()];
        str.get(dst);
        return Integer.parseInt(new String(dst));
    }


    @Test
    public void testInvokeStatic() {
        int parse = Platform.invokeStaticMethod(PlatformTest.class, "parse", new Object[]{"42"});
        assertEquals(42, parse);
    }

    @Test
    public void testInvokeStatic2() {
        int parseBuf = Platform.invokeStaticMethod(PlatformTest.class, "parseBuf", new Object[]{ByteBuffer.wrap("42".getBytes())});
        assertEquals(42, parseBuf);
    }

    @Test
    public void testAssignable() {
        assertTrue(Platform.isAssignableFrom(SampleEntry.class, VideoSampleEntry.class));
        assertFalse(Platform.isAssignableFrom(VideoSampleEntry.class, SampleEntry.class));
    }

}
