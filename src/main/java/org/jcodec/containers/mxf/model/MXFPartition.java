package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MXFPartition {

    private MXFPartitionPack pack;
    private long essenceFilePos;
    private boolean closed;
    private boolean complete;
    private long essenceLength;

    public MXFPartition(MXFPartitionPack pack, long essenceFilePos, boolean closed, boolean complete, long essenceLength) {
        this.pack = pack;
        this.essenceFilePos = essenceFilePos;
        this.closed = closed;
        this.complete = complete;
        this.essenceLength = essenceLength;
    }

    public static MXFPartition read(UL ul, ByteBuffer bb, long packSize, long nextPartition) {
        boolean closed = (ul.get(14) & 1) == 0;
        boolean complete = ul.get(14) > 2;

        MXFPartitionPack pp = new MXFPartitionPack(ul);
        pp.read(bb);

        long essenceFilePos = roundToKag(pp.getThisPartition() + packSize, pp.getKagSize())
                + roundToKag(pp.getHeaderByteCount(), pp.getKagSize())
                + roundToKag(pp.getIndexByteCount(), pp.getKagSize());

        return new MXFPartition(pp, essenceFilePos, closed, complete, nextPartition - essenceFilePos);
    }

    static long roundToKag(long position, int kag_size) {
        long ret = (position / kag_size) * kag_size;
        return ret == position ? ret : ret + kag_size;
    }

    public MXFPartitionPack getPack() {
        return pack;
    }

    public long getEssenceFilePos() {
        return essenceFilePos;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isComplete() {
        return complete;
    }

    public long getEssenceLength() {
        return essenceLength;
    }
}
