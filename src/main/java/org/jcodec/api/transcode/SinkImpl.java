package org.jcodec.api.transcode;

import static org.jcodec.common.io.NIOUtils.writableFileChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.png.PNGEncoder;
import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.codecs.raw.RAWVideoEncoder;
import org.jcodec.codecs.vpx.IVFMuxer;
import org.jcodec.codecs.vpx.VP8Encoder;
import org.jcodec.codecs.wav.WavMuxer;
import org.jcodec.codecs.y4m.Y4MMuxer;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioEncoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
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
import org.jcodec.containers.imgseq.ImageSequenceMuxer;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * The sink that consumes the uncompressed frames and stores them into a
 * compressed file.
 * 
 * @author Stanislav Vitvitskiy
 */
public class SinkImpl implements Sink, PacketSink {
    private String destName;
    private SeekableByteChannel destStream;
    private Muxer muxer;
    private MuxerTrack videoOutputTrack;
    private MuxerTrack audioOutputTrack;
    private boolean framesOutput;
    private Codec outputVideoCodec;
    private Codec outputAudioCodec;
    private Format outputFormat;
    private ThreadLocal<ByteBuffer> bufferStore = new ThreadLocal<ByteBuffer>();

    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;
    private String profile;
    private boolean interlaced;

    @Override
    public void outputVideoPacket(Packet packet, VideoCodecMeta codecMeta) throws IOException {
        if (!outputFormat.isVideo())
            return;
        if (videoOutputTrack == null) {
            videoOutputTrack = muxer.addVideoTrack(outputVideoCodec, codecMeta);
        }
        videoOutputTrack.addFrame(packet);
        framesOutput = true;
    }

    @Override
    public void outputAudioPacket(Packet audioPkt, AudioCodecMeta audioCodecMeta) throws IOException {
        if (!outputFormat.isAudio())
            return;
        if (audioOutputTrack == null) {
            audioOutputTrack = muxer.addAudioTrack(outputAudioCodec, audioCodecMeta);
        }
        audioOutputTrack.addFrame(audioPkt);
        framesOutput = true;
    }

    public void initMuxer() throws IOException {
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
        case Y4M:
            muxer = new Y4MMuxer(destStream);
            break;
        }
    }

    public void finish() throws IOException {
        if (framesOutput) {
            muxer.finish();
        } else {
            Logger.warn("No frames output.");
        }
        if (destStream != null) {
            IOUtils.closeQuietly(destStream);
        }
    }

    public SinkImpl(String destName, Format outputFormat, Codec outputVideoCodec, Codec outputAudioCodec) {
        this.destName = destName;
        this.outputFormat = outputFormat;
        this.outputVideoCodec = outputVideoCodec;
        this.outputAudioCodec = outputAudioCodec;
        this.outputFormat = outputFormat;
    }

    @Override
    public void init() throws IOException {
        initMuxer();
        if (outputFormat.isVideo() && outputVideoCodec != null) {
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
            case RAW:
                videoEncoder = new RAWVideoEncoder();
                break;
            default:
                throw new RuntimeException("Could not find encoder for the codec: " + outputVideoCodec);
            }
        }
    }

    protected EncodedFrame encodeVideo(Picture8Bit frame, ByteBuffer _out) {
        if (!outputFormat.isVideo())
            return null;

        return videoEncoder.encodeFrame8Bit(frame, _out);
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

    protected ByteBuffer encodeAudio(AudioBuffer audioBuffer) {
        if (audioEncoder == null) {
            AudioFormat format = audioBuffer.getFormat();
            audioEncoder = createAudioEncoder(outputAudioCodec, format);
        }

        return audioEncoder.encode(audioBuffer.getData(), null);
    }

    @Override
    public Format getOutputFormat() {
        return outputFormat;
    }

    public Codec getOutputVideoCodec() {
        return outputVideoCodec;
    }

    public Codec getOutputAudioCodec() {
        return outputAudioCodec;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setInterlaced(Boolean interlaced) {
        this.interlaced = interlaced;
    }

    @Override
    public void outputVideoFrame(VideoFrameWithPacket videoFrame) throws IOException {
        if (!outputFormat.isVideo())
            return;
        Packet outputVideoPacket;
        ByteBuffer buffer = bufferStore.get();
        int bufferSize = videoEncoder.estimateBufferSize(videoFrame.getFrame());
        if (buffer == null || bufferSize < buffer.capacity()) {
            buffer = ByteBuffer.allocate(bufferSize);
            bufferStore.set(buffer);
        }
        buffer.clear();
        Picture8Bit frame = videoFrame.getFrame();
        EncodedFrame enc = encodeVideo(frame, buffer);
        outputVideoPacket = Packet.createPacketWithData(videoFrame.getPacket(), NIOUtils.clone(enc.getData()));
        outputVideoPacket.setFrameType(enc.isKeyFrame() ? FrameType.KEY : FrameType.INTER);
        outputVideoPacket(outputVideoPacket,
                new VideoCodecMeta(new Size(frame.getWidth(), frame.getHeight()), frame.getColor()));
    }

    @Override
    public void outputAudioFrame(AudioFrameWithPacket audioFrame) throws IOException {
        if (!outputFormat.isAudio())
            return;
        outputAudioPacket(Packet.createPacketWithData(audioFrame.getPacket(), encodeAudio(audioFrame.getAudio())),
                new AudioCodecMeta(audioFrame.getAudio().getFormat()));
    }

    @Override
    public ColorSpace getInputColor() {
        if (videoEncoder == null)
            return null;
        return videoEncoder.getSupportedColorSpaces()[0];
    }

    @Override
    public void setOption(Options option, Object value) {
        if (option == Options.PROFILE)
            profile = (String) value;
        else if (option == Options.INTERLACED)
            interlaced = (Boolean) value;
    }
}