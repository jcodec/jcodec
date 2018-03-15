package org.jcodec.containers.mxf;

import static java.util.Collections.unmodifiableList;
import static org.jcodec.containers.mxf.MXFConst.klMetadata;
import static org.jcodec.containers.mxf.model.MXFUtil.findAllMeta;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jcodec.api.NotSupportedException;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mxf.model.FileDescriptor;
import org.jcodec.containers.mxf.model.GenericDescriptor;
import org.jcodec.containers.mxf.model.GenericPictureEssenceDescriptor;
import org.jcodec.containers.mxf.model.GenericSoundEssenceDescriptor;
import org.jcodec.containers.mxf.model.IndexSegment;
import org.jcodec.containers.mxf.model.KLV;
import org.jcodec.containers.mxf.model.MXFMetadata;
import org.jcodec.containers.mxf.model.MXFPartition;
import org.jcodec.containers.mxf.model.MXFUtil;
import org.jcodec.containers.mxf.model.SourceClip;
import org.jcodec.containers.mxf.model.TimecodeComponent;
import org.jcodec.containers.mxf.model.TimelineTrack;
import org.jcodec.containers.mxf.model.UL;
import org.jcodec.containers.mxf.model.WaveAudioDescriptor;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MXF demuxer
 * 
 * @author The JCodec project
 * 
 */
public class MXFDemuxer {

    protected List<MXFMetadata> metadata;
    protected MXFPartition header;
    protected List<MXFPartition> partitions;
    protected List<IndexSegment> indexSegments;
    protected SeekableByteChannel ch;
    protected MXFDemuxerTrack[] tracks;
    protected int totalFrames;
    protected double duration;
    protected TimecodeComponent timecode;

    public MXFDemuxer(SeekableByteChannel ch) throws IOException {
        this.ch = ch;
        ch.setPosition(0);
        parseHeader(ch);
        findIndex();
        tracks = findTracks();
        timecode = MXFUtil.findMeta(metadata, TimecodeComponent.class);
    }

    public static final class OP {
        public final static OP OP1a = new OP(1, 1);
        public final static OP OP1b = new OP(1, 2);
        public final static OP OP1c = new OP(1, 3);
        public final static OP OP2a = new OP(2, 1);
        public final static OP OP2b = new OP(2, 2);
        public final static OP OP2c = new OP(2, 3);
        public final static OP OP3a = new OP(3, 1);
        public final static OP OP3b = new OP(3, 2);
        public final static OP OP3c = new OP(3, 3);
        public final static OP OPAtom = new OP(0x10, 0);

        private final static OP[] _values = new OP[] { OP1a, OP1b, OP1c, OP2a, OP2b, OP2c, OP3a, OP3b, OP3c, OPAtom };
        public int major;
        public int minor;

        private OP(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        public static OP[] values() {
            return _values;
        }
    }

    public OP getOp() {
        UL op = header.getPack().getOp();

        OP[] values = OP.values();
        for (int i = 0; i < values.length; i++) {
            OP op2 = values[i];
            if (op.get(12) == op2.major && op.get(13) == op2.minor)
                return op2;
        }
        return OP.OPAtom;
    }

    public MXFDemuxerTrack[] findTracks() throws IOException {
        List<TimelineTrack> _tracks = findAllMeta(metadata, TimelineTrack.class);
        List<FileDescriptor> descriptors = findAllMeta(metadata, FileDescriptor.class);
        Map<Integer, MXFDemuxerTrack> tracks = new LinkedHashMap<Integer, MXFDemuxerTrack>();
        for (TimelineTrack track : _tracks) {
            //SMPTE S377-1-2009
            //A Track ID value of zero (0) is deprecated, and shall not be used by MXF encoders conforming to this version of the MXF file format specification. 
            if (track.getTrackId() == 0 || track.getTrackNumber() == 0) {
                Logger.warn("trackId == 0 || trackNumber == 0");
                continue;
            }
            int trackId = track.getTrackId();
            if (tracks.containsKey(trackId)) {
                Logger.warn("duplicate trackId " + trackId);
                continue;
            }

            FileDescriptor descriptor = findDescriptor(descriptors, track.getTrackId());
            if (descriptor == null) {
                Logger.warn("No generic descriptor for track: " + track.getTrackId());
                if (descriptors.size() == 1 && descriptors.get(0).getLinkedTrackId() == 0) {
                    descriptor = descriptors.get(0);
                }
            }
            if (descriptor == null) {
                Logger.warn("Track without descriptor: " + track.getTrackId());
                continue;
            }
            int trackNumber = track.getTrackNumber();
            UL ul = UL.newULFromInts(new int[]{0x06, 0x0e, 0x2b, 0x34, 0x01, 0x02, 0x01, 0x01, 0x0d, 0x01, 0x03, 0x01,
                    (trackNumber >>> 24) & 0xff, (trackNumber >>> 16) & 0xff, (trackNumber >>> 8) & 0xff,
                    trackNumber & 0xff});
            MXFDemuxerTrack dt = createTrack(ul, track, descriptor);
            if (dt.getCodec() != null || (descriptor instanceof WaveAudioDescriptor)) {
                tracks.put(trackId, dt);
            }
        }

        return tracks.values().toArray(new MXFDemuxerTrack[tracks.size()]);
    }

