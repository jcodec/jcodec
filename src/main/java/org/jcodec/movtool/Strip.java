package org.jcodec.movtool;

import static java.lang.System.arraycopy;
import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jcodec.common.Tuple._2;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.containers.mp4.Chunk;
import org.jcodec.containers.mp4.ChunkReader;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MediaHeaderBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Strips movie to editlist
 * 
 * @author The JCodec project
 * 
 */
public class Strip {
    public static void main1(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: strip <ref movie> <out movie>");
            System.exit(-1);
        }
        SeekableByteChannel input = null;
        SeekableByteChannel out = null;
        try {
            input = readableChannel(new File(args[0]));
            File file = new File(args[1]);
            Platform.deleteFile(file);
            out = writableChannel(file);
            Movie movie = MP4Util.createRefFullMovie(input, "file://" + new File(args[0]).getAbsolutePath());
            new Strip().stripToChunks(movie.getMoov());
            MP4Util.writeFullMovie(out, movie);
        } finally {
            if (input != null)
                input.close();
            if (out != null)
                out.close();
        }
    }

    public void strip(MovieBox movie) throws IOException {
        stripToChunks(movie);
        stripToSamples(movie, false);
    }

    public void stripToChunks(MovieBox movie) throws IOException {
        RationalLarge maxDuration = RationalLarge.ZERO;
        TrakBox[] tracks = movie.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrakBox track = tracks[i];
            RationalLarge duration = stripTrack(movie, track);
            if (duration.greaterThen(maxDuration))
                maxDuration = duration;
        }
        movie.setDuration(movie.rescale(maxDuration.getNum(), maxDuration.getDen()));
    }

    public void stripToSamples(MovieBox movie, boolean toWholeSampleMeta) throws IOException {
        RationalLarge maxDuration = RationalLarge.ZERO;
        TrakBox[] tracks = movie.getTracks();
        for (int i = 0; i < tracks.length; i++) {
            TrakBox track = tracks[i];
            RationalLarge duration = trimEdits(movie, track,
                    !"meta".equals(track.getHandlerType()) || toWholeSampleMeta);

            if (duration.greaterThen(maxDuration))
                maxDuration = duration;
        }
        movie.setDuration(movie.rescale(maxDuration.getNum(), maxDuration.getDen()));
    }

    public RationalLarge stripTrack(MovieBox movie, TrakBox track) throws IOException {
        ChunkReader chunks = new ChunkReader(track, null);
        List<Edit> edits = track.getEdits();
        List<Edit> oldEdits = deepCopy(edits);
        List<Chunk> result = new ArrayList<Chunk>();

        Chunk chunk;
        while ((chunk = chunks.next()) != null) {
            boolean intersects = false;
            for (Edit edit : oldEdits) {
                if (edit.getMediaTime() == -1)
                    continue; // track offset, not real edit
                long editS = edit.getMediaTime();
                long editE = edit.getMediaTime() + track.rescale(edit.getDuration(), movie.getTimescale());
                long chunkS = chunk.getStartTv();
                long chunkE = chunk.getStartTv() + chunk.getDuration();

                intersects = intersects(editS, editE, chunkS, chunkE);
                if (intersects)
                    break;
            }
            if (!intersects) {
                for (int i = 0; i < oldEdits.size(); i++) {
                    if (oldEdits.get(i).getMediaTime() >= chunk.getStartTv() + chunk.getDuration())
                        edits.get(i).shift(-chunk.getDuration());
                }
            } else {
                result.add(chunk);
            }
        }
        NodeBox stbl = NodeBox.findFirstPath(track, NodeBox.class, Box.path("mdia.minf.stbl"));
        stbl.replace("stts", getTimeToSamples(result));
        stbl.replace("stsz", getSampleSizes(result));
        stbl.replace("stsc", getSamplesToChunk(result));
        stbl.removeChildren(new String[] { "stco", "co64" });
        stbl.add(getChunkOffsets(result));
        long duration = totalDuration(result);
        MediaHeaderBox mdhd = NodeBox.findFirstPath(track, MediaHeaderBox.class, Box.path("mdia.mdhd"));
        mdhd.setDuration(duration);
        track.setDuration(movie.rescale(duration, mdhd.getTimescale()));
        return new RationalLarge(duration, mdhd.getTimescale());
    }
    
    static List<_2<Long, Long>> findGaps(RationalLarge rescale, List<Edit> edits, _2<Long, Long> timeline) {
        List<_2<Long, Long>> intervals = new ArrayList<_2<Long, Long>>();
        intervals.add(timeline);
        for (Edit edit : edits) {
            if (edit.getMediaTime() == -1) {
                // Timeline shift
                continue;
            }
            List<_2<Long, Long>> newGaps = new ArrayList<_2<Long, Long>>();
            long editEnd = edit.getMediaTime() + rescale.multiplyS(edit.getDuration());
            for (_2<Long, Long> gap : intervals) {
                if (gap.v0 <= edit.getMediaTime() && gap.v1 > edit.getMediaTime()) {
                    _2<Long, Long> gap0 = new _2<Long, Long>(gap.v0, edit.getMediaTime() - 1);
                    _2<Long, Long> gap1 = new _2<Long, Long>(edit.getMediaTime(), gap.v1);
                    newGaps.add(gap0);
                    gap = gap1;
                }
                if (gap.v0 <= editEnd && gap.v1 > editEnd) {
                    _2<Long, Long> gap0 = new _2<Long, Long>(gap.v0, editEnd - 1);
                    _2<Long, Long> gap1 = new _2<Long, Long>(editEnd, gap.v1);
                    newGaps.add(gap0);
                    newGaps.add(gap1);
                } else {
                    newGaps.add(gap);
                }
            }
            intervals = newGaps;
        }
        List<_2<Long, Long>> unclaimed = new ArrayList<_2<Long, Long>>();
        for (_2<Long, Long> gap : intervals) {
            boolean claimed = false;
            for (Edit edit : edits) {
                if (edit.getMediaTime() == -1) {
                    // Timeline shift
                    continue;
                }
                long editEnd = edit.getMediaTime() + rescale.multiplyS(edit.getDuration()) - 1;
                if (gap.v0 == edit.getMediaTime() || gap.v1 == editEnd) {
                    claimed = true;
                    break;
                }
            }
            if (!claimed)
                unclaimed.add(gap);
        }
        return unclaimed;
    }

    static List<Chunk> cutChunksToGaps(List<Chunk> chunks, List<_2<Long, Long>> gaps, List<_2<Long, Long>> newIntervals) {
        List<_2<Long, Long>> newGaps = new ArrayList<_2<Long, Long>>();
        List<Chunk> result = new ArrayList<Chunk>();
        long pullBack = 0;
        // Pass 0, splitting the chunks by gap points, dropping middle chunk for each gap
        Iterator<Chunk> it = chunks.iterator();
        while (it.hasNext()) {
            Chunk chunk = it.next();
            for (_2<Long, Long> gap : gaps) {
                if (gap.v0 > chunk.getStartTv() && gap.v0 < chunk.getStartTv() + chunk.getDuration()) {
                    // break it down
                    _2<Chunk, Chunk> split = chunk.split(gap.v0 - chunk.getStartTv(), true);
                    Chunk left = split.v0;
                    result.add(left);
                    long wantDur = gap.v0 - chunk.getStartTv();
                    if (newIntervals != null) {
                        newGaps.add(new _2<Long, Long>(gap.v0 - pullBack, left.getDuration() - wantDur));
                    } else {
                        left.trimLastSample(left.getDuration() - wantDur);
                    }
                    chunk = split.v1;
                    while (chunk != null && gap.v1 >= chunk.getStartTv() + chunk.getDuration()) {
                        pullBack += chunk.getDuration();
                        chunk = it.hasNext() ? it.next() : null;
                    }
                    if (chunk != null && chunk.getSampleCount() == 0) {
                        chunk = null;
                    }
                }
                if (chunk == null)
                    break;
                if (gap.v1 >= chunk.getStartTv() && gap.v1 < chunk.getStartTv() + chunk.getDuration()) {
                    // break it down
                    _2<Chunk, Chunk> split = chunk.split(gap.v1 - chunk.getStartTv() + 1, false);
                    long wantStart = gap.v1 + 1;
                    long realStart = split.v0.getDuration() + chunk.getStartTv();
                    pullBack += split.v0.getDuration();
                    chunk = split.v1;
                    if (chunk != null && chunk.getSampleCount() == 0) {
                        chunk = null;
                    }
                    long trimDur = wantStart - realStart;
                    if (trimDur != 0) {
                        if (newIntervals != null) {
                            newGaps.add(new _2<Long, Long>(realStart - pullBack, trimDur));
                        } else {
                            split.v1.trimFirstSample(trimDur);
                        }
                    }
                }
                if (chunk == null)
                    break;
            }
            if (chunk != null)
                result.add(chunk);
        }
        // Pass 1, translating chunk start tv to the new timeline
        long startTv = 0;
        for (Chunk chunk : result) {
            chunk.setStartTv(startTv);
            startTv += chunk.getDuration();
        }
        // Pass 2, inverting the gaps to make the intervals
        if (newIntervals != null) {
            long totalDur = startTv;
            startTv = result.isEmpty() ? 0 : result.get(0).getStartTv();
            for (_2<Long,Long> gap : newGaps) {
                if (startTv < gap.v0)
                    newIntervals.add(new _2<Long,Long>(startTv, gap.v0 - startTv));
                startTv = gap.v1 + gap.v0;
            }
            if (startTv < totalDur) {
                newIntervals.add(new _2<Long,Long>(startTv, totalDur - startTv));
            }
        }

        return result;
    }

    public RationalLarge trimEdits(MovieBox movie, TrakBox track, boolean toWholeSample) throws IOException {
        ChunkReader chunkReader = new ChunkReader(track, null);
        List<Edit> edits = track.getEdits();

        List<Chunk> chunks = chunkReader.readAll();
        _2<Long, Long> timeline;
        if (!chunks.isEmpty()) {
            Chunk firstChunk = chunks.get(0);
            Chunk lastChunk = chunks.get(chunks.size() - 1);
            timeline = new _2<Long, Long>(firstChunk.getStartTv(), lastChunk.getStartTv() + lastChunk.getDuration());
        } else {
            timeline = new _2<Long, Long>(0L, 0L);
        }
        List<_2<Long, Long>> gaps = findGaps(RationalLarge.R(track.getTimescale(), movie.getTimescale()), edits, timeline);
        List<_2<Long, Long>> newIntervals = new ArrayList<_2<Long, Long>>();
        List<Chunk> result = cutChunksToGaps(chunks, gaps, toWholeSample ? newIntervals : null);
        List<Edit> newEdits = new ArrayList<Edit>();
        if (toWholeSample) {
            for (_2<Long, Long> edit : newIntervals) {
                newEdits.add(new Edit(movie.rescale(edit.v1, track.getTimescale()), edit.v0, 1f));
            }
        } else if (result.size() > 0) {
            Chunk firstChunk = result.get(0);
            Chunk lastChunk = result.get(result.size() - 1);
            long start = firstChunk.getStartTv();
            long duration = lastChunk.getStartTv() + lastChunk.getDuration();
            newEdits.add(new Edit(movie.rescale(duration, track.getTimescale()), start, 1f));
        }
        if (!edits.isEmpty() ) {
            Edit firstEdit = edits.get(0);
            if (firstEdit.getMediaTime() == -1) {
               newEdits.add(0, firstEdit);
            }
        }
        track.setEdits(newEdits);
        NodeBox stbl = NodeBox.findFirstPath(track, NodeBox.class, Box.path("mdia.minf.stbl"));
        stbl.replace("stts", getTimeToSamples(result));
        stbl.replace("stsz", getSampleSizes(result));
        stbl.replace("stsc", getSamplesToChunk(result));
        stbl.removeChildren(new String[] { "stco", "co64" });
        stbl.add(getChunkOffsets(result));
        long duration = totalDuration(result);
        MediaHeaderBox mdhd = NodeBox.findFirstPath(track, MediaHeaderBox.class, Box.path("mdia.mdhd"));
        mdhd.setDuration(duration);
        track.setDuration(movie.rescale(duration, mdhd.getTimescale()));
        return new RationalLarge(duration, mdhd.getTimescale());
    }

    private long totalDuration(List<Chunk> result) {
        long duration = 0;
        for (Chunk chunk : result) {
            duration += chunk.getDuration();
        }
        return duration;
    }

    private List<Edit> deepCopy(List<Edit> edits) {
        List<Edit> newList = new ArrayList<Edit>();
        if (edits != null) {
            for (Edit edit : edits) {
                newList.add(Edit.createEdit(edit));
            }
        }
        return newList;
    }

    public Box getChunkOffsets(List<Chunk> chunks) {
        long[] result = new long[chunks.size()];
        boolean longBox = false;
        int i = 0;
        for (Chunk chunk : chunks) {
            if (chunk.getOffset() >= 0x100000000L)
                longBox = true;
            result[i++] = chunk.getOffset();
        }
        return longBox ? ChunkOffsets64Box.createChunkOffsets64Box(result)
                : ChunkOffsetsBox.createChunkOffsetsBox(result);
    }

    public TimeToSampleBox getTimeToSamples(List<Chunk> chunks) {
        ArrayList<TimeToSampleEntry> tts = new ArrayList<TimeToSampleEntry>();
        int curTts = -1, cnt = 0;
        for (Chunk chunk : chunks) {
            if (chunk.getSampleDur() != Chunk.UNEQUAL_DUR) {
                if (curTts == -1 || curTts != chunk.getSampleDur()) {
                    if (curTts != -1)
                        tts.add(new TimeToSampleEntry(cnt, curTts));
                    cnt = 0;
                    curTts = chunk.getSampleDur();
                }
                cnt += chunk.getSampleCount();
            } else {
                for (int dur : chunk.getSampleDurs()) {
                    if (curTts == -1 || curTts != dur) {
                        if (curTts != -1)
                            tts.add(new TimeToSampleEntry(cnt, curTts));
                        cnt = 0;
                        curTts = dur;
                    }
                    ++cnt;
                }
            }
        }
        if (cnt > 0)
            tts.add(new TimeToSampleEntry(cnt, curTts));
        return TimeToSampleBox.createTimeToSampleBox(tts.toArray(new TimeToSampleEntry[0]));
    }

    public SampleSizesBox getSampleSizes(List<Chunk> chunks) {
        int nSamples = 0;
        int prevSize = chunks.size() != 0 ? chunks.get(0).getSampleSize() : 0;
        for (Chunk chunk : chunks) {
            nSamples += chunk.getSampleCount();
            if (prevSize == 0 && chunk.getSampleSize() != 0)
                throw new RuntimeException("Mixed sample sizes not supported");
        }

        if (prevSize > 0)
            return SampleSizesBox.createSampleSizesBox(prevSize, nSamples);

        int[] sizes = new int[nSamples];
        int startSample = 0;
        for (Chunk chunk : chunks) {
            arraycopy(chunk.getSampleSizes(), 0, sizes, startSample, chunk.getSampleCount());
            startSample += chunk.getSampleCount();
        }
        return SampleSizesBox.createSampleSizesBox2(sizes);
    }

    public SampleToChunkBox getSamplesToChunk(List<Chunk> chunks) {
        ArrayList<SampleToChunkEntry> result = new ArrayList<SampleToChunkEntry>();
        Iterator<Chunk> it = chunks.iterator();
        if (it.hasNext()) {
            Chunk chunk = it.next();
            int curSz = chunk.getSampleCount();
            int curEntry = chunk.getEntry();
            int first = 1, cnt = 1;
            while (it.hasNext()) {
                chunk = it.next();
                int newSz = chunk.getSampleCount();
                int newEntry = chunk.getEntry();
                if (curSz != newSz || curEntry != newEntry) {
                    result.add(new SampleToChunkEntry(first, curSz, curEntry));
                    curSz = newSz;
                    curEntry = newEntry;
                    first += cnt;
                    cnt = 0;
                }
                ++cnt;
            }
            result.add(new SampleToChunkEntry(first, curSz, curEntry));
        }

        return SampleToChunkBox.createSampleToChunkBox(result.toArray(new SampleToChunkEntry[0]));
    }

    /**
     * Checks if two intervals intersect
     * @param as Interval a start
     * @param ae Interval a end (exclusive)
     * @param bs Interval b start
     * @param be Interval b end (exclusive)
     * @return True if intersect
     */
    private static boolean intersects(long as, long ae, long bs, long be) {
        be -= 1;
        ae -= 1;
        return (as >= bs && as <= be) || (ae >= bs && ae <= be) || (bs >= as && bs <= ae) || (be >= as && be <= ae);
    }
    /** Temporary for backward comatibility */
    public void trim(MovieBox movie, String param) {
    }
}
