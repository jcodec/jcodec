package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

class MP42Wav extends V2VTranscoder {

    protected class MP42WavTranscoder extends GenericTranscoder {

        private DemuxerTrack audioInputTrack;
        private WavOutput audioOutputTrack;
        private AACDecoder audioDecoder;
        private SeekableByteChannel sink;

        public MP42WavTranscoder(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            MP4Demuxer demuxer = new MP4Demuxer(source);

            List<DemuxerTrack> tracks = demuxer.getAudioTracks();
            audioInputTrack = null;
            for (DemuxerTrack track : tracks) {
                if (track.getMeta().getCodec() == Codec.AAC) {
                    audioInputTrack = track;
                    break;
                }
            }
            if (audioInputTrack == null) {
                Logger.error("Could not find an AAC track");
                return;
            } else {
                Logger.info("Using the AAC track: " + audioInputTrack.getMeta().getIndex());
            }
            audioDecoder = new AACDecoder(audioInputTrack.getMeta().getCodecPrivate());
        }

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            this.sink = sink;
        }

        @Override
        protected void finishEncode() throws IOException {
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            return null;
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
        }

        @Override
        protected boolean haveAudio() {
            return true;
        }

        @Override
        protected Packet inputAudioPacket() throws IOException {
            return audioInputTrack.nextFrame();
        }

        @Override
        protected void outputAudioPacket(Packet audioPkt) throws IOException {
            audioOutputTrack.write(audioPkt.getData());
        }

        @Override
        protected ByteBuffer decodeAudio(ByteBuffer audioPkt) throws IOException {
            AudioBuffer decodeFrame = audioDecoder.decodeFrame(audioPkt, null);
            if (audioOutputTrack == null)
                audioOutputTrack = new WavOutput(sink, decodeFrame.getFormat());
            return decodeFrame.getData();
        }

        @Override
        protected ByteBuffer encodeAudio(ByteBuffer data) {
            return data;
        }
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.WAV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return null;
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return null;
    }

    @Override
    public Set<Codec> inputAudioCodec() {
        return TranscodeMain.codecs(Codec.AAC);
    }

    @Override
    public Set<Codec> outputAudioCodec() {
        return TranscodeMain.codecs(Codec.PCM);
    }

    @Override
    protected GenericTranscoder getTranscoder(Cmd cmd, Profile profile) {
        return new MP42WavTranscoder(cmd, profile);
    }
}