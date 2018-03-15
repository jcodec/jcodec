package org.jcodec.api;
import static org.jcodec.Utils.picturesRoughlyEqual;

import org.jcodec.Utils;
import org.jcodec.codecs.h264.io.model.Frame;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.System;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
            FrameGrab frameGrab1 = FrameGrab.createFrameGrab(ch1);

            PictureWithMetadata fr1;
            List<PictureWithMetadata> decoded = new ArrayList<PictureWithMetadata>();
            do {
                fr1 = frameGrab1.getNativeFrameWithMetadata();
                if (fr1 == null)
                    break;
                fr1 = PictureWithMetadata.createPictureWithMetadata(fr1.getPicture().cloneCropped(), fr1.getTimestamp(),
                        fr1.getDuration());
                decoded.add(fr1);
            } while (fr1 != null);

            Collections.sort(decoded, new Comparator<PictureWithMetadata>() {
                @Override
                public int compare(PictureWithMetadata o1, PictureWithMetadata o2) {
                    return o1.getTimestamp() < o2.getTimestamp() ? -1
                            : (o1.getTimestamp() == o2.getTimestamp() ? 0 : 1);
                }
            });

            for (PictureWithMetadata pic : decoded) {
                Frame frame = (Frame) pic.getPicture();
                Picture fr2 = Utils.readYuvFrame(ch2, frame.getWidth(), frame.getHeight());

                if (saveImages && Utils.maxDiff(frame, fr2) > 0) {
                    System.out.println(String.format("POC: %d, pts: %f", frame.getPOC(), pic.getTimestamp()));
                    Utils.saveImage(fr2, String.format("/tmp/orig_%s_%f.%s", new File(compressed).getName(),
                            pic.getTimestamp(), "png"));
                    Utils.saveImage(frame, String.format("/tmp/dec_%s_%f.%s", new File(compressed).getName(),
                            pic.getTimestamp(), "png"));

                    Utils.saveImage(Utils.diff(frame, fr2, 10), String.format("/tmp/diff_%s_%f.%s", new File(
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