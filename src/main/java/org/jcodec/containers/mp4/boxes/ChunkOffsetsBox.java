package org.jcodec.containers.mp4.boxes;

import org.jcodec.containers.mp4.boxes.Box.AtomField;
import org.jcodec.platform.Platform;

import java.nio.ByteBuffer;

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

    public ChunkOffsetsBox(Header atom) {
        super(atom);
    }

    private long[] chunkOffsets;
    
    public static String fourcc() {
        return "stco";
    }

    public static ChunkOffsetsBox createChunkOffsetsBox(long[] chunkOffsets) {
        ChunkOffsetsBox stco = new ChunkOffsetsBox(new Header(fourcc()));
        stco.chunkOffsets = chunkOffsets;
        return stco;
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        int length = input.getInt();
        chunkOffsets = new long[length];
        for (int i = 0; i < length; i++) {
            chunkOffsets[i] = Platform.unsignedInt(input.getInt());
        }
    }

    @Override
    public void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(chunkOffsets.length);
        for (int i = 0; i < chunkOffsets.length; i++) {
            long offset = chunkOffsets[i];
            out.putInt((int) offset);
        }
    }
    
    @Override
    public int estimateSize() {
        return 12 + 4 + chunkOffsets.length * 4;
    }

    @AtomField(idx=0)
    public long[] getChunkOffsets() {
        return chunkOffsets;
    }

    public void setChunkOffsets(long[] chunkOffsets) {
        this.chunkOffsets = chunkOffsets;
    }
}
