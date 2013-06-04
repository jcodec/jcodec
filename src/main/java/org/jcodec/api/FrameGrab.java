package org.jcodec.api;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jcodec.api.specific.AVCMP4Adaptor;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.JCodecUtil.Format;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts frames from a movie into uncompressed images suitable for processing.
 * 
 * Supports going to random points inside of a movie ( seeking ) by frame number of by second.
 * 
 * NOTE: Supports only AVC ( H.264 ) in MP4 ( ISO BMF, QuickTime ) at this point.
 * 
 * @author The JCodec project
 * 
 */
public class FrameGrab {

    private static final int KEYFRAME_TEST_STEP = 24;
    private DemuxerTrack videoTrack;
    private ContainerAdaptor decoder;
    private SeekableByteChannel ch;

    public FrameGrab(SeekableByteChannel in) throws IOException, JCodecException {
        this.ch = in;
        ByteBuffer header = ByteBuffer.allocate(65536);
        ch.read(header);
        header.flip();
        Format detectFormat = JCodecUtil.detectFormat(header);

        switch (detectFormat) {
        case MOV:
            MP4Demuxer demuxer = new MP4Demuxer(ch);
            videoTrack = demuxer.getVideoTrack();
            break;
        case MPEG_PS:
            throw new UnsupportedFormatException("MPEG PS is temporarily unsupported.");
        case MPEG_TS:
            throw new UnsupportedFormatException("MPEG TS is temporarily unsupported.");
        default:
            throw new UnsupportedFormatException("Container format is not supported by JCodec");
        }

        decodeLeadingFrames();
    }

    /**
     * Position frame grabber to a specific second in a movie. As a result the
     * next decoded frame will be precisely at the requested second.
     * 
     * WARNING: potentially very slow. Use only when you absolutely need precise
     * seek. Tries to seek to exactly the requested second and for this it might
     * have to decode a sequence of frames from the closes key frame. Depending
     * on GOP structure this may be as many as 500 frames.
     * 
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToSecondPrecise(double second) throws IOException, JCodecException {
        videoTrack.seek(second);
        decodeLeadingFrames();
        return this;
    }

    /**
     * Position frame grabber to a specific frame in a movie. As a result the
     * next decoded frame will be precisely the requested frame number.
     * 
     * WARNING: potentially very slow. Use only when you absolutely need precise
     * seek. Tries to seek to exactly the requested frame and for this it might
     * have to decode a sequence of frames from the closes key frame. Depending
     * on GOP structure this may be as many as 500 frames.
     * 
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToFramePrecise(int frameNumber) throws IOException, JCodecException {
        videoTrack.gotoFrame(frameNumber);
        decodeLeadingFrames();
        return this;
    }

    /**
     * Position frame grabber to a specific second in a movie.
     * 
     * Performs a sloppy seek, meaning that it may actually not seek to exact
     * second requested, instead it will seek to the closest key frame
     * 
     * NOTE: fast, as it just seeks to the closest previous key frame and
     * doesn't try to decode frames in the middle
     * 
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToSecondSloppy(double second) throws IOException, JCodecException {
        videoTrack.seek(second);
        goToPrevKeyframe();
        return this;
    }

    /**
     * Position frame grabber to a specific frame in a movie
     * 
     * Performs a sloppy seek, meaning that it may actually not seek to exact
     * frame requested, instead it will seek to the closest key frame
     * 
     * NOTE: fast, as it just seeks to the closest previous key frame and
     * doesn't try to decode frames in the middle
     * 
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public FrameGrab seekToFrameSloppy(int frameNumber) throws IOException, JCodecException {
        videoTrack.gotoFrame(frameNumber);
        goToPrevKeyframe();
        return this;
    }

    private void goToPrevKeyframe() throws IOException, JCodecException {
        Packet frame = videoTrack.nextFrame();
        int orig = (int) frame.getFrameNo();

        decoder = detectDecoder(videoTrack, frame);

        if (!frame.isKeyFrame() || !decoder.canSeek(frame)) {
            List<Packet> packets = new ArrayList<Packet>();
            int keyFrame = detectKeyFrame((int) frame.getFrameNo(), packets);
            videoTrack.gotoFrame(keyFrame == -1 ? 0 : keyFrame);
        } else
            videoTrack.gotoFrame(orig);
    }

    private void decodeLeadingFrames() throws IOException, JCodecException {
        Packet frame = videoTrack.nextFrame();
        int orig = (int) frame.getFrameNo();

        decoder = detectDecoder(videoTrack, frame);

        if (!frame.isKeyFrame() || !decoder.canSeek(frame)) {
            List<Packet> packets = new ArrayList<Packet>();
            int keyFrame = detectKeyFrame((int) frame.getFrameNo(), packets);
            if (keyFrame != -1) {
                Collections.sort(packets, Packet.FRAME_ASC);
                for (Iterator<Packet> it = packets.iterator(); it.hasNext() && it.next().getFrameNo() != keyFrame;)
                    it.remove();
                Picture buf = Picture.create(1920, 1088, ColorSpace.YUV444);
                for (Packet packet : packets) {
                    decoder.decodeFrame(packet, buf.getData());
                }
            } else
                orig = 0;
        }
        videoTrack.gotoFrame(orig);
    }

    private int detectKeyFrame(int start, List<Packet> packets) throws IOException {
        int keyFrame = -1;
        Packet frame;
        while (keyFrame == -1 && start > 0) {
            int prevStart = Math.max(start - KEYFRAME_TEST_STEP, 0);
            videoTrack.gotoFrame(prevStart);
            while (videoTrack.getCurFrame() < start) {
                frame = videoTrack.nextFrame();
                if (frame.isKeyFrame() && decoder.canSeek(frame))
                    keyFrame = (int) frame.getFrameNo();
                packets.add(frame);
            }
            start = prevStart;
        }
        return keyFrame;
    }

    private ContainerAdaptor detectDecoder(DemuxerTrack videoTrack, Packet frame) throws JCodecException {
        if (videoTrack instanceof AbstractMP4DemuxerTrack) {
            SampleEntry se = ((AbstractMP4DemuxerTrack) videoTrack).getSampleEntries()[((MP4Packet) frame).getEntryNo()];
            VideoDecoder byFourcc = byFourcc(se.getHeader().getFourcc());
            if (byFourcc instanceof H264Decoder)
                return new AVCMP4Adaptor(((AbstractMP4DemuxerTrack) videoTrack).getSampleEntries());
        }

        throw new UnsupportedFormatException("Codec is not supported");
    }

    private VideoDecoder byFourcc(String fourcc) {
        if (fourcc.equals("avc1")) {
            return new H264Decoder();
        } else if (fourcc.equals("m1v1") || fourcc.equals("m2v1")) {
            return new MPEGDecoder();
        } else if (fourcc.equals("apco") || fourcc.equals("apcs") || fourcc.equals("apcn") || fourcc.equals("apch")
                || fourcc.equals("ap4h")) {
            return new ProresDecoder();
        }
        return null;
    }

    /**
     * Get frame at current position in AWT image
     * 
     * @return
     * @throws IOException
     */
    public BufferedImage getFrame() throws IOException {
        return JCodecUtil.toBufferedImage(getNativeFrame());
    }

