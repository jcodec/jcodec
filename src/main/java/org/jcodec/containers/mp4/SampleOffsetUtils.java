package org.jcodec.containers.mp4;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox.SampleToChunkEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class SampleOffsetUtils {

    public static ByteBuffer getSampleData(int sample, File file) throws IOException {
        MovieBox moov = MP4Util.parseMovie(file);
        MediaInfoBox minf = moov.getAudioTracks().get(0).getMdia().getMinf();
        ChunkOffsetsBox stco = NodeBox.findFirst(minf, ChunkOffsetsBox.class, "stbl", "stco");
        SampleToChunkBox stsc = NodeBox.findFirst(minf, SampleToChunkBox.class, "stbl", "stsc");
        SampleSizesBox stsz = NodeBox.findFirst(minf, SampleSizesBox.class, "stbl", "stsz");
        long sampleOffset = getSampleOffset(sample, stsc, stco, stsz);
        MappedByteBuffer map = NIOUtils.map(file);
        map.position((int) sampleOffset);
        map.limit(map.position() + stsz.getSizes()[sample]);
        return map;
    }

    public static long getSampleOffset(int sample, SampleToChunkBox stsc, ChunkOffsetsBox stco, SampleSizesBox stsz) {
        int chunkBySample = getChunkBySample(sample, stco, stsc);
        int firstSampleAtChunk = getFirstSampleAtChunk(chunkBySample, stsc, stco);
        long offset = stco.getChunkOffsets()[chunkBySample - 1];
        int[] sizes = stsz.getSizes();
        for (int i = firstSampleAtChunk; i < sample; i++) {
            offset += sizes[i];
        }
        return offset;
    }

    public static int getFirstSampleAtChunk(int chunk, SampleToChunkBox stsc, ChunkOffsetsBox stco) {
        int chunks = stco.getChunkOffsets().length;
        int samples = 0;
        for (int i = 1; i <= chunks; i++) {
            if (i == chunk) {
                break;
            }
            int samplesInChunk = getSamplesInChunk(i, stsc);
            samples += samplesInChunk;
        }
        return samples;
    }

    public static int getChunkBySample(int sampleOfInterest, ChunkOffsetsBox stco, SampleToChunkBox stsc) {
        int chunks = stco.getChunkOffsets().length;
        int startSample = 0;
        int endSample = 0;
        for (int i = 1; i <= chunks; i++) {
            int samplesInChunk = getSamplesInChunk(i, stsc);
            endSample = startSample + samplesInChunk;
            if (sampleOfInterest >= startSample && sampleOfInterest < endSample) {
                return i;
            }
            startSample = endSample;
        }
        return -1;
    }

    public static int getSamplesInChunk(int chunk, SampleToChunkBox stsc) {
        //TODO this is faster with binary search 
        SampleToChunkEntry[] sampleToChunk = stsc.getSampleToChunk();
        int sampleCount = 0;
        for (SampleToChunkEntry sampleToChunkEntry : sampleToChunk) {
            if (sampleToChunkEntry.getFirst() > chunk) {
                return sampleCount;
            }
            sampleCount = sampleToChunkEntry.getCount();
        }
        return sampleCount;
    }
}
