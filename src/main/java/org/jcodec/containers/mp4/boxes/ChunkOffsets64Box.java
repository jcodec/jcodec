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

    public ChunkOffsets64Box(Header atom) {
        super(atom);
    }
    
    public ChunkOffsets64Box() {
        super(new Header(fourcc(), 0));
    }

    public ChunkOffsets64Box(long[] offsets) {
        this();
        this.chunkOffsets = offsets;
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
        for (long offset : chunkOffsets) {
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
