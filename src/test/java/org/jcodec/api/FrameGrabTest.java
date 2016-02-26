package org.jcodec.api;

import static org.jcodec.Utils.picturesRoughlyEqual;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jcodec.Utils;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture8Bit;
import org.junit.Assert;
import org.junit.Test;

public class FrameGrabTest {
    private static final String SEQ_1_MP4 = "src/test/resources/video/seq_h264_1.mp4";
    private static final String SEQ_2_MP4 = "src/test/resources/video/seq_h264_2.mp4";
    private static final String SEQ_3_MP4 = "src/test/resources/video/seq_h264_3.mp4";

    private static final String SEQ_1_YUV = "src/test/resources/video/seq_1.yuv";
    private static final String SEQ_2_YUV = "src/test/resources/video/seq_2.yuv";
    private static final String SEQ_3_YUV = "src/test/resources/video/seq_3.yuv";

    private boolean saveImages = false;

    @Test
    public void testFrameGrab() throws IOException, JCodecException {
        compareOneSequence(SEQ_1_MP4, SEQ_1_YUV);
        compareOneSequence(SEQ_2_MP4, SEQ_2_YUV);
        compareOneSequence(SEQ_3_MP4, SEQ_3_YUV);
    }

    private void compareOneSequence(String compressed, String uncompressed) throws FileNotFoundException, IOException,
            JCodecException {
        FileChannelWrapper ch1 = null, ch2 = null;
        try {
            ch1 = NIOUtils.readableChannel(new File(compressed));
            ch2 = NIOUtils.readableChannel(new File(uncompressed));
            FrameGrab8Bit frameGrab1 = FrameGrab8Bit.createFrameGrab(ch1);

            PictureWithMetadata8Bit fr1;
            List<PictureWithMetadata8Bit> decoded = new ArrayList<PictureWithMetadata8Bit>();
            do {
                fr1 = frameGrab1.getNativeFrameWithMetadata();
                if (fr1 == null)
                    break;
                fr1 = new PictureWithMetadata8Bit(fr1.getPicture().cloneCropped(), fr1.getTimestamp(),
                        fr1.getDuration());
                decoded.add(fr1);
            } while (fr1 != null);

            Collections.sort(decoded, new Comparator<PictureWithMetadata8Bit>() {
                @Override
                public int compare(PictureWithMetadata8Bit o1, PictureWithMetadata8Bit o2) {
                    return o1.getTimestamp() < o2.getTimestamp() ? -1
                            : (o1.getTimestamp() == o2.getTimestamp() ? 0 : 1);
                }
            });

            for (PictureWithMetadata8Bit pic : decoded) {
                Frame frame = (Frame) pic.getPicture();
                Picture8Bit fr2 = Utils.readYuvFrame(ch2, frame.getWidth(), frame.getHeight());

                if (saveImages && Utils.maxDiff(frame, fr2) > 0) {
                    System.out.println(String.format("POC: %d, pts: %f", frame.getPOC(), pic.getTimestamp()));
                    Utils.saveImage(fr2, "png", String.format("/tmp/orig_%s_%f.%s", new File(compressed).getName(),
                            pic.getTimestamp(), "png"));
                    Utils.saveImage(frame, "png", String.format("/tmp/dec_%s_%f.%s", new File(compressed).getName(),
                            pic.getTimestamp(), "png"));

                    Utils.saveImage(Utils.diff(frame, fr2, 10), "png", String.format("/tmp/diff_%s_%f.%s", new File(
                            compressed).getName(), pic.getTimestamp(), "png"));
                }

                Assert.assertTrue(
                        String.format("Seq %s, poc %d, pts %f", compressed, frame.getPOC(), pic.getTimestamp()),
                        picturesRoughlyEqual(frame, fr2, 50));
            }
        } finally {
            IOUtils.closeQuietly(ch1);
            IOUtils.closeQuietly(ch2);
        }
    }
}