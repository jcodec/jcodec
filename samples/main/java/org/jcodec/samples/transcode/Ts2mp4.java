package org.jcodec.samples.transcode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jcodec.codecs.aac.AACConts;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
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
        private FramesMP4MuxerTrack videoOutputTrack;
        private FramesMP4MuxerTrack audioOutputTrack;
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
                if (videoTrackNo >= 0 && audioTrackNo >= 0)
                    break;
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
            Codec codec = track.getMeta().getCodec();
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
            if (videoInputTrack != null) {
                videoOutputTrack = muxer.addTrack(MP4TrackType.VIDEO, 90000);
            }
            if (audioInputTrack != null) {
                audioOutputTrack = muxer.addTrack(MP4TrackType.SOUND, 90000);
            }
        }

        @Override
        protected void finishEncode() throws IOException {
            muxer.writeHeader();
        }

        @Override
        protected Packet inputVideoPacket() throws IOException {
            return videoInputTrack != null ? videoInputTrack.nextFrame() : null;
        }

        @Override
        protected void outputVideoPacket(Packet packet) throws IOException {
            if (videoOutputTrack != null) {
                if (videoOutputTrack.getEntries().size() == 0) {
                    List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
                    List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();
                    H264Utils.wipePSinplace(packet.getData(), spsList, ppsList);
                    videoOutputTrack.addSampleEntry(H264Utils.createMOVSampleEntryFromSpsPpsList(spsList, ppsList, 4));
                } else {
                    H264Utils.wipePSinplace(packet.getData(), null, null);
                }
                H264Utils.encodeMOVPacket(packet.getData());
                videoOutputTrack.addFrame(packet);
            }
        }

        @Override
        protected boolean haveAudio() {
            return audioInputTrack != null;
        }

        @Override
        protected Packet inputAudioPacket() throws IOException {
            return audioInputTrack != null ? audioInputTrack.nextFrame() : null;
        }

        @Override
        protected void outputAudioPacket(Packet audioPkt) throws IOException {
            if (audioOutputTrack != null) {
                org.jcodec.codecs.aac.ADTSParser.Header header = ADTSParser.read(audioPkt.getData());
                if (audioOutputTrack.getEntries().size() == 0) {

                    AudioSampleEntry ase = AudioSampleEntry.createAudioSampleEntry(Header.createHeader("mp4a", 0),
                            (short) 1, (short) AACConts.AAC_CHANNEL_COUNT[header.getChanConfig()], (short) 16,
                            AACConts.AAC_SAMPLE_RATES[header.getSamplingIndex()], (short) 0, 0, 0, 0, 0, 0, 0, 2,
                            (short) 0);

                    audioOutputTrack.addSampleEntry(ase);
                    ase.add(EsdsBox.fromADTS(header));
                }

                audioOutputTrack.addFrame(audioPkt);
            }
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