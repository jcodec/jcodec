package org.jcodec.api;

import static org.jcodec.common.Format.MOV;
import static org.jcodec.common.Tuple._3;
import static org.jcodec.common.model.ColorSpace.RGB;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import org.jcodec.api.transcode.Filter;
import org.jcodec.api.transcode.PixelStore;
import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.api.transcode.Sink;
import org.jcodec.api.transcode.SinkImpl;
import org.jcodec.api.transcode.Source;
import org.jcodec.api.transcode.SourceImpl;
import org.jcodec.api.transcode.Transcoder;
import org.jcodec.api.transcode.Transcoder.TranscoderBuilder;
import org.jcodec.common.Codec;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.junit.Test;

public class TranscoderTest {
    final class CustomFilter implements Filter {
        @Override
        public ColorSpace getOutputColor() {
            return RGB;
        }

        @Override
        public ColorSpace getInputColor() {
            return RGB;
        }

        @Override
        public LoanerPicture filter(Picture picture, PixelStore store) {
            BufferedImage rgb = AWTUtil.toBufferedImage(picture);
            Graphics g = rgb.getGraphics();
            g.drawString("hello", 42, 43);

            LoanerPicture tmpRGB = store.getPicture(rgb.getWidth(), rgb.getHeight(), RGB);
            AWTUtil.fromBufferedImage(rgb, tmpRGB.getPicture());

            return tmpRGB;
        }
    }

    @Test
    public void canFilterVideoAndCopyAudio() throws Exception {
        new File("tmp").mkdirs();

        File input = new File("src/test/resources/video/seq_h264_4_audio.mp4");
        File output = new File("tmp/canFilterVideoAndCopyAudio.mp4");

        Source source = SourceImpl.create(input.getAbsolutePath());
        Sink sink = new SinkImpl(output.getAbsolutePath(), MOV, Codec.H264, Codec.AAC);

        TranscoderBuilder builder = Transcoder.newTranscoder();
        builder.addSource(source);
        builder.addSink(sink);
        builder.addFilter(0, new CustomFilter());
        builder.setAudioMapping(0, 0, true);
        builder.setVideoMapping(0, 0, false);

        Transcoder transcoder = builder.create();
        transcoder.transcode();
    }
}
