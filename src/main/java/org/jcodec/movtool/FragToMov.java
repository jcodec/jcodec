package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.ArrayUtil;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrackFragmentBaseMediaDecodeTimeBox;
import org.jcodec.containers.mp4.boxes.TrackFragmentBox;
import org.jcodec.containers.mp4.boxes.TrackFragmentHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.TrunBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Converts a bunch of DASH fragments into a movie.
 * 
 * @author The JCodec project
 * 
 */
public class FragToMov {
    private static final Flag FLAG_INIT = Flag.flag("init", "i", "A file that contains global sequence headers");
    private static final Flag FLAG_OUT = Flag.flag("out", "o", "Output file");
    private static final Flag[] flags = { FLAG_INIT, FLAG_OUT };

    public static void main(String[] args) throws IOException {
        final Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelpCmd("frag2mov", flags, Arrays.asList(new String[] { "...out movie" }));
            System.exit(-1);
            return;
        }

        FragToMov fragToMov = new FragToMov();
        if (cmd.getStringFlag(FLAG_INIT) != null) {
            fragToMov.setInit(new File(cmd.getStringFlag(FLAG_INIT)));
        }

        for (int i = 0; i < cmd.argsLength(); i++) {
            fragToMov.addFragment(new File(cmd.getArg(i)));
        }

