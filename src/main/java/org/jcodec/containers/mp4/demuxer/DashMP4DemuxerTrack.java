package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.common.TrackType.AUDIO;
import static org.jcodec.common.TrackType.OTHER;
import static org.jcodec.common.TrackType.VIDEO;
import static org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.ArrayUtil;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.TrackFragmentBaseMediaDecodeTimeBox;
import org.jcodec.containers.mp4.boxes.TrackFragmentBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.TrunBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxes one track out of multiple DASH fragments
 * 
 * @author The JCodec project
 * 
 */
public class DashMP4DemuxerTrack implements SeekableDemuxerTrack, Closeable {

    private int[][] sizes;
    private int[][] compOffsets;
    private long[] chunkOffsets;
    private int[] avgDur;
    private int[][] sampleDurations;
    private long offInChunk;
    private SeekableByteChannel[] inputs;
    private int curFrame;
    private int curFrag;
    private long globalFrame;
    private long pts;
    private int frameCount;
    private long totalDuration;
    private TrakBox trak;
    private SampleEntry[] sampleEntries;
    private double durationHint;

    public static class Fragment {
        TrackFragmentBox frag;
        long offset;
        SeekableByteChannel input;

        public Fragment(TrackFragmentBox frag, long offset, SeekableByteChannel input) {
            this.frag = frag;
            this.offset = offset;
            this.input = input;
        }
    }

    public static class FragmentComparator implements Comparator<Fragment> {
        @Override
        public int compare(Fragment arg0, Fragment arg1) {
            long a = arg0.frag.getTfdt() == null ? 0 : arg0.frag.getTfdt().getBaseMediaDecodeTime();
            long b = arg1.frag.getTfdt() == null ? 0 : arg1.frag.getTfdt().getBaseMediaDecodeTime();
            return a < b ? -1 : (a == b ? 0 : 1);
        }
    }

    public DashMP4DemuxerTrack(MovieBox mov, TrakBox trak, Fragment[] fragments) {
        long prevOffset = 0;
        int prevSampleCount = 0;
        sizes = new int[fragments.length][];
        compOffsets = new int[fragments.length][];
        chunkOffsets = new long[fragments.length];
        avgDur = new int[fragments.length];
        sampleDurations = new int[fragments.length][];
        this.trak = trak;
        inputs = new SeekableByteChannel[fragments.length];
        sampleEntries = NodeBox.findAllPath(trak, SampleEntry.class,
                new String[] { "mdia", "minf", "stbl", "stsd", null });

        if (sortable(fragments))
            Arrays.sort(fragments, 0, fragments.length, new FragmentComparator());

        int i = 0;
        for (Fragment fragment : fragments) {
            TrackFragmentBox frag = fragment.frag;
            TrunBox trun = frag.getTrun();
            TrackFragmentBaseMediaDecodeTimeBox tfdt = frag.getTfdt();
            if (tfdt != null) {
                if (i > 0) {
                    avgDur[i - 1] = (int) ((tfdt.getBaseMediaDecodeTime() - prevOffset) / prevSampleCount);
                    totalDuration = tfdt.getBaseMediaDecodeTime();
                }
                prevOffset = tfdt.getBaseMediaDecodeTime();
            }
            prevSampleCount = (int) trun.getSampleCount();
            sizes[i] = trun.getSampleSizes();
            compOffsets[i] = trun.getSampleCompositionOffsets();
            chunkOffsets[i] = fragment.offset + frag.getTfhd().getBaseDataOffset() + frag.getTrun().getDataOffset();
            if (trun.isSampleDurationAvailable()) {
                sampleDurations[i] = trun.getSampleDurations();
                totalDuration += ArrayUtil.sumInt(sampleDurations[i]);
            }
            frameCount += sizes[i].length;
            inputs[i] = fragment.input;
            i++;
        }
        if (avgDur.length > 1) {
            avgDur[avgDur.length - 1] = avgDur[avgDur.length - 2];
            totalDuration += avgDur[avgDur.length - 1] * prevSampleCount;
        }
    }

    private boolean sortable(Fragment[] fragments) {
        for (Fragment fragment : fragments) {
            if (fragment.frag.getTfdt() == null)
                return false;
        }
        return true;
    }

