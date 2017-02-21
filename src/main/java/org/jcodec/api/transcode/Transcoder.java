package org.jcodec.api.transcode;

import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.io.NIOUtils.writableFileChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.png.PNGDecoder;
import org.jcodec.codecs.png.PNGEncoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.raw.RAWVideoDecoder;
import org.jcodec.codecs.vpx.IVFMuxer;
import org.jcodec.codecs.vpx.VP8Decoder;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.wav.WavMuxer;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.AudioEncoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.Tuple._3;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.VideoEncoder.EncodedFrame;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.containers.imgseq.ImageSequenceDemuxer;
import org.jcodec.containers.imgseq.ImageSequenceMuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.webp.WebpDemuxer;
import org.jcodec.containers.y4m.Y4MDemuxer;
import org.jcodec.api.transcode.filters.ColorTransformFilter;

import net.sourceforge.jaad.aac.AACException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Transcoder core.
 * 
 * @author The JCodec project
 * 
 */
public class Transcoder {
    private static final int REORDER_BUFFER_SIZE = 7;

    private ThreadLocal<Picture8Bit> pixelBufferStore = new ThreadLocal<Picture8Bit>();
    private ThreadLocal<ByteBuffer> bufferStore = new ThreadLocal<ByteBuffer>();
    private Format inputFormat;
    private Format outputFormat;
    private _3<Integer, Integer, Codec> inputVideoCodec;
    private Codec outputVideoCodec;
    private _3<Integer, Integer, Codec> inputAudioCodec;
    private Codec outputAudioCodec;
    private int seekFrames;
    private int maxFrames;
    private String sourceName;
    private String destName;
    private boolean videoCodecCopy;
    private boolean audioCodecCopy;
    private List<Filter> extraFilters;

    private SeekableByteChannel sourceStream;
    private SeekableByteChannel destStream;
    private Demuxer demuxVideo;
    private Demuxer demuxAudio;
    private Muxer muxer;
    private MuxerTrack videoOutputTrack;
    private DemuxerTrack videoInputTrack;
    private DemuxerTrack audioInputTrack;
    private MuxerTrack audioOutputTrack;
    private AudioDecoder audioDecoder;
    private AudioEncoder audioEncoder;
    private VideoDecoder videoDecoder;
    private VideoEncoder videoEncoder;

    private String profile;
    private Boolean interlaced;
    private Integer downscale;

    private VideoCodecMeta videoCool;

    private List<Filter> filters = new ArrayList<Filter>();

    public Transcoder(String sourceName, String destName, Format inputFormat, Format outputFormat,
            _3<Integer, Integer, Codec> inputVideoCodec, Codec outputVideoCodec,
            _3<Integer, Integer, Codec> inputAudioCodec, Codec outputAudioCodec, boolean videoCodecCopy,
            boolean audioCodecCopy, List<Filter> extraFilters) {
        this.sourceName = sourceName;
        this.destName = destName;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.inputVideoCodec = inputVideoCodec;
        this.outputVideoCodec = outputVideoCodec;
        this.inputAudioCodec = inputAudioCodec;
        this.outputAudioCodec = outputAudioCodec;
        this.videoCodecCopy = videoCodecCopy;
        this.audioCodecCopy = audioCodecCopy;
        this.extraFilters = extraFilters;

        // Inferring video-only or audio-only output
        if (!outputFormat.isVideo()) {
            this.inputVideoCodec = null;
            this.outputVideoCodec = null;
        }
        if (!outputFormat.isAudio()) {
            this.inputAudioCodec = null;
            this.outputAudioCodec = null;
        }
    }

    public Format getInputFormat() {
        return inputFormat;
    }

    public Format getOutputFormat() {
        return outputFormat;
    }

    public _3<Integer, Integer, Codec> getIntputVideoCodec() {
        return inputVideoCodec;
    }

    public Codec getOutputVideoCodec() {
        return outputVideoCodec;
    }

    public _3<Integer, Integer, Codec> getInputAudioCode() {
        return inputAudioCodec;
    }

    public Codec getOutputAudioCodec() {
        return outputAudioCodec;
    }

    public int getSeekFrames() {
        return seekFrames;
    }