        fragToMov.toMovie(new File(cmd.getStringFlagD(FLAG_OUT, "out.mp4")));
    }

    private File init;
    private List<File> fragments = new LinkedList<File>();

    private void addFragment(File file) {
        this.fragments.add(file);
    }

    private void setInit(File file) {
        init = file;
    }

    void toMovie(File outFile) throws IOException {
        MovieBox initMov = null;
        if (init != null)
            initMov = MP4Util.parseMovie(init);
        List<Fragment> list = new LinkedList<Fragment>();
        FileChannelWrapper out = null;
        try {
            out = NIOUtils.writableChannel(outFile);
            out.write(MP4Util.writeBox(Brand.MP4.getFileTypeBox(), 32));
            long mdatPos = out.position();
            MP4Util.mdatPlaceholder(out);

            for (File file : fragments) {
                FileChannelWrapper in = null;
                try {
                    in = NIOUtils.readableChannel(file);
                    Fragment frag = createFragment();

                    for (Atom atom : MP4Util.getRootAtoms(in)) {
                        if ("moov".equals(atom.getHeader().getFourcc()) && initMov == null) {
                            initMov = (MovieBox) atom.parseBox(in);
                        } else if ("moof".equalsIgnoreCase(atom.getHeader().getFourcc())) {
                            frag.addBox((MovieFragmentBox) atom.parseBox(in), atom.getOffset());
                        } else if ("mdat".equalsIgnoreCase(atom.getHeader().getFourcc())) {
                            frag.addOffset(out.position(), atom.getOffset() + atom.getHeader().headerSize(),
                                    atom.getHeader().getSize());
                            atom.copyContents(in, out);
                        }
                    }
                    list.add(frag);
                } finally {
                    NIOUtils.closeQuietly(in);
                }
            }
            long mdatSize = out.position() - mdatPos - 16;
            MP4Util.writeMovie(out, getMoov(initMov, list));
            MP4Util.writeMdat(out, mdatPos, mdatSize);
        } finally {
            NIOUtils.closeQuietly(out);
        }

    }

    private static Fragment createFragment() {
        return new Fragment();
    }

    private static class Offset {
        long srcPos;
        long len;
        long dstPos;

        public Offset(long dstPos, long srcPos, long len) {
            this.dstPos = dstPos;
            this.srcPos = srcPos;
            this.len = len;
        }
    }

    private static class BoxOffset {
        MovieFragmentBox box;
        long offset;

        public BoxOffset(MovieFragmentBox box, long offset) {
            this.box = box;
            this.offset = offset;
        }
    }

    private static class Fragment {
        List<BoxOffset> boxes;
        List<Offset> offsets;

        public Fragment() {
            this.boxes = new LinkedList<BoxOffset>();
            this.offsets = new LinkedList<Offset>();
        }

        public void addBox(MovieFragmentBox box, long offset) {
            this.boxes.add(new BoxOffset(box, offset));
        }

        public void addOffset(long dstPos, long srcPos, long len) {
            this.offsets.add(new Offset(dstPos, srcPos, len));
        }
    }

    public static MovieBox getMoov(MovieBox init, List<Fragment> fragments) {
        if (init == null)
            return null;

        MovieBox movieBox = MovieBox.createMovieBox();
        List<List<TrackFragmentBox>> tr = new ArrayList<List<TrackFragmentBox>>();
        // Applying offset to fragments
        for (Fragment frag : fragments) {
            for (BoxOffset bo : frag.boxes) {
                int no = 0;
                for (TrackFragmentBox traf : bo.box.getTracks()) {
                    long baseOff = traf.getTfhd().getBaseDataOffset() + bo.offset;
                    TrunBox trun = traf.getTrun();
                    for (Offset offset : frag.offsets) {
                        long dataOff = baseOff + trun.getDataOffset();
                        if (dataOff >= offset.srcPos && dataOff < offset.srcPos + offset.len) {
                            trun.setDataOffset(dataOff - offset.srcPos + offset.dstPos);
                            break;
                        }
                    }
                    if (tr.size() <= no)
                        tr.add(no, new ArrayList<TrackFragmentBox>());
                    tr.get(no++).add(traf);
                }
            }
        }
        movieBox.addFirst(init.getMovieHeader());
        int i = 0;
        for (List<TrackFragmentBox> list : tr) {
            TrakBox trak = createTrack(movieBox, init.getTracks()[i++], list);
            movieBox.add(trak);
            if (trak.getDuration() > movieBox.getDuration())
                movieBox.setDuration(trak.getDuration());
        }

        return movieBox;
    }

    private static TrakBox createTrack(MovieBox movie, TrakBox trakBox, List<TrackFragmentBox> list) {
        int defaultSampleSize = -1;
        int defaultSampleDuration = -1;
        List<int[]> sampleSizes = new LinkedList<int[]>();
        List<int[]> sampleDurations = new LinkedList<int[]>();
        long[] co = new long[list.size()];
        SampleToChunkEntry[] sampleToCh = new SampleToChunkEntry[list.size()];
        int nSamples = 0;
        TimeToSampleEntry[] avgSampleDur = new TimeToSampleEntry[list.size()];

        int i = 0;
        long prevDecodeTime = 0;
        for (TrackFragmentBox traf : list) {
            TrunBox trun = traf.getTrun();
            TrackFragmentBaseMediaDecodeTimeBox tfdt = traf.getTfdt();
            if (tfdt != null) {
                if (i > 0) {
                    avgSampleDur[i - 1] = new TimeToSampleEntry((int) trun.getSampleCount(),
                            (int) ((tfdt.getBaseMediaDecodeTime() - prevDecodeTime) / trun.getSampleCount()));
                }
                prevDecodeTime = tfdt.getBaseMediaDecodeTime();
            }
            co[i] = trun.getDataOffset();
            sampleToCh[i] = new SampleToChunkEntry(nSamples + 1, (int) trun.getSampleCount(), 1);
            nSamples += trun.getSampleCount();
            i++;
            TrackFragmentHeaderBox tfhd = traf.getTfhd();

            if (tfhd.isDefaultSampleSizeAvailable()) {
                int ss = tfhd.getDefaultSampleSize();
                if (defaultSampleSize == -1) {
                    defaultSampleSize = ss;
                } else if (ss != defaultSampleSize) {
                    throw new RuntimeException("Incompatible fragments, default sample sizes differ.");
                }

            } else {
                sampleSizes.add(trun.getSampleSizes());
            }
            if (tfhd.isDefaultSampleDurationAvailable()) {
                int ss = tfhd.getDefaultSampleDuration();
                if (defaultSampleDuration == -1)
                    defaultSampleDuration = tfhd.getDefaultSampleDuration();
                else if (ss != defaultSampleDuration) {
                    throw new RuntimeException("Incompatible fragments, default sample durations differ.");
                }
            } else {
                if (trun.isSampleDurationAvailable())
                    sampleDurations.add(trun.getSampleDurations());
            }
        }
        if (avgSampleDur.length > 1)
            avgSampleDur[avgSampleDur.length - 1] = avgSampleDur[avgSampleDur.length - 2];
        SampleSizesBox stsz = defaultSampleSize != -1 ? SampleSizesBox.createSampleSizesBox(defaultSampleSize, nSamples)
                : SampleSizesBox.createSampleSizesBox2(ArrayUtil.flatten2DL(sampleSizes));
        TimeToSampleEntry[] tts = getStts(defaultSampleDuration, nSamples, sampleDurations);
        if (tts == null)
            tts = avgSampleDur;
        TimeToSampleBox stts = TimeToSampleBox.createTimeToSampleBox(tts);
        setTrackDuration(movie, trakBox, tts);
        ChunkOffsets64Box co64 = ChunkOffsets64Box.createChunkOffsets64Box(co);
        SampleToChunkBox stsc = SampleToChunkBox.createSampleToChunkBox(sampleToCh);
        trakBox.getStbl().replaceBox(stsz);
        trakBox.getStbl().replaceBox(stts);
        trakBox.getStbl().replaceBox(co64);
        trakBox.getStbl().replaceBox(stsc);
        trakBox.getStbl().removeChildren(new String[] { "stco" });

        return trakBox;
    }

    private static void setTrackDuration(MovieBox movie, TrakBox trakBox, TimeToSampleEntry[] tts) {
        long totalDur = 0;
        for (TimeToSampleEntry tt : tts) {
            totalDur += tt.getSegmentDuration();
        }
        trakBox.setDuration(movie.rescale(totalDur, trakBox.getTimescale()));

    }

    private static TimeToSampleEntry[] getStts(int defaultSampleDuration, int nSamples, List<int[]> sampleDurations) {
        List<TimeToSampleEntry> tts = new LinkedList<TimeToSampleEntry>();
        if (defaultSampleDuration != -1) {
            tts.add(new TimeToSampleEntry(nSamples, defaultSampleDuration));
        } else if (sampleDurations.size() > 0) {
            for (int[] is : sampleDurations) {
                for (int i = 0; i < is.length; i++) {
                    tts.add(new TimeToSampleEntry(1, is[i]));
                }
            }
        }
        return null;
    }
}
