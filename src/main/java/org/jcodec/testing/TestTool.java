package org.jcodec.testing;
import static org.jcodec.common.ArrayUtil.toByteArrayShifted;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
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

    public static void main1(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("JCodec h.264 test tool");
            System.out.println("Syntax: <path to ldecod> <movie file> <foder for errors>");
            return;
        }

        new TestTool(args[0], args[2]).doIt(args[1]);
    }

    private void doIt(String _in) throws Exception {
        SeekableByteChannel raw = null;
        SeekableByteChannel source = null;
        try {
            source = new FileChannelWrapper(new FileInputStream(_in).getChannel());

            MP4Demuxer demux = MP4Demuxer.createMP4Demuxer(source);

            SeekableDemuxerTrack inTrack = (SeekableDemuxerTrack) demux.getVideoTrack();

            ByteBuffer _rawData = ByteBuffer.allocate(1920 * 1088 * 6);

            ByteBuffer codecPrivate = inTrack.getMeta().getCodecPrivate();
            H264Decoder decoder = H264Decoder.createH264DecoderFromCodecPrivate(codecPrivate);

            Packet inFrame;

            int sf = 2600;
            inTrack.gotoFrame(sf);
            while ((inFrame = inTrack.nextFrame()) != null && !inFrame.isKeyFrame())
                ;
            if (inFrame == null) {
                throw new NullPointerException("inFrame == null");
            }
            inTrack.gotoFrame(inFrame.getFrameNo());

            List<Picture> decodedPics = new ArrayList<Picture>();
            int totalFrames = (int) inTrack.getMeta().getTotalFrames(), seqNo = 0;
            for (int i = sf; (inFrame = inTrack.nextFrame()) != null; i++) {
                ByteBuffer data = inFrame.getData();
                List<ByteBuffer> nalUnits = H264Utils.splitFrame(data);
                _rawData.clear();
                H264Utils.joinNALUnitsToBuffer(nalUnits, _rawData);
                _rawData.flip();

                if (H264Utils.isByteBufferIDRSlice(_rawData)) {
                    if (raw != null) {
                        raw.close();
                        runJMCompareResults(decodedPics, seqNo);
                        decodedPics = new ArrayList<Picture>();
                        seqNo = i;
                    }
                    raw = new FileChannelWrapper(new FileOutputStream(coded).getChannel());
                    raw.write(codecPrivate);
                }
                if (raw == null) {
                    throw new IllegalStateException("IDR slice not found");
                }
                raw.write(_rawData);

                Size size = inTrack.getMeta().getVideoCodecMeta().getSize();

                decodedPics.add(decoder.decodeFrameFromNals(nalUnits, Picture
                        .create((size.getWidth() + 15) & ~0xf, (size.getHeight() + 15) & ~0xf, ColorSpace.YUV420)
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

        try {
            Process process = Runtime.getRuntime().exec(jm + " -d " + jmconf.getAbsolutePath());
            process.waitFor();

            ByteBuffer yuv = NIOUtils.fetchFromFile(decoded);
            for (Picture pic : decodedPics) {
                pic = pic.cropped();
                boolean equals = Platform.arrayEqualsByte(
                        toByteArrayShifted(JCodecUtil2.getAsIntArray(yuv, pic.getPlaneWidth(0) * pic.getPlaneHeight(0))),
                        pic.getPlaneData(0));
                equals &= Platform.arrayEqualsByte(
                        toByteArrayShifted(JCodecUtil2.getAsIntArray(yuv, pic.getPlaneWidth(1) * pic.getPlaneHeight(1))),
                        pic.getPlaneData(1));
                equals &= Platform.arrayEqualsByte(
                        toByteArrayShifted(JCodecUtil2.getAsIntArray(yuv, pic.getPlaneWidth(2) * pic.getPlaneHeight(2))),
                        pic.getPlaneData(2));
                if (!equals)
                    diff(seqNo);
            }
        } catch (Exception e) {
            diff(seqNo);
        }
    }

    private void diff(int seqNo) {
        System.out.println(seqNo + ": DIFF!!!");
        coded.renameTo(new File(errs, String.format("seq%08d.264", seqNo)));
        decoded.renameTo(new File(errs, String.format("seq%08d_dec.yuv", seqNo)));
    }

    private void prepareJMConf() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("InputFile              = \"").append(coded.getAbsolutePath()).append("\"\n");
        sb.append("OutputFile             = \"").append(decoded.getAbsolutePath()).append("\"\n");
        sb.append("RefFile                = \"/dev/null\"\n");
        sb.append("WriteUV                = 1\n");
        sb.append("FileFormat             = 0\n");
        sb.append("RefOffset              = 0\n");
        sb.append("POCScale               = 2\n");
        sb.append("DisplayDecParams       = 0\n");
        sb.append("ConcealMode            = 0\n");
        sb.append("RefPOCGap              = 2\n");
        sb.append("POCGap                 = 2\n");
        sb.append("Silent                 = 1\n");
        sb.append("IntraProfileDeblocking = 1\n");
        sb.append("DecFrmNum              = 0\n");
        sb.append("DecodeAllLayers        = 0\n");
        IOUtils.writeStringToFile(jmconf, sb.toString());
    }
}