    public static DashMP4DemuxerTrack createFromFiles(List<File> files) throws IOException {
        List<Fragment> fragments = new ArrayList<Fragment>();

        MovieBox moov = null;
        for (File file : files) {
            SeekableByteChannel channel = NIOUtils.readableChannel(file);
            for (Atom atom : MP4Util.getRootAtoms(channel)) {
                if ("moov".equals(atom.getHeader().getFourcc())) {
                    moov = (MovieBox) atom.parseBox(channel);
                } else if ("moof".equalsIgnoreCase(atom.getHeader().getFourcc())) {
                    MovieFragmentBox mfra = (MovieFragmentBox) atom.parseBox(channel);
                    TrackFragmentBox tfra = mfra.getTracks()[0];
                    fragments.add(new Fragment(tfra, atom.getOffset(), channel));
                }
            }

        }
        return new DashMP4DemuxerTrack(moov, moov.getTracks()[0], fragments.toArray(new Fragment[0]));
    }

    @Override
    public synchronized MP4Packet nextFrame() throws IOException {
        if (curFrag >= sizes.length)
            return null;
        if (curFrame >= sizes[curFrag].length) {
            curFrag++;
            curFrame = 0;
            offInChunk = 0;
        }
        if (curFrag >= sizes.length)
            return null;
        int size = sizes[curFrag][(int) curFrame];

        return getNextFrame(ByteBuffer.allocate(size));
    }

    protected ByteBuffer readPacketData(SeekableByteChannel input, ByteBuffer buffer, long offset, int size)
            throws IOException {
        ByteBuffer result = buffer.duplicate();
        synchronized (input) {
            input.setPosition(offset);
            NIOUtils.readL(input, result, size);
        }
        result.flip();
        return result;
    }

    public synchronized MP4Packet getNextFrame(ByteBuffer storage) throws IOException {
        if (curFrag >= sizes.length)
            return null;
        if (curFrame >= sizes[curFrag].length)
            return null;
        int size = sizes[curFrag][(int) curFrame];

        if (storage != null && storage.remaining() < size) {
            throw new IllegalArgumentException("Buffer size is not enough to fit a packet");
        }

        long pktPos = chunkOffsets[curFrag] + offInChunk;

        ByteBuffer result = readPacketData(inputs[curFrag], storage, pktPos, size);

        if (result != null && result.remaining() < size)
            return null;

        int duration = sampleDurations[curFrag] == null
                ? (avgDur[curFrag] == 0 ? (int) (durationHint * trak.getTimescale()) : avgDur[curFrag])
                : sampleDurations[curFrag][curFrame];

        boolean sync = curFrame == 0;

        long realPts = pts;
        if (compOffsets[curFrag] != null) {
            realPts = pts + compOffsets[curFrag][curFrame];
        }

        FrameType ftype = sync ? FrameType.KEY : FrameType.INTER;
        MP4Packet pkt = new MP4Packet(result, realPts, trak.getTimescale(), duration, globalFrame, ftype, null, 0,
                realPts, 1, pktPos, size, false);

        offInChunk += size;

        pts += duration;
        curFrame++;
        globalFrame++;

        return pkt;
    }

    @Override
    public DemuxerTrackMeta getMeta() {
        MP4TrackType type = TrakBox.getTrackType(trak);
        TrackType t = type == MP4TrackType.VIDEO ? VIDEO : (type == MP4TrackType.SOUND ? AUDIO : OTHER);
        int[] seekFrames = new int[sizes.length];
        for (int i = 0, numFrames = 0; i < sizes.length; i++) {
            seekFrames[i] = numFrames;
            numFrames += sizes[i].length;
        }

        ByteBuffer codecPrivate = getCodecPrivate();
        VideoCodecMeta videoCodecMeta = getVideoCodecMeta();
        AudioCodecMeta audioCodecMeta = getAudioCodecMeta();
        return new MP4DemuxerTrackMeta(t, Codec.codecByFourcc(sampleEntries[0].getFourcc()), totalDuration, seekFrames,
                frameCount, codecPrivate, videoCodecMeta, audioCodecMeta, sampleEntries);
    }

