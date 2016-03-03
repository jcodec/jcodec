package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Box type
 * 
 * @author The JCodec project
 * 
 */
public class ChunkOffsets64Box extends FullBox {
    private long[] chunkOffsets;
    
    public static String fourcc() {
        return "co64";
    }

    public static ChunkOffsets64Box createChunkOffsets64Box(long[] offsets) {
        ChunkOffsets64Box co64 = new ChunkOffsets64Box(Header.createHeader(fourcc(), 0));
        co64.chunkOffsets = offsets;
        return co64;
    }

    public ChunkOffsets64Box(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        int length = input.getInt();
        chunkOffsets = new long[length];
        for (int i = 0; i < length; i++) {
            chunkOffsets[i] = input.getLong();
        }
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(chunkOffsets.length);
        for (int i = 0; i < chunkOffsets.length; i++) {
            long offset = chunkOffsets[i];
            out.putLong(offset);
        }
    }
    
    public long[] getChunkOffsets() {
        return chunkOffsets;
    }

    public void setChunkOffsets(long[] chunkOffsets) {
        this.chunkOffsets = chunkOffsets;
    }
}
