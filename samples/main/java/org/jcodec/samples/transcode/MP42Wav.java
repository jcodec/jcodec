package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.aac.AACUtils.AACMetadata;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;

class MP42Wav extends V2VTranscoder {

    protected class MP42WavTranscoder extends GenericTranscoder {

        private AbstractMP4DemuxerTrack audioInputTrack;
        private WavOutput audioOutputTrack;
        private Decoder audioDecoder;

        public MP42WavTranscoder(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            MP4Demuxer demuxer = new MP4Demuxer(source);

            List<AbstractMP4DemuxerTrack> tracks = demuxer.getAudioTracks();
            audioInputTrack = null;
            for (AbstractMP4DemuxerTrack track : tracks) {
                if (track.getCodec() == Codec.AAC) {
                    audioInputTrack = track;
                    break;
                }
            }
            if (audioInputTrack == null) {
                Logger.error("Could not find an AAC track");
                return;
            } else {
                Logger.info("Using the AAC track: " + audioInputTrack.getNo());
            }
            SampleEntry sampleEntry = audioInputTrack.getSampleEntries()[0];
            audioDecoder = new Decoder(NIOUtils.toArray(AACUtils.getCodecPrivate(sampleEntry)));
        }

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            SampleEntry sampleEntry = audioInputTrack.getSampleEntries()[0];
            AACMetadata meta = AACUtils.getMetadata(sampleEntry);
            audioOutputTrack = new WavOutput(sink, meta.getFormat());
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
            SampleBuffer sampleBuffer = new SampleBuffer();

            audioDecoder.decodeFrame(NIOUtils.toArray(audioPkt), sampleBuffer);
            if (sampleBuffer.isBigEndian())
                toLittleEndian(sampleBuffer);
            return ByteBuffer.wrap(sampleBuffer.getData());
        }
        
        @Override
        protected ByteBuffer encodeAudio(ByteBuffer wrap) {
            return wrap;
        }

        private void toLittleEndian(SampleBuffer sampleBuffer) {
            byte[] data = sampleBuffer.getData();
            for (int i = 0; i < data.length; i += 2) {
                byte tmp = data[i];
                data[i] = data[i + 1];
                data[i + 1] = tmp;
            }
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