    public static FileDescriptor findDescriptor(List<FileDescriptor> descriptors, int trackId) {
        for (FileDescriptor descriptor : descriptors) {
            if (descriptor.getLinkedTrackId() == trackId) {
                return descriptor;
            }
        }
        return null;
    }

    protected MXFDemuxerTrack createTrack(UL ul, TimelineTrack track, GenericDescriptor descriptor) throws IOException {
        return new MXFDemuxerTrack(this, ul, track, descriptor);
    }

    public List<IndexSegment> getIndexes() {
        return indexSegments;
    }

    public List<MXFPartition> getEssencePartitions() {
        return partitions;
    }

    public TimecodeComponent getTimecode() {
        return timecode;
    }

    public void parseHeader(SeekableByteChannel ff) throws IOException {
        KLV kl;
        header = readHeaderPartition(ff);

        metadata = new ArrayList<MXFMetadata>();

        partitions = new ArrayList<MXFPartition>();
        long nextPartition = ff.size();
        ff.setPosition(header.getPack().getFooterPartition());
        do {
            long thisPartition = ff.position();
            kl = KLV.readKL(ff);
            ByteBuffer fetchFrom = NIOUtils.fetchFromChannel(ff, (int) kl.len);
            header = MXFPartition.read(kl.key, fetchFrom, ff.position() - kl.offset, nextPartition);

            if (header.getPack().getNbEssenceContainers() > 0)
                partitions.add(0, header);

            metadata.addAll(0, readPartitionMeta(ff, header));

            ff.setPosition(header.getPack().getPrevPartition());
            nextPartition = thisPartition;
        } while (header.getPack().getThisPartition() != 0);
    }

    public static List<MXFMetadata> readPartitionMeta(SeekableByteChannel ff, MXFPartition header) throws IOException {
        KLV kl;
        long basePos = ff.position();
        List<MXFMetadata> local = new ArrayList<MXFMetadata>();
        ByteBuffer metaBuffer = NIOUtils.fetchFromChannel(ff, (int) Math.max(0, header.getEssenceFilePos() - basePos));
        while (metaBuffer.hasRemaining() && (kl = KLV.readKLFromBuffer(metaBuffer, basePos)) != null) {
            if (metaBuffer.remaining() >= kl.len) {
                MXFMetadata meta = parseMeta(kl.key, NIOUtils.read(metaBuffer, (int) kl.len));
                if (meta != null)
                    local.add(meta);
            } else {
                break;
            }
        }
        return local;
    }

    public static MXFPartition readHeaderPartition(SeekableByteChannel ff) throws IOException {
        KLV kl;
        MXFPartition header = null;
        while ((kl = KLV.readKL(ff)) != null) {
            if (MXFConst.HEADER_PARTITION_KLV.equals(kl.key)) {
                ByteBuffer data = NIOUtils.fetchFromChannel(ff, (int) kl.len);
                header = MXFPartition.read(kl.key, data, ff.position() - kl.offset, 0);
                break;
            } else {
                ff.setPosition(ff.position() + kl.len);
            }
        }
        return header;
    }

