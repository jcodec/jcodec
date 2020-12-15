package org.jcodec.containers.mkv.demuxer;
import static org.jcodec.common.model.TapeTimecode.ZERO_TAPE_TIMECODE;
import static org.jcodec.containers.mkv.MKVType.Audio;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.CodecPrivate;
import static org.jcodec.containers.mkv.MKVType.DisplayHeight;
import static org.jcodec.containers.mkv.MKVType.DisplayUnit;
import static org.jcodec.containers.mkv.MKVType.DisplayWidth;
import static org.jcodec.containers.mkv.MKVType.Info;
import static org.jcodec.containers.mkv.MKVType.PixelHeight;
import static org.jcodec.containers.mkv.MKVType.PixelWidth;
import static org.jcodec.containers.mkv.MKVType.SamplingFrequency;
import static org.jcodec.containers.mkv.MKVType.TagLanguage;
import static org.jcodec.containers.mkv.MKVType.Language;
import static org.jcodec.containers.mkv.MKVType.Channels;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.TimecodeScale;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.TrackType;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.Video;
import static org.jcodec.containers.mkv.MKVType.findFirst;
import static org.jcodec.containers.mkv.MKVType.findList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Label;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.containers.mkv.MKVParser;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.MkvBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public final class MKVDemuxer implements Demuxer {
    private VideoTrack vTrack = null;
    private List<AudioTrack> aTracks;
    private List<SubtitlesTrack> subsTracks;
    private List<EbmlMaster> t;
    private SeekableByteChannel channel;
    int timescale = 1;
    int pictureWidth;
    int pictureHeight;

    private static Map<String, Codec> codecVideoMapping = new HashMap<String, Codec>();
    static {
        codecVideoMapping.put("V_MS/VFW/FOURCC", null);
        codecVideoMapping.put("V_UNCOMPRESSED", null);
        codecVideoMapping.put("V_MPEG4/ISO/SP", null);
        codecVideoMapping.put("V_MPEG4/ISO/ASP", null);
        codecVideoMapping.put("V_MPEG4/ISO/AP", null);
        codecVideoMapping.put("V_MPEG4/MS/V3", Codec.MPEG4);
        codecVideoMapping.put("V_MPEG4/ISO/AVC", Codec.H264);
        codecVideoMapping.put("V_MPEGH/ISO/HEVC", Codec.H265);
        codecVideoMapping.put("V_MPEG1", null);
        codecVideoMapping.put("V_MPEG2", Codec.MPEG2);
        codecVideoMapping.put("V_REAL/RV10", null);
        codecVideoMapping.put("V_REAL/RV20", null);
        codecVideoMapping.put("V_REAL/RV30", null);
        codecVideoMapping.put("V_REAL/RV40", null);
        codecVideoMapping.put("V_QUICKTIME", null);
        codecVideoMapping.put("V_THEORA", null);
        codecVideoMapping.put("V_PRORES", null);
        codecVideoMapping.put("V_VP8", Codec.VP8);
        codecVideoMapping.put("V_VP9", Codec.VP9);
        codecVideoMapping.put("V_FFV1", null);
    }
    private static Map<String, Codec> codecAudioMapping = new HashMap<String, Codec>();
    static {
        codecAudioMapping.put("A_MPEG/L3", Codec.MP3);
        codecAudioMapping.put("A_MPEG/L2", null);
        codecAudioMapping.put("A_MPEG/L1", null);
        codecAudioMapping.put("A_PCM/INT/BIG", null);
        codecAudioMapping.put("A_PCM/INT/LIT", null);
        codecAudioMapping.put("A_PCM/FLOAT/IEEE", null);
        codecAudioMapping.put("A_MPC", null);
        codecAudioMapping.put("A_AC3", Codec.AC3);
        codecAudioMapping.put("A_AC3/BSID9", Codec.AC3);
        codecAudioMapping.put("A_AC3/BSID10", Codec.AC3);
        codecAudioMapping.put("A_ALAC", null);
        codecAudioMapping.put("A_DTS", null);
        codecAudioMapping.put("A_DTS/EXPRESS", null);
        codecAudioMapping.put("A_DTS/LOSSLESS", null);
        codecAudioMapping.put("A_VORBIS", Codec.VORBIS);
        codecAudioMapping.put("A_FLAC", null);
        codecAudioMapping.put("A_REAL/14_4", null);
        codecAudioMapping.put("A_REAL/28_8", null);
        codecAudioMapping.put("A_REAL/COOK", null);
        codecAudioMapping.put("A_REAL/SIPR", null);
        codecAudioMapping.put("A_REAL/RALF", null);
        codecAudioMapping.put("A_REAL/ATRC", null);
        codecAudioMapping.put("A_MS/ACM", null);
        codecAudioMapping.put("A_AAC/MPEG2/MAIN", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG2/LC", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG2/LC/SBR", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG2/SSR", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG4/MAIN", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG4/LC", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG4/LC/SBR", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG4/SSR", Codec.AAC);
        codecAudioMapping.put("A_AAC/MPEG4/LTP", Codec.AAC);
        codecAudioMapping.put("A_QUICKTIME", null);
        codecAudioMapping.put("A_QUICKTIME/QDMC", null);
        codecAudioMapping.put("A_QUICKTIME/QDM2", null);
        codecAudioMapping.put("A_TTA1", null);
        codecAudioMapping.put("A_WAVPACK4", null);
        codecAudioMapping.put("A_OPUS", Codec.OPUS);

    }
    private static Map<String, Codec> codecSubtitleMapping = new HashMap<String, Codec>();
    static {
        // unmanage codec:
        codecSubtitleMapping.put("S_TEXT/UTF8", null);
        codecSubtitleMapping.put("S_TEXT/SSA", null);
        codecSubtitleMapping.put("S_TEXT/ASS", null);
        codecSubtitleMapping.put("S_TEXT/USF", null);
        codecSubtitleMapping.put("S_TEXT/WEBVTT", null);
        codecSubtitleMapping.put("S_IMAGE/BMP", null);
        codecSubtitleMapping.put("S_DVBSUB", null);
        codecSubtitleMapping.put("S_VOBSUB", null);
        codecSubtitleMapping.put("S_HDMV/PGS", null);
        codecSubtitleMapping.put("S_HDMV/TEXTST", null);
        codecSubtitleMapping.put("S_KATE", null);
    }

    public MKVDemuxer(SeekableByteChannel fileChannelWrapper) throws IOException {
        this.channel = fileChannelWrapper;
        this.aTracks = new ArrayList<AudioTrack>();
        this.subsTracks = new ArrayList<SubtitlesTrack>();
        MKVParser parser = new MKVParser(channel);
        this.t = parser.parse();
        demux();
    }

    private void demux() {
        MKVType[] path = { Segment, Info, TimecodeScale };
        EbmlUint ts = MKVType.findFirstTree(t, path);
        if (ts != null)
            timescale = (int) ts.getUint();
        MKVType[] path9 = { Segment, Tracks, TrackEntry };

        for (EbmlMaster aTrack : findList(t, EbmlMaster.class, path9)) {
            MKVType[] path1 = { TrackEntry, TrackType };
            long type = ((EbmlUint) findFirst(aTrack, path1)).getUint();
            MKVType[] path2 = { TrackEntry, TrackNumber };
            long id = ((EbmlUint) findFirst(aTrack, path2)).getUint();
            if (type == 1) {
                // video
                if (vTrack != null)
                    throw new RuntimeException("More then 1 video track, can not compute...");
                MKVType[] path3 = { TrackEntry, CodecPrivate };
                MKVType[] path10 = { TrackEntry, MKVType.CodecID };
                EbmlString codecId = (EbmlString) findFirst(aTrack, path10);
                Codec codec = codecVideoMapping.get(codecId.getString());
                
                EbmlBin videoCodecState = (EbmlBin) findFirst(aTrack, path3);
                ByteBuffer state = null;
                if (videoCodecState != null)
                    state = videoCodecState.data;

                MKVType[] path4 = { TrackEntry, Video, PixelWidth };
                EbmlUint width = (EbmlUint) findFirst(aTrack, path4);
                MKVType[] path5 = { TrackEntry, Video, PixelHeight };
                EbmlUint height = (EbmlUint) findFirst(aTrack, path5);
                MKVType[] path6 = { TrackEntry, Video, DisplayWidth };
                EbmlUint dwidth = (EbmlUint) findFirst(aTrack, path6);
                MKVType[] path7 = { TrackEntry, Video, DisplayHeight };
                EbmlUint dheight = (EbmlUint) findFirst(aTrack, path7);
                MKVType[] path8 = { TrackEntry, Video, DisplayUnit };
                EbmlUint unit = (EbmlUint) findFirst(aTrack, path8);
                if (width != null && height != null) {
                    pictureWidth = (int) width.getUint();
                    pictureHeight = (int) height.getUint();
                } else if (dwidth != null && dheight != null) {
                    if (unit == null || unit.getUint() == 0) {
                        pictureHeight = (int) dheight.getUint();
                        pictureWidth = (int) dwidth.getUint();
                    } else {
                        throw new RuntimeException("DisplayUnits other then 0 are not implemented yet");
                    }
                }
                vTrack = new VideoTrack(this, (int) id, state, codec);
            } else if (type == 2) {
                double sampleRate = 8000.0;
                long channelCount = 1;
                String language = "eng";
                MKVType[] path10 = { TrackEntry, MKVType.CodecID };
                EbmlString codecId = (EbmlString) findFirst(aTrack, path10);
                Codec codec = codecAudioMapping.get(codecId.getString());
                MKVType[] path3 = { TrackEntry, Audio, SamplingFrequency };
                EbmlFloat sf = (EbmlFloat) findFirst(aTrack, path3);
                if (sf != null) {
                    sampleRate = sf.getDouble();
                }
                MKVType[] path4 = { TrackEntry, Audio, Channels };
                EbmlUint channels = (EbmlUint) findFirst(aTrack, path4);
                if (channels != null) {
                    channelCount = channels.getUint();
                }
                MKVType[] path5 = { TrackEntry, Language };
                EbmlString tagLanguage = (EbmlString) findFirst(aTrack, path5);
                if (tagLanguage != null) {
                    language = tagLanguage.getString();
                }
                aTracks.add(new AudioTrack(this, (int) id, codec, sampleRate, channelCount, language));
            } else if (type == 17) {
                SubtitlesTrack subsTrack = new SubtitlesTrack((int) id, this);
                subsTracks.add(subsTrack);
            }
        }
        MKVType[] path2 = { Segment, Cluster };
        for (EbmlMaster aCluster : findList(t, EbmlMaster.class, path2)) {
            MKVType[] path1 = { Cluster, Timecode };
            long baseTimecode = ((EbmlUint) findFirst(aCluster, path1)).getUint();
            for (EbmlBase child : aCluster.children)
                if (MKVType.SimpleBlock.equals(child.type)) {
                    MkvBlock b = (MkvBlock) child;
                    b.absoluteTimecode = b.timecode + baseTimecode;
                    putIntoRightBasket(b);
                } else if (MKVType.BlockGroup.equals(child.type)) {
                    EbmlMaster group = (EbmlMaster) child;
                    for (EbmlBase grandChild : group.children) {
                        if (grandChild.type == MKVType.Block) {
                            MkvBlock b = (MkvBlock) grandChild;
                            b.absoluteTimecode = b.timecode + baseTimecode;
                            putIntoRightBasket(b);
                        }
                    }
                }
        }
    }

    private void putIntoRightBasket(MkvBlock b) {
        if (b.trackNumber == vTrack.trackNo) {
            vTrack.blocks.add(b);

        } else {
            for (int i = 0; i < aTracks.size(); i++) {
                AudioTrack audio = (AudioTrack) aTracks.get(i);
                if (b.trackNumber == audio.trackNo) {
                    audio.blocks.add(IndexedBlock.make(audio.framesCount, b));
                    audio.framesCount += b.frameSizes.length;
                }
            }
            for (int i = 0; i < subsTracks.size(); i++) {
                SubtitlesTrack subs = subsTracks.get(i);
                if (b.trackNumber == subs.trackNo) {
                    subs.blocks.add(IndexedBlock.make(subs.framesCount, b));
                    subs.framesCount += b.frameSizes.length;
                }
            }
        }
    }

    public static class VideoTrack implements SeekableDemuxerTrack {
        private ByteBuffer state;
        public final int trackNo;
        private int frameIdx = 0;
        List<MkvBlock> blocks;
        private MKVDemuxer demuxer;
        private Codec codec;
        private AvcCBox avcC;

        public VideoTrack(MKVDemuxer demuxer, int trackNo, ByteBuffer state, Codec codec) {
            this.blocks = new ArrayList<MkvBlock>();
            this.demuxer = demuxer;
            this.trackNo = trackNo;
            this.codec = codec;
            if (codec == Codec.H264) {
                avcC = H264Utils.parseAVCCFromBuffer(state);
                this.state = H264Utils.avcCToAnnexB(avcC);
            } else {
                this.state = state;
            }
        }
        
        @Override
        public Packet nextFrame() throws IOException {
            if (frameIdx >= blocks.size())
                return null;

            MkvBlock b = blocks.get(frameIdx);
            if (b == null)
                throw new RuntimeException("Something somewhere went wrong.");
            frameIdx++;
            /**
             * This part could be moved withing yet-another inner class, say
             * MKVPacket to that channel is actually read only when
             * Packet.getData() is executed.
             */
            demuxer.channel.setPosition(b.dataOffset);
            ByteBuffer data = ByteBuffer.allocate(b.dataLen);
            demuxer.channel.read(data);
            data.flip();
            b.readFrames(data.duplicate());
            long duration = 1;
            if (frameIdx < blocks.size())
                duration = blocks.get(frameIdx).absoluteTimecode - b.absoluteTimecode;
            ByteBuffer result = b.frames[0].duplicate();
            if (codec == Codec.H264) {
                result = H264Utils.decodeMOVPacket(result, avcC);
            }
            return Packet.createPacket(result, b.absoluteTimecode, demuxer.timescale, duration,
                    frameIdx - 1, b._keyFrame ? FrameType.KEY : FrameType.INTER, ZERO_TAPE_TIMECODE);
        }

        @Override
        public boolean gotoFrame(long i) {
            if (i > Integer.MAX_VALUE)
                return false;
            if (i > blocks.size())
                return false;

            frameIdx = (int) i;
            return true;
        }

        @Override
        public long getCurFrame() {
            return frameIdx;
        }

        @Override
        public void seek(double second) {
            throw new RuntimeException("Not implemented yet");

        }

        public int getFrameCount() {
            return blocks.size();
        }

        public ByteBuffer getCodecState() {
            return state;
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return new DemuxerTrackMeta(org.jcodec.common.TrackType.VIDEO, codec, 0, null, 0, state,
                    org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta(new Size(demuxer.pictureWidth, demuxer.pictureHeight), ColorSpace.YUV420), null);
        }

        @Override
        public boolean gotoSyncFrame(long i) {
            throw new RuntimeException("Unsupported");
        }
    }

    public static class IndexedBlock {
        public int firstFrameNo;
        public MkvBlock block;

        public static IndexedBlock make(int no, MkvBlock b) {
            IndexedBlock ib = new IndexedBlock();
            ib.firstFrameNo = no;
            ib.block = b;
            return ib;
        }
    }

    public static class SubtitlesTrack extends MkvTrack {
        SubtitlesTrack(int trackNo, MKVDemuxer demuxer) {
            super(trackNo, demuxer);
        }
    }

    private static class MkvBlockData {
        final MkvBlock block;
        final ByteBuffer data;
        final int count;

        MkvBlockData(MkvBlock block, ByteBuffer data, int count) {
            this.block = block;
            this.data = data;
            this.count = count;
        }
    }

    public static class MkvTrack implements SeekableDemuxerTrack {
        public final int trackNo;
        List<IndexedBlock> blocks;
        int framesCount = 0;
        private int frameIdx = 0;
        private int blockIdx = 0;
        private int frameInBlockIdx = 0;
        private MKVDemuxer demuxer;

        public MkvTrack(int trackNo, MKVDemuxer demuxer) {
            this.blocks = new ArrayList<IndexedBlock>();

            this.trackNo = trackNo;
            this.demuxer = demuxer;
        }

        @Override
        public Packet nextFrame() throws IOException {
            MkvBlockData bd = nextBlock();
            if (bd == null) return null;
            return Packet.createPacket(bd.data, bd.block.absoluteTimecode, demuxer.timescale, 1, frameIdx - 1, FrameType.KEY,
                    ZERO_TAPE_TIMECODE);
        }

        protected MkvBlockData nextBlock() throws IOException {
            if (frameIdx >= blocks.size() || blockIdx >= blocks.size())
                return null;

            MkvBlock b = blocks.get(blockIdx).block;
            if (b == null)
                throw new RuntimeException("Something somewhere went wrong.");

            if (b.frames == null || b.frames.length == 0) {
                /**
                 * This part could be moved withing yet-another inner class, say
                 * MKVPacket to that channel is actually rean only when
                 * Packet.getData() is executed.
                 */
                demuxer.channel.setPosition(b.dataOffset);
                ByteBuffer data = ByteBuffer.allocate(b.dataLen);
                demuxer.channel.read(data);
                b.readFrames(data);
            }
            ByteBuffer data = b.frames[frameInBlockIdx].duplicate();
            frameInBlockIdx++;
            frameIdx++;
            if (frameInBlockIdx >= b.frames.length) {
                blockIdx++;
                frameInBlockIdx = 0;
            }

            return new MkvBlockData(b, data, 1);

        }

        @Override
        public boolean gotoFrame(long i) {
            if (i > Integer.MAX_VALUE)
                return false;
            if (i > this.framesCount)
                return false;

            int frameBlockIdx = findBlockIndex(i);
            if (frameBlockIdx == -1)
                return false;

            frameIdx = (int) i;
            blockIdx = frameBlockIdx;
            frameInBlockIdx = (int) i - blocks.get(blockIdx).firstFrameNo;

            return true;
        }

        private int findBlockIndex(long i) {
            for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
                if (i < blocks.get(blockIndex).block.frameSizes.length)
                    return blockIndex;

                i -= blocks.get(blockIndex).block.frameSizes.length;
            }

            return -1;
        }

        @Override
        public long getCurFrame() {
            return frameIdx;
        }

        @Override
        public void seek(double second) {
            throw new RuntimeException("Not implemented yet");
        }

        /**
         * Get multiple frames
         * 
         * @param count
         * @return
         */
        public Packet getFrames(int count) {

            MkvBlockData frameBlock = getFrameBlock(count);
            if (frameBlock == null) return null;


            return Packet.createPacket(frameBlock.data, frameBlock.block.absoluteTimecode, demuxer.timescale,
                    frameBlock.count, 0, FrameType.KEY, ZERO_TAPE_TIMECODE);
        }

        MkvBlockData getFrameBlock(int count) {
            if (count + frameIdx >= framesCount)
                return null;
            List<ByteBuffer> packetFrames = new ArrayList<ByteBuffer>();
            MkvBlock firstBlockInAPacket = blocks.get(blockIdx).block;
            while (count > 0) {
                MkvBlock b = blocks.get(blockIdx).block;
                if (b.frames == null || b.frames.length == 0) {
                    /**
                     * This part could be moved withing yet-another inner class,
                     * say MKVPacket to that channel is actually rean only when
                     * Packet.getData() is executed.
                     */
                    try {
                        demuxer.channel.setPosition(b.dataOffset);
                        ByteBuffer data = ByteBuffer.allocate(b.dataLen);
                        demuxer.channel.read(data);
                        b.readFrames(data);
                    } catch (IOException ioe) {
                        throw new RuntimeException("while reading frames of a Block at offset 0x"
                                + Long.toHexString(b.dataOffset).toUpperCase() + ")", ioe);
                    }
                }
                packetFrames.add(b.frames[frameInBlockIdx].duplicate());
                frameIdx++;
                frameInBlockIdx++;
                if (frameInBlockIdx >= b.frames.length) {
                    frameInBlockIdx = 0;
                    blockIdx++;
                }
                count--;
            }

            int size = 0;
            for (ByteBuffer aFrame : packetFrames)
                size += aFrame.limit();

            ByteBuffer data = ByteBuffer.allocate(size);
            for (ByteBuffer aFrame : packetFrames)
                data.put(aFrame);

            return new MkvBlockData(firstBlockInAPacket, data, packetFrames.size());

        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return null;
        }

        @Override
        public boolean gotoSyncFrame(long frame) {
            return gotoFrame(frame);
        }
    }

    public static class AudioTrack extends MkvTrack {
        private double sampleRate = 8000;
        private String language = "eng";
        private long channelCount = 1;
        private Codec codec = null;

        public AudioTrack(MKVDemuxer demuxer, int trackNo, Codec codec, double sampleRate, long channelCount, String language) {
            super(trackNo, demuxer);
            this.codec = codec;
            this.language = language;
            this.sampleRate = sampleRate;
            this.channelCount = channelCount;
        }

        @Override
        public Packet nextFrame() throws IOException {
            MkvBlockData b = nextBlock();
            if (b == null) return null;

            return Packet.createPacket(b.data, b.block.absoluteTimecode, (int) Math.round(sampleRate), 1, 0, FrameType.KEY,
                    ZERO_TAPE_TIMECODE);
        }

        /**
         * Get multiple frames
         *
         * @param count
         * @return
         */
        public Packet getFrames(int count) {
            MkvBlockData frameBlock = getFrameBlock(count);
            if (frameBlock == null) return null;

            return Packet.createPacket(frameBlock.data, frameBlock.block.absoluteTimecode, (int) Math.round(sampleRate),
                    frameBlock.count, 0, FrameType.KEY, ZERO_TAPE_TIMECODE);
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return new DemuxerTrackMeta(org.jcodec.common.TrackType.AUDIO, this.codec, 0, null, 0, null, null, AudioCodecMeta.createAudioCodecMeta("", 0, (int)this.channelCount, (int)this.sampleRate,
                    ByteOrder.LITTLE_ENDIAN, this.codec.isPcm(), null, null));
        }

        @Override
        public boolean gotoSyncFrame(long frame) {
            return gotoFrame(frame);
        }

        public String getLanguage() {
            return language;
        }

        public long getChannelCount() {
            return channelCount;
        }
    }

    public int getPictureWidth() {
        return pictureWidth;
    }

    public int getPictureHeight() {
        return pictureHeight;
    }

    @Override
    public List<DemuxerTrack> getAudioTracks() {
        return (List<DemuxerTrack>) (List) aTracks;
    }

    @Override
    public List<DemuxerTrack> getTracks() {
        ArrayList<DemuxerTrack> tracks = new ArrayList<DemuxerTrack>(aTracks);
        tracks.add(vTrack);
        tracks.addAll(subsTracks);
        return tracks;
    }

    @Override
    public List<DemuxerTrack> getVideoTracks() {
        ArrayList<DemuxerTrack> tracks = new ArrayList<DemuxerTrack>();
        tracks.add(vTrack);
        return tracks;
    }

    public List<DemuxerTrack> getSubtitleTracks() {
        return (List<DemuxerTrack>) (List) subsTracks;
    }

    public List<? extends EbmlBase> getTree() {
        return t;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
