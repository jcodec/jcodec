package org.jcodec.api;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.api.specific.AVCFLVAdaptor;
import org.jcodec.api.specific.AVCMP4Adaptor;
import org.jcodec.api.specific.AVCMTSAdapter;
import org.jcodec.api.specific.CodecContainerAdaptor;
import org.jcodec.api.specific.IFrameOnlyAdapter;
import org.jcodec.api.specific.MPEGMP4Adapter;
import org.jcodec.api.specific.MPEGMPSAdapter;
import org.jcodec.api.specific.MPEGMTSAdapter;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.flv.FLVTrackDemuxer;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

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
 * NOTE: Supports only AVC ( H.264 ) in MP4 ( ISO BMF, QuickTime ) or FLV at
 * this point.
 * 
 * @author The JCodec project
 * 
 */
public class FrameGrab {

    private CodecContainerAdaptor adapter;

    public static class MediaInfo {
        private Size dim;

        public MediaInfo(Size dim) {
            super();
            this.dim = dim;
        }

        public Size getDim() {
            return dim;
        }

        public void setDim(Size dim) {
            this.dim = dim;
        }
    }

    private static Map<Format, Map<Codec, Class<? extends CodecContainerAdaptor>>> supported = new HashMap<Format, Map<Codec, Class<? extends CodecContainerAdaptor>>>() {
        {
            put(Format.MOV, new HashMap<Codec, Class<? extends CodecContainerAdaptor>>() {
                {
                    put(Codec.H264, AVCMP4Adaptor.class);
                    put(Codec.MPEG2, MPEGMP4Adapter.class);
                    put(Codec.PRORES, IFrameOnlyAdapter.class);
                    put(Codec.J2K, IFrameOnlyAdapter.class);
                    put(Codec.JPEG, IFrameOnlyAdapter.class);
                }
            });
            put(Format.MPEG_TS, new HashMap<Codec, Class<? extends CodecContainerAdaptor>>() {
                {
                    put(Codec.H264, AVCMTSAdapter.class);
                    put(Codec.MPEG2, MPEGMTSAdapter.class);
                }
            });
            put(Format.MPEG_PS, new HashMap<Codec, Class<? extends CodecContainerAdaptor>>() {
                {
                    put(Codec.MPEG2, MPEGMPSAdapter.class);
                }
            });
            put(Format.FLV, new HashMap<Codec, Class<? extends CodecContainerAdaptor>>() {
                {
                    put(Codec.H264, AVCFLVAdaptor.class);
                }
            });
            put(Format.AVI, new HashMap<Codec, Class<? extends CodecContainerAdaptor>>() {
                {
                }
            });
            put(Format.MKV, new HashMap<Codec, Class<? extends CodecContainerAdaptor>>() {
                {
                }
            });
        }
    };

    public FrameGrab(SeekableByteChannel in) throws IOException, JCodecException {
        ByteBuffer header = ByteBuffer.allocate(65536);
        in.read(header);
        header.flip();
        Format detectFormat = JCodecUtil.detectFormat(header);

        DemuxerTrack videoTrack;
        switch (detectFormat) {
        case MOV:
            videoTrack = new MP4Demuxer(in).getVideoTrack();
            break;
        case FLV:
            videoTrack = new FLVTrackDemuxer(in).getVideoTrack();
            break;
        case MPEG_PS:
            throw new UnsupportedFormatException("MPEG PS is temporarily unsupported.");
        case MPEG_TS:
            throw new UnsupportedFormatException("MPEG TS is temporarily unsupported.");
        case AVI:
            throw new UnsupportedFormatException("AVI is temporarily unsupported.");
        case MKV:
            throw new UnsupportedFormatException("MKV is temporarily unsupported.");
        default:
            throw new UnsupportedFormatException("Container format is not supported by JCodec");
        }
        Packet frame = videoTrack.nextFrame();
        Codec codec = JCodecUtil.detectDecoder(frame.getData());
        Map<Codec, Class<? extends CodecContainerAdaptor>> map = supported.get(detectFormat);
        if (map != null) {
            Class<? extends CodecContainerAdaptor> adapterClass = map.get(codec);
            try {
                adapter = adapterClass.getConstructor(DemuxerTrack.class).newInstance(videoTrack);
            } catch(Exception e) { 
                throw new JCodecException("Could not instantiate adapter", e);
            }
        }
    }

    public FrameGrab(CodecContainerAdaptor decoder) {
        this.adapter = decoder;
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
        adapter.seek(second);
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
        adapter.gotoFrame(frameNumber);
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
        adapter.seekToKeyFrame(second);
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
        adapter.gotoToKeyFrame(frameNumber);
        return this;
    }

    /**
     * Get frame at current position in JCodec native image
     * 
     * @return
     * @throws IOException
     */
    public Picture getNativeFrame() throws IOException {
        return adapter.nextFrame();
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

    /**
     * Get a specified frame by number from an already open demuxer track
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrame(SeekableDemuxerTrack vt, CodecContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return new FrameGrab(decoder).seekToFramePrecise(frameNumber).getNativeFrame();
    }

    /**
     * Get a specified frame by second from an already open demuxer track
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrame(SeekableDemuxerTrack vt, CodecContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return new FrameGrab(decoder).seekToSecondPrecise(second).getNativeFrame();
    }

    /**
     * Get a specified frame by number from an already open demuxer track (
     * sloppy mode, i.e. nearest keyframe )
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrameSloppy(SeekableDemuxerTrack vt, CodecContainerAdaptor decoder, int frameNumber)
            throws IOException, JCodecException {
        return new FrameGrab(decoder).seekToFrameSloppy(frameNumber).getNativeFrame();
    }

    /**
     * Get a specified frame by second from an already open demuxer track (
     * sloppy mode, i.e. nearest keyframe )
     * 
     * @param vt
     * @param decoder
     * @param frameNumber
     * @return
     * @throws IOException
     * @throws JCodecException
     */
    public static Picture getNativeFrameSloppy(SeekableDemuxerTrack vt, CodecContainerAdaptor decoder, double second)
            throws IOException, JCodecException {
        return new FrameGrab(decoder).seekToSecondSloppy(second).getNativeFrame();
    }

    /**
     * Gets info about the media
     * 
     * @return
     */
    public MediaInfo getMediaInfo() {
        return adapter.getMediaInfo();
    }
}