package org.jcodec.codecs.h264.conformance;

import static org.jcodec.common.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.Test;

public class ConformanceTest {

    @Test
    public void testNoContainer() throws IOException {
        String dir = "src/test/resources/video/seq_h264_4.mp4";
        String yuv = "src/test/resources/video/seq_h264_4.yuv";

        MP4Demuxer mp4Demuxer = MP4Demuxer.createMP4Demuxer(NIOUtils.readableChannel(new File(dir)));
        DemuxerTrack videoTrack = mp4Demuxer.getVideoTrack();
        DemuxerTrackMeta meta = videoTrack.getMeta();
        H264Decoder decoder = H264Decoder.createH264DecoderFromCodecPrivate(meta.getCodecMeta().getCodecPrivate());
        Size size = meta.getCodecMeta().video().getSize();
        int width = size.getWidth();
        int height = size.getHeight();
        Picture tmp = Picture.create(width, height, ColorSpace.YUV420);
        RawReader rawReader = new RawReader(new File(yuv), width, height);

        for (int fn = 0; fn < meta.getTotalFrames(); fn++) {
            Packet pkt = videoTrack.nextFrame();
            Picture pic = decoder.decodeFrame(pkt.getData(), tmp.getData());

            Picture ref = rawReader.readNextFrame();
            if (ref == null)
                break;
            assertTrue("frame=" + fn + " FAILED", compare(ref, pic));
        }

        mp4Demuxer.close();
    }

    private static boolean compare(Picture expected, Picture actual) {
        int size = expected.getWidth() * expected.getHeight();

        byte[] expY = expected.getPlaneData(0);
        byte[] actY = actual.getPlaneData(0);
        for (int i = 0; i < size; i++) {
            if (expY[i] != actY[i])
                return false;
        }

        byte[] expCb = expected.getPlaneData(1);
        byte[] actCb = actual.getPlaneData(1);
        for (int i = 0; i < (size >> 2); i++) {
            if (expCb[i] != actCb[i])
                return false;
        }

        byte[] expCr = expected.getPlaneData(2);
        byte[] actCr = actual.getPlaneData(2);
        for (int i = 0; i < (size >> 2); i++) {
            if (expCr[i] != actCr[i])
                return false;
        }

        return true;
    }
}
