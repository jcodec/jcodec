package org.jcodec.containers.mxf;

import static org.jcodec.containers.mxf.MXFConst.klMetadataMapping;
import static org.jcodec.containers.mxf.read.MXFUtil.findAllMeta;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mxf.MXFConst.MXFCodecMapping;
import org.jcodec.containers.mxf.read.GenericDescriptor;
import org.jcodec.containers.mxf.read.IndexSegment;
import org.jcodec.containers.mxf.read.KLV;
import org.jcodec.containers.mxf.read.MXFMetadata;
import org.jcodec.containers.mxf.read.MXFPartition;
import org.jcodec.containers.mxf.read.MXFUtil;
import org.jcodec.containers.mxf.read.TimecodeComponent;
import org.jcodec.containers.mxf.read.Track;
import org.jcodec.containers.mxf.read.UL;

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

    private List<IndexSegment> indexSegments;
    private List<MXFPartition> partitions;
    private SeekableByteChannel ch;
    private MXFDemuxerTrack[] tracks;
    private List<MXFMetadata> metadata;
    private int totalFrames;
    private double duration;
    private TimecodeComponent timecode;

    public MXFDemuxer(SeekableByteChannel ch) throws IOException {
        this.ch = ch;
        ch.position(0);
        parseHeader(ch);
        findIndex();
        tracks = findTracks();
        timecode = MXFUtil.findMeta(metadata, TimecodeComponent.class);
    }

    private MXFDemuxerTrack[] findTracks() throws IOException {
        List<MXFDemuxerTrack> rt = new ArrayList<MXFDemuxerTrack>();
        List<Track> findMeta = findAllMeta(metadata, Track.class);
        List<GenericDescriptor> descriptors = findAllMeta(metadata, GenericDescriptor.class);
        for (Track track : findMeta) {
            if (track.getTrackNumber() != 0) {
                int trackNumber = track.getTrackNumber();

                GenericDescriptor descriptor = findDescriptor(descriptors, track.getTrackId());
                if (descriptor == null) {
                    System.out.println("No generic descriptor for track: " + track.getTrackId());
                    continue;
                }
                MXFDemuxerTrack dt = createTrack(new UL(0x06, 0x0e, 0x2b, 0x34, 0x01, 0x02, 0x01, 0x01, 0x0d, 0x01,
                        0x03, 0x01, (trackNumber >>> 24) & 0xff, (trackNumber >>> 16) & 0xff,
                        (trackNumber >>> 8) & 0xff, trackNumber & 0xff), track, descriptor);
                if (dt.getCodec() != null)
                    rt.add(dt);
            }
        }

        return rt.toArray(new MXFDemuxerTrack[0]);
    }

    private GenericDescriptor findDescriptor(List<GenericDescriptor> descriptors, int trackId) {
        for (GenericDescriptor descriptor : descriptors) {
            if (descriptor.getLinkedTrackId() == trackId) {
                return descriptor;
            }
        }
        return null;
    }

    protected MXFDemuxerTrack createTrack(UL ul, Track track, GenericDescriptor descriptor) throws IOException {
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
        MXFPartition header = readHeaderPartition(ff);

        metadata = new ArrayList<MXFMetadata>();

        partitions = new ArrayList<MXFPartition>();
        long nextPartition = ff.size();
        ff.position(header.getPack().getFooterPartition());
        do {
            List<MXFMetadata> local = new ArrayList<MXFMetadata>();
            long thisPartition = ff.position();
            kl = KLV.readKL(ff);
            ByteBuffer fetchFrom = NIOUtils.fetchFrom(ff, (int) kl.len);
            header = MXFPartition.read(kl.key, fetchFrom, ff.position() - kl.offset, nextPartition);

            if (header.getPack().getNbEssenceContainers() > 0)
                partitions.add(0, header);

            long basePos = ff.position();
            ByteBuffer metaBuffer = NIOUtils.fetchFrom(ff, (int) Math.max(0, header.getEssenceFilePos() - basePos));
            while (metaBuffer.hasRemaining() && (kl = KLV.readKL(metaBuffer, basePos)) != null) {
                MXFMetadata meta = parseMeta(kl.key, NIOUtils.read(metaBuffer, (int) kl.len));
                if (meta != null)
                    local.add(meta);
            }

            ff.position(header.getPack().getPrevPartition());
            nextPartition = thisPartition;
            metadata.addAll(0, local);
        } while (header.getPack().getThisPartition() != 0);
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

    private MXFMetadata parseMeta(UL ul, ByteBuffer _bb) {
        Class<? extends MXFMetadata> class1 = klMetadataMapping.get(ul);
        if (class1 == null)
            return null;
        try {
            MXFMetadata meta = class1.getConstructor(UL.class).newInstance(ul);
            meta.read(_bb);
            return meta;
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (IllegalArgumentException e) {
        } catch (SecurityException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
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

    public class MXFDemuxerTrack implements DemuxerTrack {

        private UL essenceUL;
        private int dataLen;
        private int indexSegmentIdx;
        private int indexSegmentSubIdx;
        private int frameNo;
        private long pts;
        private int partIdx;
        private long partEssenceOffset;
        private GenericDescriptor descriptor;
        private Track track;
        private boolean video;
        private boolean audio;
        private MXFCodecMapping codec;
        private int audioFrameDuration;
        private int audioTimescale;

        public MXFDemuxerTrack(UL essenceUL, Track track, GenericDescriptor descriptor) throws IOException {
            this.essenceUL = essenceUL;
            this.track = track;
            this.descriptor = descriptor;

            if (descriptor.getPictureUl() != null)
                video = true;
            else if (descriptor.getSoundUl() != null)
                audio = true;
            codec = resolveCodec();

            if (codec != null) {
                System.out.println("Track type: " + video + ", " + audio);

                if (audio && codec.getCodec() == null) {
                    cacheAudioFrameSizes(ch);
                    audioFrameDuration = dataLen / ((descriptor.getBitsPerSample() >> 3) * descriptor.getChannels());
                    audioTimescale = descriptor.getSrNum() / descriptor.getSrDen();
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

            long[] off = seg.getIe().getOff();
            int erDen = seg.getIndexEditRateNum();
            int erNum = seg.getIndexEditRateDen();

            long frameEssenceOffset = off[indexSegmentSubIdx];

            while (frameEssenceOffset >= partEssenceOffset + partitions.get(partIdx).getEssenceLength()
                    && partIdx < partitions.size() - 1) {
                partEssenceOffset += partitions.get(partIdx).getEssenceLength();
                partIdx++;
            }

            long frameFileOffset = frameEssenceOffset - partEssenceOffset + partitions.get(partIdx).getEssenceFilePos();

            Packet result;
            if (!audio) {
                result = readPacket(frameFileOffset, dataLen, pts, erDen, erNum, frameNo++);
                pts += erNum;
            } else {
                result = readPacket(frameFileOffset, dataLen, pts, audioTimescale, audioFrameDuration, frameNo++);
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

        public MXFPacket readPacket(long off, int len, long pts, int timescale, int duration, int frameNo)
                throws IOException {
            synchronized (ch) {
                ch.position(off);

                KLV kl = KLV.readKL(ch);
                while (kl != null && !essenceUL.equals(kl.key)) {
                    ch.position(ch.position() + kl.len);
                    kl = KLV.readKL(ch);
                }

                return kl != null && essenceUL.equals(kl.key) ? new MXFPacket(NIOUtils.fetchFrom(ch, (int) kl.len),
                        pts, timescale, duration, frameNo, true, null, off, len) : null;
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
            for (MXFCodecMapping codec : EnumSet.allOf(MXFConst.MXFCodecMapping.class)) {
                if (codec.getUl().equals(descriptor.getPictureUl(), 0xff7f)
                        || codec.getUl().equals(descriptor.getSoundUl(), 0xff7f))
                    return codec;
            }
            System.out.println("Unknown codec: "
                    + (descriptor.getPictureUl() != null ? descriptor.getPictureUl() : descriptor.getSoundUl()));

            return null;
        }

        public int getTrackId() {
            return track.getTrackId();
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
}