package org.jcodec.api;
import org.jcodec.api.specific.AVCMP4Adaptor;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.JCodecUtil.Format;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

import js.io.File;
import js.io.IOException;
import js.lang.NullPointerException;
import js.lang.ThreadLocal;
import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Extracts frames from a movie into uncompressed images suitable for
 * processing.
 * 
 * Supports going to random points inside of a movie ( seeking ) by frame number
 * of by second.
 * 
 * NOTE: Supports only AVC ( H.264 ) in MP4 ( ISO BMF, QuickTime ) at this
 * point.
 * 
 * @author The JCodec project
 * 
 */
@Deprecated
public class FrameGrab {

    private SeekableDemuxerTrack videoTrack;
    private ContainerAdaptor decoder;
    private ThreadLocal<int[][]> buffers;

    private static int _detectKeyFrame(DemuxerTrack videoTrack, int start) throws IOException {
        int[] seekFrames = videoTrack.getMeta().getSeekFrames();
        if (seekFrames == null)
            return start;
        int prev = seekFrames[0];
        for (int i = 1; i < seekFrames.length; i++) {
            if (seekFrames[i] > start)
                break;
            prev = seekFrames[i];
        }
        return prev;
    }

    private static ContainerAdaptor _detectDecoder(SeekableDemuxerTrack sdt, Packet frame) throws JCodecException {
        DemuxerTrackMeta meta = sdt.getMeta();
        switch (meta.getCodec()) {
        case H264:
            return new AVCMP4Adaptor(meta);
        default:
            throw new UnsupportedFormatException("Codec is not supported");
        }
    }

    static ContainerAdaptor detectDecoder(SeekableDemuxerTrack sdt) throws IOException, JCodecException {
        int curFrame = (int) sdt.getCurFrame();
        int keyFrame = _detectKeyFrame(sdt, curFrame);
        sdt.gotoFrame(keyFrame);

        Packet frame = sdt.nextFrame();
        ContainerAdaptor decoder = _detectDecoder(sdt, frame);
        return decoder;
    }

    public static FrameGrab createFrameGrab(SeekableByteChannel _in) throws IOException, JCodecException {
        ByteBuffer header = ByteBuffer.allocate(65536);
        _in.read(header);
        header.flip();
        Format detectFormat = JCodecUtil.detectFormatBuffer(header);
        SeekableDemuxerTrack videoTrack = null;

        switch (detectFormat) {
        case MOV:
            MP4Demuxer d1 = new MP4Demuxer(_in);
            videoTrack = d1.getVideoTrack();
            break;
        case MPEG_PS:
            throw new UnsupportedFormatException("MPEG PS is temporarily unsupported.");
        case MPEG_TS:
            throw new UnsupportedFormatException("MPEG TS is temporarily unsupported.");
        default:
            throw new UnsupportedFormatException("Container format is not supported by JCodec");
        }
        return new FrameGrab(videoTrack, detectDecoder(videoTrack));
    }

    public FrameGrab(SeekableDemuxerTrack videoTrack, ContainerAdaptor decoder) {
        if (decoder == null || videoTrack == null) {
            throw new NullPointerException();
        }
        this.buffers = new ThreadLocal<int[][]>();
        this.videoTrack = videoTrack;
        this.decoder = decoder;
    }

    private SeekableDemuxerTrack sdt() throws JCodecException {
        if (!(videoTrack instanceof SeekableDemuxerTrack))
            throw new JCodecException("Not a seekable track");

        return (SeekableDemuxerTrack) videoTrack;
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
        sdt().seek(second);
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
        sdt().gotoFrame(frameNumber);
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
        sdt().seek(second);
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
        sdt().gotoFrame(frameNumber);
        goToPrevKeyframe();
        return this;
    }

    private void goToPrevKeyframe() throws IOException, JCodecException {
        sdt().gotoFrame(detectKeyFrame((int) sdt().getCurFrame()));
    }

    private void decodeLeadingFrames() throws IOException, JCodecException {
        SeekableDemuxerTrack sdt = sdt();

        int curFrame = (int) sdt.getCurFrame();
        int keyFrame = detectKeyFrame(curFrame);
        sdt.gotoFrame(keyFrame);

        Packet frame = sdt.nextFrame();

        while (frame.getFrameNo() < curFrame) {
            decoder.decodeFrame(frame, getBuffer());
            frame = sdt.nextFrame();
        }
        sdt.gotoFrame(curFrame);
    }

    private int[][] getBuffer() {
        int[][] buf = buffers.get();
        if (buf == null) {
            buf = decoder.allocatePicture();
            buffers.set(buf);
        }
        return buf;
    }

    private int detectKeyFrame(int start) throws IOException {
        int[] seekFrames = videoTrack.getMeta().getSeekFrames();
        if (seekFrames == null)
            return start;
        int prev = seekFrames[0];
        for (int i = 1; i < seekFrames.length; i++) {
            if (seekFrames[i] > start)
                break;
            prev = seekFrames[i];
        }
        return prev;
    }

    /**
     * Get frame at current position in JCodec native image
     * 
     * @return
     * @throws IOException
     */
    public Picture getNativeFrame() throws IOException {
        Packet frame = videoTrack.nextFrame();
        if (frame == null)
            return null;

        return decoder.decodeFrame(frame, getBuffer());
    }

    /**
     * Gets info about the media
     * 
     * @return
     */
    public MediaInfo getMediaInfo() {
        return decoder.getMediaInfo();
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
    public static Picture getFrameFromChannel(SeekableByteChannel file, int frameNumber)
            throws JCodecException, IOException {
        return createFrameGrab(file).seekToFramePrecise(frameNumber).getNativeFrame();
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
    public static Picture getFrameFromFile(File file, int frameNumber) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return createFrameGrab(ch).seekToFramePrecise(frameNumber).getNativeFrame();
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
    public static Picture getFrameAtSecFromChannel(SeekableByteChannel file, double second)
            throws JCodecException, IOException {
        return createFrameGrab(file).seekToSecondPrecise(second).getNativeFrame();
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
    public static Picture getFrameAtSec(File file, double second) throws IOException, JCodecException {
        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            return createFrameGrab(ch).seekToSecondPrecise(second).getNativeFrame();
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    public SeekableDemuxerTrack getVideoTrack() {
        return videoTrack;
    }

    public ContainerAdaptor getDecoder() {
        return decoder;
    }
}