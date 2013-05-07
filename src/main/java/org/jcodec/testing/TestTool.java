package org.jcodec.testing;

import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;
import static org.jcodec.common.JCodecUtil.getAsIntArray;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.MP4Demuxer.MP4DemuxerTrack;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.LeafBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

public class TestTool {

    private String jm;
    private File coded;
    private File decoded;
    private File jmconf;
    private File errs;

    public TestTool(String jm, String errs) throws IOException {
        this.jm = jm;
        this.errs = new File(errs);

        coded = File.createTempFile("seq", ".264");
        decoded = File.createTempFile("seq_dec", ".yuv");
        jmconf = File.createTempFile("ldecod", ".conf");

        prepareJMConf();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("JCodec h.264 test tool");
            System.out.println("Syntax: <path to ldecod> <movie file> <foder for errors>");
            return;
        }

        new TestTool(args[0], args[2]).doIt(args[1]);
    }

    private void doIt(String in) throws Exception {
        SeekableByteChannel raw = null;
        SeekableByteChannel source = null;
        try {
            source = new FileChannelWrapper(new File(in));

            MP4Demuxer demux = new MP4Demuxer(source);

            H264Decoder decoder = new H264Decoder();

            MP4DemuxerTrack inTrack = demux.getVideoTrack();

            VideoSampleEntry ine = (VideoSampleEntry) inTrack.getSampleEntries()[0];
            AvcCBox avcC = Box.as(AvcCBox.class, Box.findFirst(ine, LeafBox.class, "avcC"));
            
            ByteBuffer _rawData = ByteBuffer.allocate(1920*1088*6);

            decoder.addSps(avcC.getSpsList());
            decoder.addPps(avcC.getPpsList());

            Packet inFrame;
            List<Picture> decodedPics = new ArrayList<Picture>();
            int totalFrames = (int) inTrack.getFrameCount(), seqNo = 0;
            for (int i = 0; (inFrame = inTrack.getFrames(1)) != null; i++) {
                ByteBuffer data = inFrame.getData();
                List<ByteBuffer> nalUnits = splitMOVPacket(data, avcC);
                _rawData.clear();
                H264Utils.joinNALUnits(nalUnits, _rawData);
                _rawData.flip();
                
                if (H264Utils.idrSlice(_rawData)) {
                    if (raw != null) {
                        raw.close();
                        runJMCompareResults(decodedPics, seqNo);
                        decodedPics = new ArrayList<Picture>();
                        seqNo = i;
                    }
                    raw = new FileChannelWrapper(coded);
                    H264Utils.saveStreamParams(avcC, raw);
                }
                raw.write(_rawData);

                decodedPics.add(decoder.decodeFrame(nalUnits,
                        Picture.create((ine.getWidth() + 15) & ~0xf, (ine.getHeight() + 15) & ~0xf, ColorSpace.YUV420)
                                .getData()));
                if (i % 500 == 0)
                    System.out.println((i * 100 / totalFrames) + "%");
            }
            if (decodedPics.size() > 0)
                runJMCompareResults(decodedPics, seqNo);
        } finally {
            if (source != null)
                source.close();
            if (raw != null)
                raw.close();
        }
    }

    private void runJMCompareResults(List<Picture> decodedPics, int seqNo) throws Exception {

        Process process = Runtime.getRuntime().exec(jm + " -d " + jmconf.getAbsolutePath());
        process.waitFor();

        ByteBuffer yuv = NIOUtils.fetchFrom(decoded);
        for (Picture pic : decodedPics) {
            pic = pic.cropped();
            boolean equals = Arrays.equals(getAsIntArray(yuv, pic.getPlaneWidth(0) * pic.getPlaneHeight(0)),
                    pic.getPlaneData(0));
            equals &= Arrays.equals(getAsIntArray(yuv, pic.getPlaneWidth(1) * pic.getPlaneHeight(1)),
                    pic.getPlaneData(1));
            equals &= Arrays.equals(getAsIntArray(yuv, pic.getPlaneWidth(2) * pic.getPlaneHeight(2)),
                    pic.getPlaneData(2));
            if (!equals) {
                System.out.println(seqNo + ": DIFF!!!");
                coded.renameTo(new File(errs, String.format("seq%08d.264", seqNo)));
                decoded.renameTo(new File(errs, String.format("seq%08d_dec.yuv", seqNo)));
            }
        }
    }

    private void prepareJMConf() throws IOException {
        InputStream cool = null;
        try {
            cool = getClass().getClassLoader().getResourceAsStream("org/jcodec/testing/jm.conf");
            String str = IOUtils.toString(cool);
            str = str.replace("%input_file%", coded.getAbsolutePath());
            str = str.replace("%output_file%", decoded.getAbsolutePath());
            FileUtils.writeStringToFile(jmconf, str);
        } finally {
            IOUtils.closeQuietly(cool);
        }
    }
}