    private static MXFMetadata parseMeta(UL ul, ByteBuffer _bb) {
        Class<? extends MXFMetadata> class1 = klMetadata.get(ul);
        if (class1 == null) {
            Logger.warn("Unknown metadata piece: " + ul);
            return null;
        }
        try {
            MXFMetadata meta = Platform.newInstance(class1, new Object[]{ul});
            meta.readBuf(_bb);
            return meta;
        } catch (Exception e) {
        }
        Logger.warn("Unknown metadata piece: " + ul);
        return null;
    }

    private void findIndex() {
        indexSegments = new ArrayList<IndexSegment>();
        for (MXFMetadata meta : metadata) {
            if (meta instanceof IndexSegment) {
                IndexSegment is = (IndexSegment) meta;
                indexSegments.add(is);
                totalFrames += is.getIndexDuration();
                duration += ((double) is.getIndexEditRateDen() * is.getIndexDuration()) / is.getIndexEditRateNum();
            }
        }
    }

    public MXFDemuxerTrack[] getTracks() {
        return tracks;
    }

    public MXFDemuxerTrack getVideoTrack() {
        for (MXFDemuxerTrack track : tracks) {
            if (track.isVideo())
                return track;
        }
        return null;
    }

    public MXFDemuxerTrack[] getAudioTracks() {
        List<MXFDemuxerTrack> audio = new ArrayList<MXFDemuxerTrack>();
        for (MXFDemuxerTrack track : tracks) {
            if (track.isAudio())
                audio.add(track);
        }
        return audio.toArray(new MXFDemuxerTrack[0]);
    }

    public static class MXFDemuxerTrack implements SeekableDemuxerTrack {

        private UL essenceUL;
        private int dataLen;
        private int indexSegmentIdx;
        private int indexSegmentSubIdx;
        private int frameNo;
        private long pts;
        private int partIdx;
        private long partEssenceOffset;
        private GenericDescriptor descriptor;
        private TimelineTrack track;
        private boolean video;
        private boolean audio;
        private MXFCodec codec;
        private int audioFrameDuration;
        private int audioTimescale;
        private MXFDemuxer demuxer;

        public MXFDemuxerTrack(MXFDemuxer demuxer, UL essenceUL, TimelineTrack track, GenericDescriptor descriptor)
                throws IOException {
            this.demuxer = demuxer;
            this.essenceUL = essenceUL;
            this.track = track;
            this.descriptor = descriptor;

            if (descriptor instanceof GenericPictureEssenceDescriptor)
                video = true;
            else if (descriptor instanceof GenericSoundEssenceDescriptor)
                audio = true;
            codec = resolveCodec();

            if (codec != null || (descriptor instanceof WaveAudioDescriptor)) {
                Logger.warn("Track type: " + video + ", " + audio);

                if (audio && (descriptor instanceof WaveAudioDescriptor)) {
                    WaveAudioDescriptor wave = (WaveAudioDescriptor) descriptor;
                    cacheAudioFrameSizes(demuxer.ch);
                    audioFrameDuration = dataLen / ((wave.getQuantizationBits() >> 3) * wave.getChannelCount());
                    audioTimescale = (int) wave.getAudioSamplingRate().scalar();
                }
            }
        }

        public boolean isAudio() {
            return audio;
        }

        public boolean isVideo() {
            return video;
        }

        public double getDuration() {
            return demuxer.duration;
        }

        public int getNumFrames() {
            return demuxer.totalFrames;
        }

        public String getName() {
            return track.getName();
        }

        private void cacheAudioFrameSizes(SeekableByteChannel ch) throws IOException {
            for (MXFPartition mxfPartition : demuxer.partitions) {
                if (mxfPartition.getEssenceLength() > 0) {
                    ch.setPosition(mxfPartition.getEssenceFilePos());
                    KLV kl;
                    do {
                        kl = KLV.readKL(ch);
                        if (kl == null)
                            break;
                        ch.setPosition(ch.position() + kl.len);
                    } while (!essenceUL.equals(kl.key));

                    if (kl != null && essenceUL.equals(kl.key)) {
                        dataLen = (int) kl.len;
                        break;
                    }
                }
            }
        }

