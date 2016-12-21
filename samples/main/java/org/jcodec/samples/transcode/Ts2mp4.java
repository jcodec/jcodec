package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.aac.AACConts;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.aac.ADTSParser.Header;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;

class Ts2mp4 extends V2VTranscoder {
    private static class Ts2mp4Transcoder extends GenericTranscoder {

        private static final Set<Codec> supportedCodecs = new HashSet<Codec>();
        private MP4Muxer muxer;
        private DemuxerTrack videoInputTrack;
        private DemuxerTrack audioInputTrack;
        private MuxerTrack videoOutputTrack;
        private MuxerTrack audioOutputTrack;
        private int videoTrackNo = -1;
        private int audioTrackNo = -1;

        static {
            // Track selection will only pick up these tracks
            supportedCodecs.add(Codec.H264);
            supportedCodecs.add(Codec.AAC);
        }

        public Ts2mp4Transcoder(Cmd cmd, Profile profile) {
            super(cmd, profile);
        }

        @Override
        protected void initDecode(SeekableByteChannel source) throws IOException {
            selectTracks(source);
            if (videoTrackNo < 0 && audioTrackNo < 0)
                throw new IOException("No suitable tracks found for transcode.");
            source.setPosition(0);
            openDemuxers(source);
        }

        private void openDemuxers(SeekableByteChannel source) throws IOException {
            int trackNo = 0;
            MTSDemuxer mtsDemuxer = new MTSDemuxer(source);
            for (int pid : mtsDemuxer.getPrograms()) {
                MPSDemuxer program = new MPSDemuxer(mtsDemuxer.getProgram(pid));
                boolean demuxerUsed = false;
                for (MPEGDemuxerTrack track : program.getVideoTracks()) {
                    if (trackNo == videoTrackNo) {
                        videoInputTrack = track;
                        demuxerUsed = true;
                    } else
                        track.ignore();
                    ++trackNo;
                }
                for (MPEGDemuxerTrack track : program.getAudioTracks()) {
                    if (trackNo == audioTrackNo) {
                        audioInputTrack = track;
                        demuxerUsed = true;
                    } else
                        track.ignore();
                    ++trackNo;
                }
                if (!demuxerUsed)
                    program.close();
            }
        }

        private void selectTracks(SeekableByteChannel source) throws IOException {
            int trackNo = 0;
            MTSDemuxer mtsDemuxer = new MTSDemuxer(source);
            for (int pid : mtsDemuxer.getPrograms()) {
                MPSDemuxer demuxer = new MPSDemuxer(mtsDemuxer.getProgram(pid));
                for (MPEGDemuxerTrack track : demuxer.getVideoTracks()) {
                    if (videoTrackNo == -1 && checkTrack(track))
                        videoTrackNo = trackNo;
                    ++trackNo;
                }
                for (MPEGDemuxerTrack track : demuxer.getAudioTracks()) {
                    if (audioTrackNo == -1 && checkTrack(track))
                        audioTrackNo = trackNo;
                    ++trackNo;
                }
                if (videoTrackNo >= 0 && audioTrackNo >= 0)
                    break;
            }
        }

        private boolean checkTrack(MPEGDemuxerTrack track) throws IOException {
            Codec codec = track.getMeta() == null ? null : track.getMeta().getCodec();
            for (int i = 0; i < 2; i++) {
                if (codec != null && supportedCodecs.contains(codec))
                    return true;
                if (codec == null) {
                    Packet firstFrame = track.nextFrame();
                    codec = JCodecUtil.detectDecoder(firstFrame.getData());
                }
            }
            return false;
        }

        @Override
        protected void initEncode(SeekableByteChannel sink) throws IOException {
            muxer = MP4Muxer.createMP4Muxer(sink, Brand.MP4);
        }

        @Override
        protected void finishEncode() throws IOException {
            muxer.finish();
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            if (videoInputTrack == null)
                return null;
            Packet nextFrame = videoInputTrack.nextFrame();
            if (videoOutputTrack == null) {
                Codec codec = JCodecUtil.detectDecoder(nextFrame.getData());
                if (codec == null)
                    throw new RuntimeException("Could not detect codec");
                VideoDecoder videoDecoder = JCodecUtil.createVideoDecoder(codec, nextFrame.getData());
                VideoCodecMeta meta = videoDecoder.getCodecMeta(nextFrame.getData());
                videoOutputTrack = muxer.addVideoTrack(codec, meta);
            }
            return nextFrame;
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
            videoOutputTrack.addFrame(packet);
        }

        @Override
        protected boolean haveAudio() {
            return audioInputTrack != null;
        }

        @Override
        protected Packet inputAudioPacket() throws IOException {
            Packet nextFrame = audioInputTrack.nextFrame();
            if (nextFrame == null)
                return null;

            if (audioOutputTrack == null) {
                Codec codec = JCodecUtil.detectDecoder(nextFrame.getData());
                if (codec == null)
                    throw new RuntimeException("Could not detect codec");
                AudioDecoder audioDecoder = JCodecUtil.createAudioDecoder(codec, nextFrame.getData());
                AudioCodecMeta meta = audioDecoder.getCodecMeta(nextFrame.getData());
                audioOutputTrack = muxer.addAudioTrack(codec, meta);
            }
            return nextFrame;
        }

        @Override
        protected void outputAudioPacket(Packet audioPkt) throws IOException {
            audioOutputTrack.addFrame(audioPkt);
        }

        @Override
        protected boolean audioCodecCopy() {
            return true;
        }

        @Override
        protected boolean videoCodecCopy() {
            return true;
        }
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MPEG_TS);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return TranscodeMain.codecs(Codec.H264);
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
        return TranscodeMain.codecs(Codec.AAC);
    }

    @Override
    protected GenericTranscoder getTranscoder(Cmd cmd, Profile profile) {
        return new Ts2mp4Transcoder(cmd, profile);
    }
}