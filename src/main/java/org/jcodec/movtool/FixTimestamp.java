package org.jcodec.movtool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.Chunk;
import org.jcodec.containers.mp4.ChunkReader;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A hacky way to fix timestamps without rewriting the file
 * 
 * @author The JCodec project
 * 
 */
public abstract class FixTimestamp {
    public void fixTimestamp(TrakBox trakBox, SeekableByteChannel ch) throws IOException {
        SeekableByteChannel[] inputs = new SeekableByteChannel[] { ch };
        ChunkReader chr = new ChunkReader(trakBox, inputs);
        long prevPts = -1;
        long oldPts = 0;
        int oldDur = -1;
        long editStart = 0;
        long totalDur = 0;
        IntArrayList durations = IntArrayList.createIntArrayList();
        while (chr.hasNext()) {
            Chunk next = chr.next();
            ByteBuffer data = next.getData().duplicate();
            int[] sampleSizes = next.getSampleSizes();
            int[] sampleDurs = next.getSampleDurs();
            for (int i = 0; i < sampleSizes.length; i++) {
                int sz = sampleSizes[i];
                oldDur = sampleDurs != null ? sampleDurs[i] : next.getSampleDur();
                ByteBuffer sampleData = NIOUtils.read(data, sz);
                long pts = (long) (getPts(sampleData, oldPts, trakBox) * trakBox.getTimescale());
                totalDur = pts;
                System.out.println("old: " + oldPts + ", new: " + pts);
                oldPts += oldDur;
                if (prevPts != -1 && pts >= prevPts) {
                    long dur = pts - prevPts;
                    durations.add((int) dur);
                    prevPts = pts;
                } else if (prevPts == -1) {
                    prevPts = pts;
                    editStart = pts;
                }
            }
        }
        if (oldDur != -1) {
            durations.add(oldDur);
            totalDur += oldDur;
        }
        trakBox.getStbl().replaceBox(createStts(durations));
        if (editStart != 0) {
            ArrayList<Edit> edits = new ArrayList<Edit>();
            edits.add(new Edit(-editStart, totalDur - editStart, 1f));
            trakBox.setEdits(edits);
        }
    }

    private Box createStts(IntArrayList durations) {
        TimeToSampleEntry[] entries = new TimeToSampleEntry[durations.size()];
        for (int i = 0; i < durations.size(); i++)
            entries[i] = new TimeToSampleEntry(1, durations.get(i));
        return TimeToSampleBox.createTimeToSampleBox(entries);
    }

    protected abstract double getPts(ByteBuffer sampleData, double orig, TrakBox trakBox) throws IOException;
}