        @Override
        public Packet nextFrame() throws IOException {
            if (indexSegmentIdx >= demuxer.indexSegments.size())
                return null;

            IndexSegment seg = demuxer.indexSegments.get(indexSegmentIdx);

            long[] off = seg.getIe().getFileOff();
            int erDen = seg.getIndexEditRateNum();
            int erNum = seg.getIndexEditRateDen();

            long frameEssenceOffset = off[indexSegmentSubIdx];

            byte toff = seg.getIe().getDisplayOff()[indexSegmentSubIdx];
            boolean kf = seg.getIe().getKeyFrameOff()[indexSegmentSubIdx] == 0;

            while (frameEssenceOffset >= partEssenceOffset + demuxer.partitions.get(partIdx).getEssenceLength()
                    && partIdx < demuxer.partitions.size() - 1) {
                partEssenceOffset += demuxer.partitions.get(partIdx).getEssenceLength();
                partIdx++;
            }

            long frameFileOffset = frameEssenceOffset - partEssenceOffset
                    + demuxer.partitions.get(partIdx).getEssenceFilePos();

            Packet result;
            if (!audio) {
                result = readPacket(frameFileOffset, dataLen, pts + erNum * toff, erDen, erNum, frameNo++, kf);
                pts += erNum;
            } else {
                result = readPacket(frameFileOffset, dataLen, pts, audioTimescale, audioFrameDuration, frameNo++, kf);
                pts += audioFrameDuration;
            }

            indexSegmentSubIdx++;

            if (indexSegmentSubIdx >= off.length) {
                indexSegmentIdx++;
                indexSegmentSubIdx = 0;

                if (dataLen == 0 && indexSegmentIdx < demuxer.indexSegments.size()) {
                    IndexSegment nseg = demuxer.indexSegments.get(indexSegmentIdx);
                    pts = pts * nseg.getIndexEditRateNum() / erDen;
                }
            }

            return result;
        }

        public MXFPacket readPacket(long off, int len, long pts, int timescale, int duration, int frameNo, boolean kf)
                throws IOException {
            SeekableByteChannel ch = demuxer.ch;
            synchronized (ch) {
                ch.setPosition(off);

                KLV kl = KLV.readKL(ch);
                while (kl != null && !essenceUL.equals(kl.key)) {
                    ch.setPosition(ch.position() + kl.len);
                    kl = KLV.readKL(ch);
                }

                return kl != null && essenceUL.equals(kl.key)
                        ? new MXFPacket(NIOUtils.fetchFromChannel(ch, (int) kl.len), pts, timescale, duration, frameNo,
                                kf ? FrameType.KEY : FrameType.INTER, null, off, len)
                        : null;
            }
        }

        @Override
        public boolean gotoFrame(long frameNo) {
            if (frameNo == this.frameNo)
                return true;
            indexSegmentSubIdx = (int) frameNo;
            for (indexSegmentIdx = 0; indexSegmentIdx < demuxer.indexSegments.size()
                    && indexSegmentSubIdx >= demuxer.indexSegments.get(indexSegmentIdx)
                            .getIndexDuration(); indexSegmentIdx++) {
                indexSegmentSubIdx -= demuxer.indexSegments.get(indexSegmentIdx).getIndexDuration();
            }
            indexSegmentSubIdx = Math.min(indexSegmentSubIdx,
                    (int) demuxer.indexSegments.get(indexSegmentIdx).getIndexDuration());

            return true;
        }

        @Override
        public boolean gotoSyncFrame(long frameNo) {
            if (!gotoFrame(frameNo))
                return false;
            IndexSegment seg = demuxer.indexSegments.get(indexSegmentIdx);
            byte kfOff = seg.getIe().getKeyFrameOff()[indexSegmentSubIdx];
            return gotoFrame(frameNo + kfOff);
        }

        @Override
        public long getCurFrame() {
            return frameNo;
        }

        @Override
        public void seek(double second) {
            throw new NotSupportedException("");
        }

        public UL getEssenceUL() {
            return essenceUL;
        }

        public GenericDescriptor getDescriptor() {
            return descriptor;
        }

        public MXFCodec getCodec() {
            return codec;
        }