    /**
     * Get frame at current position in JCodec native image
     * 
     * @return
     * @throws IOException
     */
    public Picture getNativeFrame() throws IOException {
        Packet frames = videoTrack.nextFrame();
        Picture buffer = Picture.create(1920, 1088, ColorSpace.YUV444);
        return decoder.decodeFrame(frames, buffer.getData());
    }

    /**
     * Get frame at a specified second as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(File file, double second) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableFileChannel(file);
            return new FrameGrab(ch).seekToSecondPrecise(second).getFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified second as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws UnsupportedFormatException
     * @throws IOException
     */
    public static BufferedImage getFrame(SeekableByteChannel file, double second) throws JCodecException, IOException {
        return new FrameGrab(file).seekToSecondPrecise(second).getFrame();
    }

    /**
     * Get frame at a specified second as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrame(File file, double second) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableFileChannel(file);
            return new FrameGrab(ch).seekToSecondPrecise(second).getNativeFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified second as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrame(SeekableByteChannel file, double second) throws JCodecException, IOException {
        return new FrameGrab(file).seekToSecondPrecise(second).getNativeFrame();
    }

    /**
     * Get frame at a specified frame number as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(File file, int frameNumber) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableFileChannel(file);
            return new FrameGrab(ch).seekToFramePrecise(frameNumber).getFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified frame number as AWT image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static BufferedImage getFrame(SeekableByteChannel file, int frameNumber) throws JCodecException, IOException {
        return new FrameGrab(file).seekToFramePrecise(frameNumber).getFrame();
    }

    /**
     * Get frame at a specified frame number as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrame(File file, int frameNumber) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableFileChannel(file);
            return new FrameGrab(ch).seekToFramePrecise(frameNumber).getNativeFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    /**
     * Get frame at a specified frame number as JCodec image
     * 
     * @param file
     * @param second
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrame(SeekableByteChannel file, int frameNumber) throws JCodecException, IOException {
        return new FrameGrab(file).seekToFramePrecise(frameNumber).getNativeFrame();
    }
}