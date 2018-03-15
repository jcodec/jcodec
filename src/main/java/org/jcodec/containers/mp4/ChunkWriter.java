package org.jcodec.containers.mp4;
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
import org.jcodec.containers.mp4.boxes.TrakBox;

import java.io.IOException;

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
    private SeekableByteChannel[] inputs;
    private int curChunk;
    private SeekableByteChannel out;
    byte[] buf;
    private TrakBox trak;

    public ChunkWriter(TrakBox trak, SeekableByteChannel[] inputs, SeekableByteChannel out) {
        this.buf = new byte[8092];
        entries = trak.getSampleEntries();
        ChunkOffsetsBox stco = trak.getStco();
        ChunkOffsets64Box co64 = trak.getCo64();
        int size;
        if (stco != null)
            size = stco.getChunkOffsets().length;
        else
            size = co64.getChunkOffsets().length;
        this.inputs = inputs;

        offsets = new long[size];
        this.out = out;
        this.trak = trak;
    }

    public void apply() {
        NodeBox stbl = NodeBox.findFirstPath(trak, NodeBox.class, Box.path("mdia.minf.stbl"));
        stbl.removeChildren(new String[]{"stco", "co64"});

        stbl.add(ChunkOffsets64Box.createChunkOffsets64Box(offsets));
        cleanDrefs(trak);
    }

    private void cleanDrefs(TrakBox trak) {
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

    private SeekableByteChannel getInput(Chunk chunk) {
        SampleEntry se = entries[chunk.getEntry() - 1];
        return inputs[se.getDrefInd() - 1];
    }

    public void write(Chunk chunk) throws IOException {
        SeekableByteChannel input = getInput(chunk);
        input.setPosition(chunk.getOffset());
        long pos = out.position();

        out.write(NIOUtils.fetchFromChannel(input, (int) chunk.getSize()));
        offsets[curChunk++] = pos;
    }
}