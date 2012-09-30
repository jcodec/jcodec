package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * A box to hold chunk offsets
 * 
 * @author The JCodec project
 * 
 */

public class ChunkOffsetsBox extends FullBox {
    private long[] chunkOffsets;
    
    public static String fourcc() {
        return "stco";
    }

    public ChunkOffsetsBox(long[] chunkOffsets) {
        super(new Header(fourcc()));
        this.chunkOffsets = chunkOffsets;
    }

    public ChunkOffsetsBox() {
        super(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        int length = (int) ReaderBE.readInt32(input);
        chunkOffsets = new long[length];
        for (int i = 0; i < length; i++) {
            chunkOffsets[i] = ReaderBE.readInt32(input);
        }
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(chunkOffsets.length);
        for (long offset : chunkOffsets) {
            out.writeInt((int) offset);
        }
    }

    public long[] getChunkOffsets() {
        return chunkOffsets;
    }

    public void setChunkOffsets(long[] chunkOffsets) {
        this.chunkOffsets = chunkOffsets;
    }
}