    public void setSeekFrames(int seekFrames) {
        this.seekFrames = seekFrames;
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    public void setMaxFrames(int maxFrames) {
        this.maxFrames = maxFrames;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setInterlaced(Boolean interlaced) {
        this.interlaced = interlaced;
    }

    public void setDownscale(int downscale) {
        this.downscale = downscale;
    }

    /**
     * Filters the decoded image before it gets to encoder.
     * 
     * @author stan
     */
    public static interface Filter {
        Picture8Bit filter(Picture8Bit picture, PixelStore store);
    }

    public static interface PixelStore {
        Picture8Bit getPicture(int width, int height, ColorSpace color);

        void putBack(Picture8Bit frame);
    }

    public static class PixelStoreImpl implements PixelStore {
        private List<Picture8Bit> buffers = new ArrayList<Picture8Bit>();

        @Override
        public Picture8Bit getPicture(int width, int height, ColorSpace color) {
            for (Picture8Bit picture8Bit : buffers) {
                if (picture8Bit.getWidth() == width && picture8Bit.getHeight() == height
                        && picture8Bit.getColor() == color) {
                    buffers.remove(picture8Bit);
                    return picture8Bit;
                }
            }
            return Picture8Bit.create(width, height, color);
        }

        @Override
        public void putBack(Picture8Bit frame) {
            frame.setCrop(null);
            buffers.add(frame);
        }
    }

    protected void initDecode(String sourceName) throws IOException {
        if (inputFormat != Format.IMG)
            sourceStream = readableFileChannel(sourceName);

        switch (inputFormat) {
        case MOV:
            demuxVideo = demuxAudio = new MP4Demuxer(sourceStream);
            break;
        case MKV:
            demuxVideo = demuxAudio = new MKVDemuxer(sourceStream);
            break;
        case IMG:
            demuxVideo = new ImageSequenceDemuxer(sourceName, maxFrames);
            break;
        case WEBP:
            demuxVideo = new WebpDemuxer(sourceStream);
            break;
        case MPEG_PS:
            demuxVideo = demuxAudio = new MPSDemuxer(sourceStream);
            break;
        case Y4M:
            Y4MDemuxer y4mDemuxer = new Y4MDemuxer(sourceStream);
            demuxVideo = demuxAudio = y4mDemuxer;
            videoInputTrack = y4mDemuxer;
            break;
        case MPEG_TS:
            MTSDemuxer mtsDemuxer = new MTSDemuxer(sourceStream);
            MPSDemuxer mpsDemuxer = null;
            if (inputVideoCodec != null) {
                mpsDemuxer = new MPSDemuxer(mtsDemuxer.getProgram(inputVideoCodec.v0));
                videoInputTrack = openTSTrack(mpsDemuxer, inputVideoCodec.v1);
                demuxVideo = mpsDemuxer;
            }
            if (inputAudioCodec != null) {
                if (inputVideoCodec == null || inputVideoCodec.v0 != inputAudioCodec.v0) {
                    mpsDemuxer = new MPSDemuxer(mtsDemuxer.getProgram(inputAudioCodec.v0));
                }
                audioInputTrack = openTSTrack(mpsDemuxer, inputAudioCodec.v1);
                demuxAudio = mpsDemuxer;
            }
            for (int pid : mtsDemuxer.getPrograms()) {
                if ((inputVideoCodec == null || pid != inputVideoCodec.v0)
                        && (inputAudioCodec == null || pid != inputAudioCodec.v0)) {
                    Logger.info("Unused program: " + pid);
                    mtsDemuxer.getProgram(pid).close();
                }
            }
        default:
            throw new RuntimeException("Input format: " + inputFormat + " is not supported.");
        }
        if (demuxVideo != null && inputVideoCodec != null) {
            List<? extends DemuxerTrack> videoTracks = demuxVideo.getVideoTracks();
            if (videoTracks.size() > 0) {
                videoInputTrack = videoTracks.get(inputVideoCodec.v1);
                DemuxerTrackMeta meta = videoInputTrack.getMeta();
                if (meta != null)
                    videoDecoder = createVideoDecoder(inputVideoCodec.v2, downscale, meta.getCodecPrivate(),
                            meta.getVideoCodecMeta());
            }
        }
        if (demuxAudio != null && inputAudioCodec != null) {
            List<? extends DemuxerTrack> audioTracks = demuxAudio.getAudioTracks();
            if (audioTracks.size() > 0) {
                audioInputTrack = audioTracks.get(inputAudioCodec.v1);
                DemuxerTrackMeta meta = audioInputTrack.getMeta();
                if (meta != null)
                    audioDecoder = createAudioDecoder(meta.getCodecPrivate());
            }
        }
    }

    private AudioDecoder createAudioDecoder(ByteBuffer codecPrivate) throws AACException {
        switch (inputAudioCodec.v2) {
        case AAC:
            return new AACDecoder(codecPrivate);
        }
        return null;
    }

    private VideoDecoder createVideoDecoder(Codec codec, int downscale, ByteBuffer codecPrivate,
            VideoCodecMeta videoCodecMeta) {
        switch (codec) {
        case H264:
            return H264Decoder.createH264DecoderFromCodecPrivate(codecPrivate);
        case PNG:
            return new PNGDecoder();
        case MPEG2:
            return MPEGDecoder.createMpegDecoder(downscale);
        case PRORES:
            return ProresDecoder.createProresDecoder(downscale);
        case VP8:
            return new VP8Decoder();
        case JPEG:
            return JpegDecoder.createJpegDecoder(downscale);
        case RAW:
            Size dim = videoCodecMeta.getSize();
            return new RAWVideoDecoder(dim.getWidth(), dim.getHeight());
        }
        return null;
    }

    private MPEGDemuxerTrack openTSTrack(MPSDemuxer demuxerVideo, Integer selectedTrack) {
        int trackNo = 0;
        for (MPEGDemuxerTrack track : demuxerVideo.getTracks()) {
            if (trackNo == selectedTrack) {
                return track;
            } else
                track.ignore();
            ++trackNo;
        }
        return null;
    }

    protected void initEncode(String destName) throws IOException {
        if (outputFormat != Format.IMG)
            destStream = writableFileChannel(destName);
        switch (outputFormat) {
        case MKV:
            muxer = new MKVMuxer(destStream);
            break;
        case MOV:
            muxer = MP4Muxer.createMP4MuxerToChannel(destStream);
            break;
        case IVF:
            muxer = new IVFMuxer(destStream);
            break;
        case IMG:
            muxer = new ImageSequenceMuxer(destName);
            break;
        case WAV:
            muxer = new WavMuxer(destStream);
            break;
        }
        if (outputVideoCodec != null) {
            switch (outputVideoCodec) {
            case PRORES:
                videoEncoder = new ProresEncoder(profile, interlaced);
                break;
            case H264:
                videoEncoder = H264Encoder.createH264Encoder();
                break;
            case VP8:
                videoEncoder = VP8Encoder.createVP8Encoder(10);
                break;
            case PNG:
                videoEncoder = new PNGEncoder();
                break;
            default:
                throw new RuntimeException("Could not find encoder for the codec: " + outputVideoCodec);
            }
            filters.add(0, new ColorTransformFilter(videoEncoder.getSupportedColorSpaces()[0]));
            filters.addAll(extraFilters);
        }
    }

    protected void finishEncode() throws IOException {
        muxer.finish();
        if (destStream != null) {
            IOUtils.closeQuietly(destStream);
        }
    }

    protected Picture8Bit createPixelBuffer(ByteBuffer firstFrame) {
        if (videoCool == null) {
            DemuxerTrackMeta meta = videoInputTrack.getMeta();
            if (meta != null && meta.getVideoCodecMeta() != null) {
                videoCool = meta.getVideoCodecMeta();
            } else {
                videoCool = videoDecoder.getCodecMeta(firstFrame);
            }
        }
        Size size = videoCool.getSize();
        return Picture8Bit.create((size.getWidth() + 15) & ~0xf, (size.getHeight() + 15) & ~0xf, videoCool.getColor());
    }

    protected Packet inputVideoPacket() throws IOException {
        if (videoInputTrack == null)
            return null;
        Packet nextFrame = videoInputTrack.nextFrame();
        if (nextFrame != null)
            Logger.debug(
                    String.format("Input frame: pts=%d, duration=%d", nextFrame.getPts(), nextFrame.getDuration()));
        if (videoDecoder == null) {
            videoDecoder = createVideoDecoder(inputVideoCodec.v2, downscale, nextFrame.getData(), null);
        }

        if (videoCodecCopy && videoOutputTrack == null) {
            VideoCodecMeta meta = videoDecoder.getCodecMeta(nextFrame.getData());
            ;
            videoOutputTrack = muxer.addVideoTrack(inputVideoCodec.v2, meta);
        }
        return nextFrame;
    }

    protected void outputVideoPacket(Packet packet) throws IOException {
        videoOutputTrack.addFrame(packet);
    }

    protected Picture8Bit decodeVideo(ByteBuffer data, Picture8Bit target1) {
        return videoDecoder.decodeFrame8Bit(data, target1.getData());
    }

    protected EncodedFrame encodeVideo(Picture8Bit frame, ByteBuffer _out) {
        if (videoOutputTrack == null) {
            videoOutputTrack = muxer.addVideoTrack(outputVideoCodec,
                    new VideoCodecMeta(new Size(frame.getWidth(), frame.getHeight()), frame.getColor()));
        }
        return videoEncoder.encodeFrame8Bit(frame, _out);
    }

    protected boolean haveAudio() {
        return audioInputTrack != null;
    }

    protected Packet inputAudioPacket() throws IOException {
        if (audioInputTrack == null)
            return null;
        Packet packet = audioInputTrack.nextFrame();
        if (audioDecoder == null) {
            audioDecoder = createAudioDecoder(packet.getData());
        }
        if (audioOutputTrack == null) {
            AudioCodecMeta meta = audioDecoder.getCodecMeta(packet.getData());
            audioOutputTrack = muxer.addAudioTrack(outputAudioCodec, meta);
        }
        return packet;
    }

    protected void outputAudioPacket(Packet audioPkt) throws IOException {
        audioOutputTrack.addFrame(audioPkt);
    }

    protected ByteBuffer decodeAudio(ByteBuffer audioPkt) throws IOException {
        if (inputAudioCodec.v2 == Codec.PCM) {
            AudioFormat format = audioInputTrack.getMeta().getAudioCodecMeta().getFormat();
            if (audioEncoder == null) {
                audioEncoder = createAudioEncoder(inputAudioCodec.v2, format);
            }
            return audioPkt;
        } else {
            AudioBuffer decodeFrame = audioDecoder.decodeFrame(audioPkt, null);
            if (audioOutputTrack == null) {
                this.audioOutputTrack = muxer.addAudioTrack(outputAudioCodec,
                        new AudioCodecMeta(decodeFrame.getFormat()));
            }
            if (audioEncoder == null) {
                audioEncoder = createAudioEncoder(outputAudioCodec, decodeFrame.getFormat());
            }
            return decodeFrame.getData();
        }
    }

    private AudioEncoder createAudioEncoder(Codec codec, AudioFormat format) {
        if (codec != Codec.PCM) {
            throw new RuntimeException("Only PCM audio encoding (RAW audio) is supported.");
        }
        return new RawAudioEncoder();
    }

    private static class RawAudioEncoder implements AudioEncoder {
        @Override
        public ByteBuffer encode(ByteBuffer audioPkt, ByteBuffer buf) {
            return audioPkt;
        }
    }

    protected ByteBuffer encodeAudio(ByteBuffer wrap) {
        return audioEncoder.encode(wrap, null);
    }

    protected boolean seek(int frame) throws IOException {
        Packet inFrame;

        if (videoInputTrack instanceof SeekableDemuxerTrack) {
            SeekableDemuxerTrack seekable = (SeekableDemuxerTrack) videoInputTrack;
            seekable.gotoFrame(frame);
            while ((inFrame = inputVideoPacket()) != null && !inFrame.isKeyFrame())
                ;
            seekable.gotoFrame(inFrame.getFrameNo());
        } else {
            Logger.error("Can not seek in " + videoInputTrack + " container.");
            return false;
        }
        return true;
    }

    protected int getBufferSize(Picture8Bit frame) {
        return videoEncoder.estimateBufferSize(frame);
    }

    protected boolean audioCodecCopy() {
        return audioCodecCopy;
    }

    protected boolean videoCodecCopy() {
        return videoCodecCopy;
    }

    private class FrameWithPacket implements Comparable<FrameWithPacket> {
        private Packet packet;
        private Picture8Bit frame;

        public FrameWithPacket(Packet inFrame, Picture8Bit dec2) {
            this.packet = inFrame;
            this.frame = dec2;
        }

        @Override
        public int compareTo(FrameWithPacket arg) {
            if (arg == null)
                return -1;
            else {
                long pts1 = packet.getPts();
                long pts2 = arg.packet.getPts();
                return pts1 > pts2 ? 1 : (pts1 == pts2 ? 0 : -1);
            }
        }
    }

    public void transcode() throws IOException {
        List<FrameWithPacket> reorderBuffer = new ArrayList<FrameWithPacket>();
        try {
            initDecode(sourceName);
            initEncode(destName);

            int skipFrames = 0;
            if (seekFrames > 0) {
                if (!seek(seekFrames)) {
                    Logger.warn("Unable to seek, will have to skip.");
                    skipFrames = seekFrames;
                }
            }
            if (maxFrames < Integer.MAX_VALUE)
                maxFrames += seekFrames;

            Packet inVideoPacket;
            boolean framesDecoded = false;
            PixelStore pixelsStore = new PixelStoreImpl();
            for (int frameNo = 0; (inVideoPacket = inputVideoPacket()) != null && frameNo <= maxFrames; frameNo++) {
                if (skipFrames > 0) {
                    skipFrames--;
                    continue;
                }
                if (!videoCodecCopy() && !framesDecoded) {
                    if (inVideoPacket.getFrameType() == FrameType.UNKOWN) {
                        detectFrameType(inVideoPacket);
                    }
                    if (!inVideoPacket.isKeyFrame())
                        continue;
                }
                framesDecoded = true;
                if (haveAudio()) {
                    double endPts = inVideoPacket.getPtsD() + 0.2;
                    outputAudioPacketsTo(endPts);
                }

                Picture8Bit decodedFrame = null;
                if (!videoCodecCopy()) {
                    Picture8Bit pixelBuffer = pixelBufferStore.get();
                    if (pixelBuffer == null) {
                        pixelBuffer = createPixelBuffer(inVideoPacket.getData());
                        pixelBufferStore.set(pixelBuffer);
                    }
                    decodedFrame = decodeVideo(inVideoPacket.getData(), pixelBuffer);
                    if (decodedFrame == null)
                        continue;
                    for (Filter filter : filters) {
                        decodedFrame = filter.filter(decodedFrame, pixelsStore);
                    }
                }
                printLegend(frameNo, maxFrames, inVideoPacket);
                if (reorderBuffer.size() > REORDER_BUFFER_SIZE) {
                    outFrames(reorderBuffer, pixelsStore, 1);
                }
                reorderBuffer.add(new FrameWithPacket(inVideoPacket, decodedFrame));
            }

            if (reorderBuffer.size() > 0) {
                outFrames(reorderBuffer, pixelsStore, reorderBuffer.size());
            }
            // Remaining audio packets
            outputAudioPacketsTo(Double.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // Logger.error("Error in transcode: " + e.getMessage());
        } finally {
            finishDecode();
            finishEncode();
        }
    }

    private void detectFrameType(Packet inVideoPacket) {
        if (inputVideoCodec.v2 != Codec.H264) {
            throw new RuntimeException("Input frame type detection is only supported for h.264");
        }
        inVideoPacket.setFrameType(
                H264Utils.isByteBufferIDRSlice(inVideoPacket.getData()) ? FrameType.KEY : FrameType.INTER);
    }

    private void finishDecode() {
        if (sourceStream != null)
            IOUtils.closeQuietly(sourceStream);
    }

    private void outputAudioPacketsTo(double endPts) throws IOException {
        Packet audioPkt;
        do {
            audioPkt = inputAudioPacket();
            if (audioPkt == null)
                break;
            if (!audioCodecCopy()) {
                ByteBuffer decodedAudio = decodeAudio(audioPkt.getData());
                outputAudioPacket(Packet.createPacketWithData(audioPkt, encodeAudio(decodedAudio)));
            } else {
                outputAudioPacket(audioPkt);
            }
        } while (audioPkt.getPtsD() < endPts);
    }

    private void printLegend(int frameNo, int maxFrames, Packet inVideoPacket) {
        if (frameNo % 100 == 0)
            System.out.print(String.format("[%6d]\r", frameNo));
    }

    private void outFrames(List<FrameWithPacket> frames, PixelStore pixelStore, int nFrames) throws IOException {
        long duration = findDuration(frames);
        System.out.println("\n" + duration);

        if (!videoCodecCopy)
            Collections.sort(frames);
        for (int i = 0; i < nFrames; i++) {
            FrameWithPacket frame = frames.remove(0);
            Packet outputVideoPacket;
            if (frame.frame != null) {
                ByteBuffer buffer = bufferStore.get();
                int bufferSize = getBufferSize(frame.frame);
                if (buffer == null || bufferSize < buffer.capacity()) {
                    buffer = ByteBuffer.allocate(bufferSize);
                    bufferStore.set(buffer);
                }
                buffer.clear();
                EncodedFrame enc = encodeVideo(frame.frame, buffer);
                pixelStore.putBack(frame.frame);
                outputVideoPacket = Packet.createPacketWithData(frame.packet, NIOUtils.clone(enc.getData()));
                outputVideoPacket.setFrameType(enc.isKeyFrame() ? FrameType.KEY : FrameType.INTER);
            } else {
                outputVideoPacket = Packet.createPacketWithData(frame.packet, NIOUtils.clone(frame.packet.getData()));
            }
            if (duration != -1)
                outputVideoPacket.setDuration(duration);
            outputVideoPacket(outputVideoPacket);
        }
    }

    private long findDuration(List<FrameWithPacket> frames) {
        long min = Long.MAX_VALUE;
        for (FrameWithPacket frame1 : frames) {
            long pts1 = frame1.packet.getPts();
            for (FrameWithPacket frame2 : frames) {
                long pts2 = frame2.packet.getPts();
                long duration = pts2 - pts1;
                if (duration > 0 && duration < min)
                    min = duration;
            }
        }
        return min;
    }
}