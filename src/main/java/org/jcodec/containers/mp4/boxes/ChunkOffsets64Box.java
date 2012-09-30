package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        int length = (int) ReaderBE.readInt32(input);
        chunkOffsets = new long[length];
        for (int i = 0; i < length; i++) {
            chunkOffsets[i] = ReaderBE.readInt64(input);
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(chunkOffsets.length);
        for (long offset : chunkOffsets) {
            out.writeLong(offset);
        }
    }
    
    public long[] getChunkOffsets() {
        return chunkOffsets;
    }

    public void setChunkOffsets(long[] chunkOffsets) {
        this.chunkOffsets = chunkOffsets;
    }
}
