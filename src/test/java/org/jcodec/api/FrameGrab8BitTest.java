package org.jcodec.api;

import java.io.File;

import org.jcodec.common.AutoFileChannelWrapper;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Test;

public class FrameGrab8BitTest {
    @Test
    public void testPerformance() throws Exception {
        File file = new File("/Users/zhukov/vgplayer_ng/thumbs_g1.mov");
        //        File file = new File("/Users/zhukov/vgplayer_ng/thumbs_g100.mov");
        //        File file = new File(
        //                "/Users/zhukov/testdata/proxies/itsallgonepetetong_2005_hd_16x9_235_2398_english_2091_JPEG2000/thumbshq.mov");
        SeekableByteChannel in = new AutoFileChannelWrapper(file);
        FrameGrab8Bit fg = new FrameGrab8Bit(in);
        long start = System.currentTimeMillis();
        int frames = 10000;
        for (int i = 0; i < frames; i++) {
            Picture8Bit nativeFrame = fg.getNativeFrame();
            System.out.println(nativeFrame);
        }
        long time = System.currentTimeMillis() - start;
        long fps = frames * 1000L / time;
        System.out.println(fps + "fps");
    }

}
