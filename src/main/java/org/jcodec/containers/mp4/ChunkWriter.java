package org.jcodec.containers.mp4;

import static org.jcodec.containers.mp4.boxes.Box.not;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.jcodec.common.io.RandomAccessInputStream;
import org.jcodec.common.io.WindowInputStream;
import org.jcodec.containers.mp4.boxes.AliasBox;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.DataInfoBox;
import org.jcodec.containers.mp4.boxes.DataRefBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleEntry;
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
    private RandomAccessInputStream[] inputs;
    private int curChunk;
    private RandomAccessFile out;
    byte[] buf = new byte[8092];
    private TrakBox trak;

    public ChunkWriter(TrakBox trak, RandomAccessInputStream[] inputs, RandomAccessFile out) {
        entries = NodeBox.findAll(trak, SampleEntry.class, "mdia", "minf", "stbl", "stsd", null);
        ChunkOffsetsBox stco = NodeBox.findFirst(trak, ChunkOffsetsBox.class, "mdia", "minf", "stbl", "stco");
        ChunkOffsets64Box co64 = NodeBox.findFirst(trak, ChunkOffsets64Box.class, "mdia", "minf", "stbl", "co64");
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
        NodeBox stbl = NodeBox.findFirst(trak, NodeBox.class, "mdia", "minf", "stbl");
        stbl.filter(not("stco"));
        stbl.filter(not("co64"));

        stbl.add(new ChunkOffsets64Box(offsets));
        cleanDrefs(trak);
    }

    private void cleanDrefs(TrakBox trak) {
        MediaInfoBox minf = trak.getMdia().getMinf();
        DataInfoBox dinf = trak.getMdia().getMinf().getDinf();
        if (dinf == null) {
            dinf = new DataInfoBox();
            minf.add(dinf);
        }

        DataRefBox dref = dinf.getDref();
        if (dref == null) {
            dref = new DataRefBox();
            dinf.add(dref);
        }

        dref.getBoxes().clear();
        dref.add(AliasBox.createSelfRef());

        for (SampleEntry entry : NodeBox.findAll(trak, SampleEntry.class, "mdia", "minf", "stbl", "stsd", null)) {
            entry.setDrefInd((short) 1);
        }
    }

    private RandomAccessInputStream getInput(Chunk chunk) {
        SampleEntry se = entries[chunk.getEntry() - 1];
        return inputs[se.getDrefInd() - 1];
    }

    public void write(Chunk chunk) throws IOException {
        RandomAccessInputStream input = getInput(chunk);
        input.seek(chunk.getOffset());
        long filePointer = out.getFilePointer();
        WindowInputStream inp = new WindowInputStream(input, chunk.getSize());
        int read;
        while ((read = inp.read(buf)) != -1)
            out.write(buf, 0, read);
        offsets[curChunk++] = filePointer;
    }
}