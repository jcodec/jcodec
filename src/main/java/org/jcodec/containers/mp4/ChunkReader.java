package org.jcodec.containers.mp4;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
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
 * @author The JCodec project
 *
 */
public class ChunkReader {
    private int curChunk;
    private int sampleNo;
    private int s2cIndex;
    private int ttsInd = 0;
    private int ttsSubInd = 0;
    private long chunkTv = 0;
    private long[] chunkOffsets;
    private SampleToChunkEntry[] sampleToChunk;
    private SampleSizesBox stsz;
    private TimeToSampleEntry[] tts;
    private SampleDescriptionBox stsd;
    private final SeekableByteChannel input;
    private final SampleEntry[] entries;

    public ChunkReader(TrakBox trakBox, SeekableByteChannel input) {
        TimeToSampleBox stts = trakBox.getStts();
        tts = stts.getEntries();
        ChunkOffsetsBox stco = trakBox.getStco();
        ChunkOffsets64Box co64 = trakBox.getCo64();
        stsz = trakBox.getStsz();
        SampleToChunkBox stsc = trakBox.getStsc();

        if (stco != null)
            chunkOffsets = stco.getChunkOffsets();
        else
            chunkOffsets = co64.getChunkOffsets();
        sampleToChunk = stsc.getSampleToChunk();
        stsd = trakBox.getStsd();
        entries = trakBox.getSampleEntries();
        this.input = input;
    }

    public boolean hasNext() {
        return curChunk < chunkOffsets.length;
    }

    public Chunk next() throws IOException {
        if (curChunk >= chunkOffsets.length)
            return null;

        if (s2cIndex + 1 < sampleToChunk.length && curChunk + 1 == sampleToChunk[s2cIndex + 1].getFirst())
            s2cIndex++;
        int sampleCount = sampleToChunk[s2cIndex].getCount();

        int[] samplesDur = null;
        int sampleDur = Chunk.UNEQUAL_DUR;
        if (ttsSubInd + sampleCount <= tts[ttsInd].getSampleCount()) {
            sampleDur = tts[ttsInd].getSampleDuration();
            ttsSubInd += sampleCount;
        } else {
            samplesDur = new int[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                if (ttsSubInd >= tts[ttsInd].getSampleCount() && ttsInd < tts.length - 1) {
                    ttsSubInd = 0;
                    ++ttsInd;
                }
                samplesDur[i] = tts[ttsInd].getSampleDuration();
                ++ttsSubInd;
            }
        }

        int size = Chunk.UNEQUAL_SIZES;
        int[] sizes = null;
        if (stsz.getDefaultSize() > 0) {
            size = stsz.getDefaultSize();
        } else {
            sizes = Platform.copyOfRangeI(stsz.getSizes(), sampleNo, sampleNo + sampleCount);
        }

        int eno = sampleToChunk[s2cIndex].getEntry();
        SampleEntry se = entries[eno - 1];
        if (se.getDrefInd() != 1)
            throw new IOException("Multiple sample entries not supported");

        Chunk chunk = new Chunk(chunkOffsets[curChunk], chunkTv, sampleCount, size, sizes, sampleDur, samplesDur, eno);

        chunkTv += chunk.getDuration();
        sampleNo += sampleCount;
        ++curChunk;

        if (input != null) {
        	input.setPosition(chunk.getOffset());
        	chunk.setData(NIOUtils.fetchFromChannel(input, (int) chunk.getSize()));
        }
        return chunk;
    }

    public int size() {
        return chunkOffsets.length;
    }

    public List<Chunk> readAll() throws IOException {
        if (input != null)
            throw new IllegalStateException("Reading all chunk data to memory is costy.");
        List<Chunk> result = new ArrayList<Chunk>();
        Chunk chunk = null;
        while ((chunk = next()) != null) {
            result.add(chunk);
        }
        return result;
    }
}
