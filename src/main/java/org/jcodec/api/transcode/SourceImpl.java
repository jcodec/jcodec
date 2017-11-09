package org.jcodec.api.transcode;

import static org.jcodec.common.io.NIOUtils.readableFileChannel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.jaad.aac.AACException;

import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.h264.BufferH264ES;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.mpeg4.MPEG4Consts;
import org.jcodec.codecs.mpeg4.MPEG4Decoder;
import org.jcodec.codecs.png.PNGDecoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.raw.RAWVideoDecoder;
import org.jcodec.codecs.vpx.VP8Decoder;
import org.jcodec.codecs.wav.WavDemuxer;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.Tuple._3;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.imgseq.ImageSequenceDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mp3.MPEGAudioDemuxer;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.containers.webp.WebpDemuxer;
import org.jcodec.containers.y4m.Y4MDemuxer;

/**
 * A source producing uncompressed video/audio streams out of a compressed file.
 * 
 * The buffers for the frames coming out of this source will be borrowed from a
 * pixel store and must be returned to it for maximum efficiency.
 * 
 * @author Stanislav Vitvitskiy
 */
public class SourceImpl implements Source, PacketSource {
    private String sourceName;
    private SeekableByteChannel sourceStream;

    private Demuxer demuxVideo;
    private Demuxer demuxAudio;

    private Format inputFormat;

    private DemuxerTrack videoInputTrack;
    private DemuxerTrack audioInputTrack;

    private _3<Integer, Integer, Codec> inputVideoCodec;
    private _3<Integer, Integer, Codec> inputAudioCodec;

    private List<VideoFrameWithPacket> frameReorderBuffer = new ArrayList<VideoFrameWithPacket>();
    private List<Packet> videoPacketReorderBuffer = new ArrayList<Packet>();
    private PixelStore pixelStore;
    private VideoCodecMeta videoCodecMeta;
    private AudioCodecMeta audioCodecMeta;

    private AudioDecoder audioDecoder;
    private VideoDecoder videoDecoder;

    private int downscale = 1;

    public void initDemuxer() throws FileNotFoundException, IOException {
        if (inputFormat != Format.IMG)
            sourceStream = readableFileChannel(sourceName);

        switch (inputFormat) {
        case MOV:
            demuxVideo = demuxAudio = MP4Demuxer.createMP4Demuxer(sourceStream);
            break;
        case MKV:
            demuxVideo = demuxAudio = new MKVDemuxer(sourceStream);
            break;
        case IMG:
            demuxVideo = new ImageSequenceDemuxer(sourceName, Integer.MAX_VALUE);
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
        case H264:
            demuxVideo = new BufferH264ES(NIOUtils.fetchFromChannel(sourceStream));
            break;
        case WAV:
            demuxAudio = new WavDemuxer(sourceStream);
            break;
        case MPEG_AUDIO:
            demuxAudio = new MPEGAudioDemuxer(sourceStream);
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
            break;
        default:
            throw new RuntimeException("Input format: " + inputFormat + " is not supported.");
        }

        if (demuxVideo != null && inputVideoCodec != null) {
            List<? extends DemuxerTrack> videoTracks = demuxVideo.getVideoTracks();
            if (videoTracks.size() > 0) {
                videoInputTrack = videoTracks.get(inputVideoCodec.v1);
            }
        }
        if (demuxAudio != null && inputAudioCodec != null) {
            List<? extends DemuxerTrack> audioTracks = demuxAudio.getAudioTracks();
            if (audioTracks.size() > 0) {
                audioInputTrack = audioTracks.get(inputAudioCodec.v1);
            }
        }
    }

