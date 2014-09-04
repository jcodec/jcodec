package org.jcodec.containers.mxf;

import static org.jcodec.common.DemuxerTrackMeta.Type.AUDIO;
import static org.jcodec.common.DemuxerTrackMeta.Type.OTHER;
import static org.jcodec.common.DemuxerTrackMeta.Type.VIDEO;
import static org.jcodec.containers.mxf.MXFConst.klMetadataMapping;
import static org.jcodec.containers.mxf.model.MXFUtil.findAllMeta;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mxf.MXFConst.MXFCodecMapping;
import org.jcodec.containers.mxf.model.FileDescriptor;
import org.jcodec.containers.mxf.model.GenericDescriptor;
import org.jcodec.containers.mxf.model.GenericPictureEssenceDescriptor;
import org.jcodec.containers.mxf.model.GenericSoundEssenceDescriptor;
import org.jcodec.containers.mxf.model.IndexSegment;
import org.jcodec.containers.mxf.model.KLV;
import org.jcodec.containers.mxf.model.MXFMetadata;
import org.jcodec.containers.mxf.model.MXFPartition;
import org.jcodec.containers.mxf.model.MXFUtil;
import org.jcodec.containers.mxf.model.TimecodeComponent;
import org.jcodec.containers.mxf.model.TimelineTrack;
import org.jcodec.containers.mxf.model.UL;
import org.jcodec.containers.mxf.model.WaveAudioDescriptor;

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
        ch.position(0);
        parseHeader(ch);
        findIndex();
        tracks = findTracks();
        timecode = MXFUtil.findMeta(metadata, TimecodeComponent.class);
    }

    public enum OP {
        OP1a(1, 1), OP1b(1, 2), OP1c(1, 3), OP2a(2, 1), OP2b(2, 2), OP2c(2, 3), OP3a(3, 1), OP3b(3, 2), OP3c(3, 3), OPAtom(
                0x10, 0);

        public int major;
        public int minor;

        private OP(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }
    }

    public OP getOp() {
        UL op = header.getPack().getOp();

        EnumSet<OP> allOf = EnumSet.allOf(OP.class);
        for (OP op2 : allOf) {
            if (op.get(12) == op2.major && op.get(13) == op2.minor)
                return op2;
        }
        return OP.OPAtom;
    }

    private MXFDemuxerTrack[] findTracks() throws IOException {
        List<MXFDemuxerTrack> rt = new ArrayList<MXFDemuxerTrack>();
        List<TimelineTrack> tracks = findAllMeta(metadata, TimelineTrack.class);
        List<FileDescriptor> descriptors = findAllMeta(metadata, FileDescriptor.class);
        for (TimelineTrack track : tracks) {
            if (track.getTrackNumber() != 0) {
                int trackNumber = track.getTrackNumber();

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
                MXFDemuxerTrack dt = createTrack(new UL(0x06, 0x0e, 0x2b, 0x34, 0x01, 0x02, 0x01, 0x01, 0x0d, 0x01,
                        0x03, 0x01, (trackNumber >>> 24) & 0xff, (trackNumber >>> 16) & 0xff,
                        (trackNumber >>> 8) & 0xff, trackNumber & 0xff), track, descriptor);
                if (dt.getCodec() != null || (descriptor instanceof WaveAudioDescriptor))
                    rt.add(dt);
            }
        }

        return rt.toArray(new MXFDemuxerTrack[rt.size()]);
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
        return new MXFDemuxerTrack(ul, track, descriptor);
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
        ff.position(header.getPack().getFooterPartition());
        do {
            long thisPartition = ff.position();
            kl = KLV.readKL(ff);
            ByteBuffer fetchFrom = NIOUtils.fetchFrom(ff, (int) kl.len);
            header = MXFPartition.read(kl.key, fetchFrom, ff.position() - kl.offset, nextPartition);

            if (header.getPack().getNbEssenceContainers() > 0)
                partitions.add(0, header);

            metadata.addAll(0, readPartitionMeta(ff, header));

            ff.position(header.getPack().getPrevPartition());
            nextPartition = thisPartition;
        } while (header.getPack().getThisPartition() != 0);
    }

    public static List<MXFMetadata> readPartitionMeta(SeekableByteChannel ff, MXFPartition header) throws IOException {
        KLV kl;
        long basePos = ff.position();
        List<MXFMetadata> local = new ArrayList<MXFMetadata>();
        ByteBuffer metaBuffer = NIOUtils.fetchFrom(ff, (int) Math.max(0, header.getEssenceFilePos() - basePos));
        while (metaBuffer.hasRemaining() && (kl = KLV.readKL(metaBuffer, basePos)) != null) {
            MXFMetadata meta = parseMeta(kl.key, NIOUtils.read(metaBuffer, (int) kl.len));
            if (meta != null)
                local.add(meta);
        }
        return local;
    }

    public static MXFPartition readHeaderPartition(SeekableByteChannel ff) throws IOException {
        KLV kl;
        MXFPartition header = null;
        while ((kl = KLV.readKL(ff)) != null) {
            if (MXFConst.HEADER_PARTITION_KLV.equals(kl.key)) {
                ByteBuffer data = NIOUtils.fetchFrom(ff, (int) kl.len);
                header = MXFPartition.read(kl.key, data, ff.position() - kl.offset, 0);
                break;
            } else {
                ff.position(ff.position() + kl.len);
            }
        }
        return header;
    }

    private static MXFMetadata parseMeta(UL ul, ByteBuffer _bb) {
        Class<? extends MXFMetadata> class1 = klMetadataMapping.get(ul);
        if (class1 == null) {
            Logger.warn("Unknown metadata piece: " + ul);
            return null;
        }
        try {
            MXFMetadata meta = class1.getConstructor(UL.class).newInstance(ul);
            meta.read(_bb);
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
        return audio.toArray(new MXFDemuxerTrack[audio.size()]);
    }

    public class MXFDemuxerTrack implements SeekableDemuxerTrack {

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
        private MXFCodecMapping codec;
        private int audioFrameDuration;
        private int audioTimescale;

        public MXFDemuxerTrack(UL essenceUL, TimelineTrack track, GenericDescriptor descriptor) throws IOException {
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
                    cacheAudioFrameSizes(ch);
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
            return duration;
        }

        public int getNumFrames() {
            return totalFrames;
        }

        public String getName() {
            return track.getName();
        }

        private void cacheAudioFrameSizes(SeekableByteChannel ch) throws IOException {
            for (MXFPartition mxfPartition : partitions) {
                if (mxfPartition.getEssenceLength() > 0) {
                    ch.position(mxfPartition.getEssenceFilePos());
                    KLV kl;
                    do {
                        kl = KLV.readKL(ch);
                        if (kl == null)
                            break;
                        ch.position(ch.position() + kl.len);
                    } while (!essenceUL.equals(kl.key));

                    if (essenceUL.equals(kl.key)) {
                        dataLen = (int) kl.len;
                        break;
                    }
                }
            }
        }

        @Override
        public Packet nextFrame() throws IOException {
            if (indexSegmentIdx >= indexSegments.size())
                return null;

            IndexSegment seg = indexSegments.get(indexSegmentIdx);

            long[] off = seg.getIe().getFileOff();
            int erDen = seg.getIndexEditRateNum();
            int erNum = seg.getIndexEditRateDen();

            long frameEssenceOffset = off[indexSegmentSubIdx];

            byte toff = seg.getIe().getDisplayOff()[indexSegmentSubIdx];
            boolean kf = seg.getIe().getKeyFrameOff()[indexSegmentSubIdx] == 0;

            while (frameEssenceOffset >= partEssenceOffset + partitions.get(partIdx).getEssenceLength()
                    && partIdx < partitions.size() - 1) {
                partEssenceOffset += partitions.get(partIdx).getEssenceLength();
                partIdx++;
            }

            long frameFileOffset = frameEssenceOffset - partEssenceOffset + partitions.get(partIdx).getEssenceFilePos();

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

                if (dataLen == 0 && indexSegmentIdx < indexSegments.size()) {
                    IndexSegment nseg = indexSegments.get(indexSegmentIdx);
                    pts = pts * nseg.getIndexEditRateNum() / erDen;
                }
            }

            return result;
        }

        public MXFPacket readPacket(long off, int len, long pts, int timescale, int duration, int frameNo, boolean kf)
                throws IOException {
            synchronized (ch) {
                ch.position(off);

                KLV kl = KLV.readKL(ch);
                while (kl != null && !essenceUL.equals(kl.key)) {
                    ch.position(ch.position() + kl.len);
                    kl = KLV.readKL(ch);
                }

                return kl != null && essenceUL.equals(kl.key) ? new MXFPacket(NIOUtils.fetchFrom(ch, (int) kl.len),
                        pts, timescale, duration, frameNo, kf, null, off, len) : null;
            }
        }

        @Override
        public boolean gotoFrame(long frameNo) {
            if (frameNo == this.frameNo)
                return true;
            indexSegmentSubIdx = (int) frameNo;
            for (indexSegmentIdx = 0; indexSegmentIdx < indexSegments.size()
                    && indexSegmentSubIdx >= indexSegments.get(indexSegmentIdx).getIndexDuration(); indexSegmentIdx++) {
                indexSegmentSubIdx -= indexSegments.get(indexSegmentIdx).getIndexDuration();
            }
            indexSegmentSubIdx = Math.min(indexSegmentSubIdx, (int) indexSegments.get(indexSegmentIdx)
                    .getIndexDuration());

            return true;
        }

        @Override
        public long getCurFrame() {
            return frameNo;
        }

        @Override
        public void seek(double second) {
            throw new UnsupportedOperationException();
        }

        public UL getEssenceUL() {
            return essenceUL;
        }

        public GenericDescriptor getDescriptor() {
            return descriptor;
        }

        public MXFCodecMapping getCodec() {
            return codec;
        }

        private MXFCodecMapping resolveCodec() {
            UL codecUL;
            if (video)
                codecUL = ((GenericPictureEssenceDescriptor) descriptor).getPictureEssenceCoding();
            else if (audio)
                codecUL = ((GenericSoundEssenceDescriptor) descriptor).getSoundEssenceCompression();
            else
                return null;

            for (MXFCodecMapping codec : EnumSet.allOf(MXFConst.MXFCodecMapping.class)) {
                if (codec.getUl().equals(codecUL, 0xff7f))
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

            return new DemuxerTrackMeta(video ? VIDEO : (audio ? AUDIO : OTHER), null, totalFrames, duration, size);
        }
    }

    public static class MXFPacket extends Packet {
        private long offset;
        private int len;

        public MXFPacket(ByteBuffer data, long pts, long timescale, long duration, long frameNo, boolean keyFrame,
                TapeTimecode tapeTimecode, long offset, int len) {
            super(data, pts, timescale, duration, frameNo, keyFrame, tapeTimecode);
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

        public void parseHeader(SeekableByteChannel ff) throws IOException {
            partitions = new ArrayList<MXFPartition>();
            metadata = new ArrayList<MXFMetadata>();

            header = readHeaderPartition(ff);
            metadata.addAll(readPartitionMeta(ff, header));
            partitions.add(header);

            ff.position(header.getPack().getFooterPartition());
            KLV kl = KLV.readKL(ff);
            ByteBuffer fetchFrom = NIOUtils.fetchFrom(ff, (int) kl.len);
            MXFPartition footer = MXFPartition.read(kl.key, fetchFrom, ff.position() - kl.offset, ff.size());

            metadata.addAll(readPartitionMeta(ff, footer));
        }
    }
}