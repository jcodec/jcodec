package org.jcodec.containers.mp4;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.boxes.AliasBox;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.DataInfoBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ChunkWriter {
    private long[] offsets;
    private SampleEntry[] entries;
    private final SeekableByteChannel input;
    private int curChunk;
    private SeekableByteChannel out;
    byte[] buf;
    private TrakBox trak;
    private IntArrayList sampleSizes;
    private int sampleSize;
    private int sampleCount;

    public ChunkWriter(TrakBox trak, SeekableByteChannel input, SeekableByteChannel out) {
        this.buf = new byte[8092];
        entries = trak.getSampleEntries();
        ChunkOffsetsBox stco = trak.getStco();
        ChunkOffsets64Box co64 = trak.getCo64();
        int size;
        if (stco != null)
            size = stco.getChunkOffsets().length;
        else
            size = co64.getChunkOffsets().length;
        this.input = input;

        offsets = new long[size];
        this.out = out;
        this.trak = trak;
        this.sampleSizes = IntArrayList.createIntArrayList();
    }

    public void apply() {
        NodeBox stbl = NodeBox.findFirstPath(trak, NodeBox.class, Box.path("mdia.minf.stbl"));
        stbl.removeChildren(new String[]{"stco", "co64"});

        stbl.add(ChunkOffsets64Box.createChunkOffsets64Box(offsets));
        cleanDrefs(trak);

        SampleSizesBox stsz = sampleCount != 0 ? SampleSizesBox.createSampleSizesBox(sampleSize, sampleCount)
                : SampleSizesBox.createSampleSizesBox2(sampleSizes.toArray());
        stbl.replaceBox(stsz);
    }

    public static void cleanDrefs(TrakBox trak) {
        MediaInfoBox minf = trak.getMdia().getMinf();
        DataInfoBox dinf = trak.getMdia().getMinf().getDinf();
        if (dinf == null) {
            dinf = DataInfoBox.createDataInfoBox();
            minf.add(dinf);
        }

        DataRefBox dref = dinf.getDref();
        if (dref == null) {
            dref = DataRefBox.createDataRefBox();
            dinf.add(dref);
        }

        dref.getBoxes().clear();
        dref.add(AliasBox.createSelfRef());

        SampleEntry[] sampleEntries = trak.getSampleEntries();
        for (int i = 0; i < sampleEntries.length; i++) {
            SampleEntry entry = sampleEntries[i];
            entry.setDrefInd((short) 1);
        }
    }

    public void write(Chunk chunk) throws IOException {
        SampleEntry se = entries[chunk.getEntry() - 1];
        if (se.getDrefInd() != 1)
            throw new IOException("Multiple sample entries not supported");

        long pos = out.position();

        ByteBuffer chunkData = chunk.getData();
        if (chunkData == null) {
            input.setPosition(chunk.getOffset());
            chunkData = NIOUtils.fetchFromChannel(input, (int) chunk.getSize());
        }

        out.write(chunkData);
        offsets[curChunk++] = pos;

        if (chunk.getSampleSize() == Chunk.UNEQUAL_SIZES) {
            if (sampleCount != 0) {
                unpackSampleSizes();
            }
            sampleSizes.addAll(chunk.getSampleSizes());
        } else {
            if (sampleSizes.size() != 0) {
                for (int i = 0; i < chunk.getSampleCount(); i++) {
                    sampleSizes.add(chunk.getSampleSize());
                }
            } else {
                if (sampleCount == 0) {
                    sampleSize = chunk.getSampleSize();
                } else if (sampleSize != chunk.getSampleSize()) {
                    unpackSampleSizes();
                    for (int i = 0; i < chunk.getSampleCount(); i++) {
                        sampleSizes.add(chunk.getSampleSize());
                    }
                }
                sampleCount += chunk.getSampleCount();
            }
        }
    }

    private void unpackSampleSizes() {
        sampleSizes = new IntArrayList(128);
        sampleSizes.fill(0, sampleCount, sampleSize);
        sampleCount = 0;
    }

    public void close() throws IOException {
        for (SeekableByteChannel input : inputs) {
            input.close();
        }
    }
}