    /**
     * Seeks to a previous key frame prior or on the given frame, if the track is
     * not seekable returns 0.
     * 
     * @param frame
     *            A frame to seek
     * @return Frame number of a key frame or 0 if the track is not seekable.
     * @throws IOException
     */
    protected int seekToKeyFrame(int frame) throws IOException {
        if (videoInputTrack instanceof SeekableDemuxerTrack) {
            SeekableDemuxerTrack seekable = (SeekableDemuxerTrack) videoInputTrack;
            seekable.gotoSyncFrame(frame);
            return (int) seekable.getCurFrame();
        } else {
            Logger.warn("Can not seek in " + videoInputTrack + " container.");
            return -1;
        }
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

    @Override
    public Packet inputVideoPacket() throws IOException {
        while (true) {
            Packet packet = getNextVideoPacket();
            if (packet != null)
                videoPacketReorderBuffer.add(packet);
            if (packet == null || videoPacketReorderBuffer.size() > Transcoder.REORDER_BUFFER_SIZE) {
                if (videoPacketReorderBuffer.size() == 0)
                    return null;
                Packet out = videoPacketReorderBuffer.remove(0);
                int duration = Integer.MAX_VALUE;
                for (Packet packet2 : videoPacketReorderBuffer) {
                    int cand = (int) (packet2.getPts() - out.getPts());
                    if (cand > 0 && cand < duration)
                        duration = cand;
                }
                if (duration != Integer.MAX_VALUE)
                    out.setDuration(duration);
                return out;
            }
        }
    }

    private Packet getNextVideoPacket() throws IOException {
        if (videoInputTrack == null)
            return null;
        Packet nextFrame = videoInputTrack.nextFrame();
        // if (nextFrame != null)
        // Logger.debug(String.format("Input frame: pts=%d, duration=%d",
        // nextFrame.getPts(), nextFrame.getDuration()));

        if (videoDecoder == null) {
            videoDecoder = createVideoDecoder(inputVideoCodec.v2, downscale, nextFrame.getData(), null);
            if (videoDecoder != null) {
                videoCodecMeta = videoDecoder.getCodecMeta(nextFrame.getData());
            }
        }

        return nextFrame;
    }

    @Override
    public Packet inputAudioPacket() throws IOException {
        if (audioInputTrack == null)
            return null;
        Packet audioPkt = audioInputTrack.nextFrame();
        if (audioDecoder == null && audioPkt != null) {
            audioDecoder = createAudioDecoder(audioPkt.getData());
            if (audioDecoder != null)
                audioCodecMeta = audioDecoder.getCodecMeta(audioPkt.getData());
        }

        return audioPkt;
    }

    public DemuxerTrackMeta getTrackVideoMeta() {
        if (videoInputTrack == null)
            return null;
        return videoInputTrack.getMeta();
    }

    public DemuxerTrackMeta getAudioMeta() {
        if (audioInputTrack == null)
            return null;
        return audioInputTrack.getMeta();
    }

    public boolean haveAudio() {
        return audioInputTrack != null;
    }

    @Override
    public void finish() {
        if (sourceStream != null)
            IOUtils.closeQuietly(sourceStream);
    }

    public SourceImpl(String sourceName, Format inputFormat, _3<Integer, Integer, Codec> inputVideoCodec,
            _3<Integer, Integer, Codec> inputAudioCodec) {
        this.sourceName = sourceName;
        this.inputFormat = inputFormat;
        this.inputVideoCodec = inputVideoCodec;
        this.inputAudioCodec = inputAudioCodec;
    }

    @Override
    public void init(PixelStore pixelStore) throws IOException {
        this.pixelStore = pixelStore;

        initDemuxer();
    }

    private AudioDecoder createAudioDecoder(ByteBuffer codecPrivate) throws AACException {
        switch (inputAudioCodec.v2) {
        case AAC:
            return new AACDecoder(codecPrivate);
        case PCM:
            return new RawAudioDecoder(getAudioMeta().getAudioCodecMeta().getFormat());
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
        case MPEG4:
            return new MPEG4Decoder();
        case RAW:
            Size dim = videoCodecMeta.getSize();
            return new RAWVideoDecoder(dim.getWidth(), dim.getHeight());
        }
        return null;
    }

    public Picture decodeVideo(ByteBuffer data, Picture target1) {
        return videoDecoder.decodeFrame(data, target1.getData());
    }

    protected ByteBuffer decodeAudio(ByteBuffer audioPkt) throws IOException {
        if (inputAudioCodec.v2 == Codec.PCM) {
            return audioPkt;
        } else {
            AudioBuffer decodeFrame = audioDecoder.decodeFrame(audioPkt, null);
            return decodeFrame.getData();
        }
    }

    private static class RawAudioDecoder implements AudioDecoder {
        private AudioFormat format;

        public RawAudioDecoder(AudioFormat format) {
            this.format = format;
        }

        @Override
        public AudioBuffer decodeFrame(ByteBuffer frame, ByteBuffer dst) throws IOException {
            return new AudioBuffer(frame, format, frame.remaining() / format.getFrameSize());
        }

        @Override
        public AudioCodecMeta getCodecMeta(ByteBuffer data) throws IOException {
            return org.jcodec.common.AudioCodecMeta.fromAudioFormat(format);
        }
    }

    @Override
    public void seekFrames(int seekFrames) throws IOException {
        if (seekFrames == 0)
            return;
        // How many frames need to be skipped from the previouse key frame,
        // if the track is not seekable this will be equal to the
        // seekFrames.
        int skipFrames = seekFrames - seekToKeyFrame(seekFrames);

        // All the frames starting from the key frame must be actually
        // decoded in the decoder so that the decoder is 'warmed up'
        Packet inVideoPacket;
        for (; skipFrames > 0 && (inVideoPacket = getNextVideoPacket()) != null;) {
            LoanerPicture loanerBuffer = getPixelBuffer(inVideoPacket.getData());
            Picture decodedFrame = decodeVideo(inVideoPacket.getData(), loanerBuffer.getPicture());
            if (decodedFrame == null) {
                pixelStore.putBack(loanerBuffer);
                continue;
            }
            frameReorderBuffer.add(new VideoFrameWithPacket(inVideoPacket, new LoanerPicture(decodedFrame, 1)));
            if (frameReorderBuffer.size() > Transcoder.REORDER_BUFFER_SIZE) {
                Collections.sort(frameReorderBuffer);
                VideoFrameWithPacket removed = frameReorderBuffer.remove(0);
                skipFrames--;
                if (removed.getFrame() != null)
                    pixelStore.putBack(removed.getFrame());
            }
        }
    }

    private void detectFrameType(Packet inVideoPacket) {
        if (inputVideoCodec.v2 != Codec.H264) {
            return;
        }
        inVideoPacket.setFrameType(
                H264Utils.isByteBufferIDRSlice(inVideoPacket.getData()) ? FrameType.KEY : FrameType.INTER);
    }

    /**
     * Returns a pixel buffer of a suitable size to hold the given video frame. The
     * video size is taken either from the video metadata or by analyzing the
     * incoming video packet.
     * 
     * @param firstFrame
     * @return
     */
    protected LoanerPicture getPixelBuffer(ByteBuffer firstFrame) {
        VideoCodecMeta videoMeta = getVideoCodecMeta();
        Size size = videoMeta.getSize();
        return pixelStore.getPicture((size.getWidth() + 15) & ~0xf, (size.getHeight() + 15) & ~0xf,
                videoMeta.getColor());
    }

    @Override
    public VideoCodecMeta getVideoCodecMeta() {
        if (videoCodecMeta != null)
            return videoCodecMeta;
        DemuxerTrackMeta meta = getTrackVideoMeta();
        if (meta != null && meta.getVideoCodecMeta() != null) {
            videoCodecMeta = meta.getVideoCodecMeta();
        }
        return videoCodecMeta;
    }

    @Override
    public VideoFrameWithPacket getNextVideoFrame() throws IOException {
        Packet inVideoPacket;
        while ((inVideoPacket = getNextVideoPacket()) != null) {
            if (inVideoPacket.getFrameType() == FrameType.UNKNOWN) {
                detectFrameType(inVideoPacket);
            }
            Picture decodedFrame = null;
            LoanerPicture pixelBuffer = getPixelBuffer(inVideoPacket.getData());
            decodedFrame = decodeVideo(inVideoPacket.getData(), pixelBuffer.getPicture());
            if (decodedFrame == null) {
                pixelStore.putBack(pixelBuffer);
                continue;
            }
            frameReorderBuffer.add(new VideoFrameWithPacket(inVideoPacket, new LoanerPicture(decodedFrame, 1)));
            if (frameReorderBuffer.size() > Transcoder.REORDER_BUFFER_SIZE) {
                return removeFirstFixDuration(frameReorderBuffer);
            }
        }

        // We don't have any more compressed video packets
        if (frameReorderBuffer.size() > 0) {
            return removeFirstFixDuration(frameReorderBuffer);
        }

        // We don't have any more compressed video packets and nothing's in
        // the buffers
        return null;
    }

    private VideoFrameWithPacket removeFirstFixDuration(List<VideoFrameWithPacket> reorderBuffer) {
        Collections.sort(reorderBuffer);
        VideoFrameWithPacket frame = reorderBuffer.remove(0);
        if (!reorderBuffer.isEmpty()) {
            // Setting duration
            VideoFrameWithPacket nextFrame = reorderBuffer.get(0);
            frame.getPacket().setDuration(nextFrame.getPacket().getPts() - frame.getPacket().getPts());
        }
        return frame;
    }

    @Override
    public AudioFrameWithPacket getNextAudioFrame() throws IOException {
        Packet audioPkt = inputAudioPacket();
        if (audioPkt == null)
            return null;
        AudioBuffer audioBuffer;
        if (inputAudioCodec.v2 == Codec.PCM) {
            DemuxerTrackMeta audioMeta = getAudioMeta();
            audioBuffer = new AudioBuffer(audioPkt.getData(), audioMeta.getAudioCodecMeta().getFormat(),
                    audioMeta.getTotalFrames());
        } else {
            audioBuffer = audioDecoder.decodeFrame(audioPkt.getData(), null);
        }
        return new AudioFrameWithPacket(audioBuffer, audioPkt);
    }

    public _3<Integer, Integer, Codec> getIntputVideoCodec() {
        return inputVideoCodec;
    }

    public _3<Integer, Integer, Codec> getInputAudioCode() {
        return inputAudioCodec;
    }

    @Override
    public void setOption(Options option, Object value) {
        if (option == Options.DOWNSCALE)
            downscale = (Integer) value;
    }

    @Override
    public AudioCodecMeta getAudioCodecMeta() {
        if (audioInputTrack != null && audioInputTrack.getMeta() != null
                && audioInputTrack.getMeta().getAudioCodecMeta() != null) {
            return audioInputTrack.getMeta().getAudioCodecMeta();
        }
        return audioCodecMeta;
    }

    @Override
    public boolean isVideo() {
        if (!inputFormat.isVideo())
            return false;
        List<? extends DemuxerTrack> tracks = demuxVideo.getVideoTracks();
        return tracks != null && tracks.size() > 0;
    }

    @Override
    public boolean isAudio() {
        if (!inputFormat.isAudio())
            return false;
        List<? extends DemuxerTrack> tracks = demuxAudio.getAudioTracks();
        return tracks != null && tracks.size() > 0;
    }
}