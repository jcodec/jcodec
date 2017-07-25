package org.jcodec.codecs.h264.conformance;

import org.jcodec.Utils;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.util.Collections.singletonList;
import static org.jcodec.common.Assert.assertTrue;

public class ConformanceTest {

    @Test
    public void testNoContainer() throws IOException {
        String dir = "src/test/resources/video/seq_h264_4.mp4";
        String yuv = "src/test/resources/video/seq_h264_4.yuv";

        MP4Demuxer mp4Demuxer = MP4Demuxer.createMP4Demuxer(NIOUtils.readableChannel(new File(dir)));
        DemuxerTrack videoTrack = mp4Demuxer.getVideoTrack();
        DemuxerTrackMeta meta = videoTrack.getMeta();
        H264Decoder decoder = H264Decoder.createH264DecoderFromCodecPrivate(meta.getCodecPrivate());
        int width = meta.getVideoCodecMeta().getSize().getWidth();
        int height = meta.getVideoCodecMeta().getSize().getHeight();
        Picture8Bit tmp = Picture8Bit.create(width, height, ColorSpace.YUV420);
        RawReader8Bit rawReader = new RawReader8Bit(new File(yuv), width, height);

        for (int fn = 0; fn < meta.getTotalFrames(); fn++) {
            Packet pkt = videoTrack.nextFrame();
            Picture8Bit pic = decoder.decodeFrame8Bit(pkt.getData(), tmp.getData());

            Picture8Bit ref = rawReader.readNextFrame();
            if (ref == null)
                break;
            assertTrue("frame=" + fn + " FAILED", compare(ref, pic));
        }

        mp4Demuxer.close();
    }

    private static boolean compare(Picture8Bit expected, Picture8Bit actual) {
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