        private MXFCodec resolveCodec() {
            UL codecUL;
            if (video)
                codecUL = ((GenericPictureEssenceDescriptor) descriptor).getPictureEssenceCoding();
            else if (audio)
                codecUL = ((GenericSoundEssenceDescriptor) descriptor).getSoundEssenceCompression();
            else
                return null;

            MXFCodec[] values = MXFCodec.values();
            for (int i = 0; i < values.length; i++) {
                MXFCodec codec = values[i];
                if (codec.getUl().equals(codecUL))
                    return codec;
            }
            Logger.warn("Unknown codec: " + codecUL);

            return null;
        }

        public int getTrackId() {
            return track.getTrackId();
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            Size size = null;
            if (video) {
                GenericPictureEssenceDescriptor pd = (GenericPictureEssenceDescriptor) descriptor;
                size = new Size(pd.getStoredWidth(), pd.getStoredHeight());
            }

            TrackType t = video ? TrackType.VIDEO : (audio ? TrackType.AUDIO : TrackType.OTHER);
            return new DemuxerTrackMeta(t, getCodec().getCodec(), demuxer.duration, null, demuxer.totalFrames, null,
                    org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta(size, ColorSpace.YUV420), null);
        }

        public Rational getEditRate() {
            return this.track.getEditRate();
        }
    }

    public static class MXFPacket extends Packet {
        private long offset;
        private int len;

        public MXFPacket(ByteBuffer data, long pts, int timescale, long duration, long frameNo, FrameType frameType,
                TapeTimecode tapeTimecode, long offset, int len) {
            super(data, pts, timescale, duration, frameNo, frameType, tapeTimecode, 0);
            this.offset = offset;
            this.len = len;
        }

        public long getOffset() {
            return offset;
        }

        public int getLen() {
            return len;
        }
    }

    /**
     * Fast loading version of demuxer, doesn't search for metadata in ALL the
     * partitions, only the header and footer are being inspected
     */
    public static class Fast extends MXFDemuxer {

        public Fast(SeekableByteChannel ch) throws IOException {
            super(ch);
        }

        @Override
        public void parseHeader(SeekableByteChannel ff) throws IOException {
            partitions = new ArrayList<MXFPartition>();
            metadata = new ArrayList<MXFMetadata>();

            header = readHeaderPartition(ff);
            metadata.addAll(readPartitionMeta(ff, header));
            partitions.add(header);

            ff.setPosition(header.getPack().getFooterPartition());
            KLV kl = KLV.readKL(ff);
            if (kl != null) {
                ByteBuffer fetchFrom = NIOUtils.fetchFromChannel(ff, (int) kl.len);
                MXFPartition footer = MXFPartition.read(kl.key, fetchFrom, ff.position() - kl.offset, ff.size());

                metadata.addAll(readPartitionMeta(ff, footer));
            }
        }
    }

    public List<SourceClip> getSourceClips(int trackId) {
        boolean trackFound = true;
        List<SourceClip> clips = new ArrayList<SourceClip>();
        for (MXFMetadata m : metadata) {
            if (m instanceof TimelineTrack) {
                TimelineTrack tt = (TimelineTrack) m;
                int trackId2 = tt.getTrackId();
                trackFound = (trackId2 == trackId);
            }
            if (trackFound && m instanceof SourceClip) {
                SourceClip clip = (SourceClip) m;
                if (clip.getSourceTrackId() == trackId) {
                    clips.add(clip);
                }
            }
        }
        return clips;
    }

    public static TapeTimecode readTapeTimecode(File mxf) throws IOException {
        FileChannelWrapper read = NIOUtils.readableChannel(mxf);
        try {
            Fast fast = new Fast(read);
            MXFDemuxerTrack track = fast.getVideoTrack();
            TimecodeComponent timecode = fast.getTimecode();
            List<SourceClip> sourceClips = fast.getSourceClips(track.getTrackId());
            long tc = 0;
            boolean dropFrame = false;
            Rational editRate = null;
            if (timecode != null) {
                editRate = track.getEditRate();
                dropFrame = timecode.getDropFrame() != 0;
                tc = timecode.getStart();
            }
            for (SourceClip sourceClip : sourceClips) {
                tc += sourceClip.getStartPosition();
            }
    
            if (editRate != null) {
                return TapeTimecode.tapeTimecode(tc, dropFrame, (int) Math.ceil(editRate.toDouble()));
            }
            return null;
        } finally {
            read.close();
        }
    }

    public List<MXFMetadata> getMetadata() {
        return unmodifiableList(metadata);
    }
}