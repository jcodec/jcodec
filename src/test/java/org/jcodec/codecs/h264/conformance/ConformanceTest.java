package org.jcodec.codecs.h264.conformance;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.StringUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.platform.Platform;
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
import static org.junit.Assert.assertTrue;

public class ConformanceTest {

    @Test
    public void testNoContainer() throws IOException {
        String dir = "src/test/resources/video/seq_h264_4";
        String yuv = "src/test/resources/video/seq_h264_4.yuv";


        String info = Platform.stringFromBytes(readFile(dir + "/info.txt").array());
        int width = parseInt(info.split(" ")[0]);
        int height = parseInt(info.split(" ")[1]);
        int frameCount = parseInt(info.split(" ")[2]);

        byte[][] picData = Picture.create(width, height, ColorSpace.YUV420J).getData();

        H264Decoder decoder = new H264Decoder();
        decoder.addSps(singletonList(readFile(dir + "/sps")));
        decoder.addPps(singletonList(readFile(dir + "/pps")));

        List<List<ByteBuffer>> frames = new ArrayList<List<ByteBuffer>>();

        for (int fn = 0; fn < frameCount; fn++) {
            ByteBuffer buf = readFile(dir + "/" + StringUtils.zeroPad3(fn));
            frames.add(extractNALUnits(buf));
        }

        RawReader rawReader = new RawReader(new File(yuv), width, height);
        for (int fn = 0; fn < frameCount; fn++) {
            Picture expected = rawReader.readNextFrame();
            Picture actual = decoder.decodeFrameFromNals(duplicateBuffers(frames.get(fn)), picData);
            if (expected == null) {
                break;
            }
            assertTrue("frame=" + fn + " FAILED", ConformanceTestTool.compare(expected, actual));
        }
    }

    @Test
    public void testMp4Container() throws IOException {
        String dir = "src/test/resources/video/seq_h264_4.mp4";
        String yuv = "src/test/resources/video/seq_h264_4.yuv";

        MP4Demuxer mp4Demuxer = MP4Demuxer.createMP4Demuxer(NIOUtils.readableChannel(new File(dir)));
        DemuxerTrack videoTrack = mp4Demuxer.getVideoTrack();
        DemuxerTrackMeta meta = videoTrack.getMeta();
        H264Decoder decoder = H264Decoder.createH264DecoderFromCodecPrivate(meta.getCodecPrivate());
        int width = meta.getVideoCodecMeta().getSize().getWidth();
        int height = meta.getVideoCodecMeta().getSize().getHeight();
        Picture tmp = Picture.create(width, height, ColorSpace.YUV420);
        RawReader rawReader = new RawReader(new File(yuv), width, height);

        for (int fn = 0; fn < meta.getTotalFrames(); fn++) {
            Packet pkt = videoTrack.nextFrame();
            Picture pic = decoder.decodeFrame(pkt.getData(), tmp.getData());

            Picture ref = rawReader.readNextFrame();
            if (ref == null)
                break;
            assertTrue("frame=" + fn + " FAILED", ConformanceTestTool.compare(ref, pic));
        }

        mp4Demuxer.close();
    }


    public static ByteBuffer readFile(String path) throws IOException {
        File file = new File(path);
        InputStream _in = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[(int) file.length()];
        _in.read(buf);
        _in.close();
        return ByteBuffer.wrap(buf);
    }


    public static List<ByteBuffer> extractNALUnits(ByteBuffer buf) {
        buf = buf.duplicate();
        List<ByteBuffer> nalUnits = new ArrayList<ByteBuffer>();

        while (buf.remaining() > 4) {
            int length = buf.getInt();
            ByteBuffer nalUnit = ByteBuffer.allocate(length);
            for (int i = 0; i < length; i++) {
                nalUnit.put(buf.get());
            }
            nalUnit.flip();
            nalUnits.add(nalUnit);
        }

        return nalUnits;
    }

    public static List<ByteBuffer> duplicateBuffers(List<ByteBuffer> bufs) {
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();

        for (ByteBuffer buf : bufs)
            result.add(buf.duplicate());

        return result;
    }
}