    protected ColorSpace getColorInfo() {
        Codec codec = Codec.codecByFourcc(trak.getFourcc());
        if (codec == Codec.H264) {
            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) sampleEntries[0]);
            List<ByteBuffer> spsList = avcC.getSpsList();
            if (spsList.size() > 0) {
                SeqParameterSet sps = SeqParameterSet.read(spsList.get(0).duplicate());
                return sps.getChromaFormatIdc();
            }
        }
        return null;
    }

    private AudioCodecMeta getAudioCodecMeta() {
        MP4TrackType type = TrakBox.getTrackType(trak);
        AudioCodecMeta audioCodecMeta = null;
        if (type == MP4TrackType.SOUND) {
            AudioSampleEntry ase = (AudioSampleEntry) sampleEntries[0];
            audioCodecMeta = AudioCodecMeta.fromAudioFormat(ase.getFormat());
        }
        return audioCodecMeta;
    }

    private VideoCodecMeta getVideoCodecMeta() {
        MP4TrackType type = TrakBox.getTrackType(trak);
        VideoCodecMeta videoCodecMeta = null;
        if (type == MP4TrackType.VIDEO) {
            videoCodecMeta = createSimpleVideoCodecMeta(trak.getCodedSize(), getColorInfo());
            PixelAspectExt pasp = NodeBox.findFirst(sampleEntries[0], PixelAspectExt.class, "pasp");
            if (pasp != null)
                videoCodecMeta.setPixelAspectRatio(pasp.getRational());
        }
        return videoCodecMeta;
    }

    public ByteBuffer getCodecPrivate() {
        Codec codec = Codec.codecByFourcc(sampleEntries[0].getFourcc());
        if (codec == Codec.H264) {
            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) sampleEntries[0]);
            return H264Utils.avcCToAnnexB(avcC);

        } else if (codec == Codec.AAC) {
            return AACUtils.getCodecPrivate(sampleEntries[0]);
        }
        // This codec does not have private section
        return null;
    }

    @Override
    public boolean gotoFrame(long frameNo) throws IOException {
        int curFrag = 0;
        int globalFrame = 0;
        int pts = 0;
        for (int[] is : sizes) {
            if (frameNo > is.length) {
                frameNo -= is.length;
                pts += sampleDurations[curFrag] == null ? avgDur[curFrag] * is.length
                        : ArrayUtil.sumInt(sampleDurations[curFrag]);
                curFrag++;
                globalFrame += is.length;
            } else {
                pts += sampleDurations[curFrag] == null ? avgDur[curFrag] * frameNo
                        : ArrayUtil.sumInt3(sampleDurations[curFrag], 0, (int) frameNo);
                this.curFrag = curFrag;
                this.curFrame = (int) frameNo;
                this.globalFrame = globalFrame + frameNo;
                this.pts = pts;
                adjustOff();

                return true;
            }
        }
        return false;
    }

    private void adjustOff() {
        this.offInChunk = 0;
        for (int i = 0; i < curFrame; i++) {
            this.offInChunk += sizes[curFrag][i];
        }
    }

    @Override
    public long getCurFrame() {
        return globalFrame;
    }

    @Override
    public void seek(double second) throws IOException {
        int curFrag = 0;
        int globalFrame = 0;
        int pts = 0;
        for (int[] is : sizes) {
            int fragDur = sampleDurations[curFrag] != null ? ArrayUtil.sumInt(sampleDurations[curFrag])
                    : avgDur[curFrag] * is.length;
            if (second > fragDur) {
                second -= fragDur;
                pts += fragDur;
                curFrag++;
                globalFrame += is.length;
            } else {
                this.curFrag = curFrag;
                if (sampleDurations[curFrag] != null) {
                    for (curFrame = 0; curFrame < sampleDurations[curFrag].length; curFrame++) {
                        if (second < sampleDurations[curFrag][curFrame])
                            break;
                        second -= sampleDurations[curFrag][curFrame];
                    }
                } else {
                    this.curFrame = (int) (second / avgDur[this.curFrag]);
                    adjustOff();
                }
                pts += sampleDurations[curFrag] == null ? avgDur[curFrag] * this.curFrame
                        : ArrayUtil.sumInt3(sampleDurations[curFrag], 0, this.curFrame);
                this.globalFrame = globalFrame + this.curFrame;
            }
        }
    }

    @Override
    public boolean gotoSyncFrame(long frameNo) throws IOException {
        int curFrag = 0;
        int globalFrame = 0;
        int pts = 0;
        for (int[] is : sizes) {
            if (frameNo > is.length) {
                frameNo -= is.length;
                pts += sampleDurations[curFrag] == null ? avgDur[curFrag] * is.length
                        : ArrayUtil.sumInt(sampleDurations[curFrag]);
                curFrag++;
                globalFrame += is.length;
            } else {
                this.curFrag = curFrag;
                this.curFrame = 0;
                this.globalFrame = globalFrame;
                this.pts = pts;
                offInChunk = 0;
                return true;
            }
        }
        return false;
    }

    public MP4TrackType getTrackType() {
        return TrakBox.getTrackType(trak);
    }

    @Override
    public void close() throws IOException {
        IOException ex = null;
        for (SeekableByteChannel channel : inputs) {
            try {
                channel.close();
            } catch (IOException e) {
                ex = e;
            }
        }
        if (ex != null)
            throw ex;
    }

    public int getNo() {
        return trak.getTrackHeader().getTrackId();
    }

    public void setDurationHint(double arg) {
        this.durationHint = arg;
    }
}