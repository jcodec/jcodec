package org.jcodec.containers.mxf.read;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MXFPartitionPack extends MXFMetadata {

    private int kagSize;
    private long thisPartition;
    private long prevPartition;
    private long footerPartition;
    private long headerByteCount;
    private long indexByteCount;
    private int indexSid;
    private int bodySid;
    private UL op;
    private int nbEssenceContainers;
    
    public MXFPartitionPack(UL ul) {
        super(ul);
    }

    @Override
    public void read(ByteBuffer bb) {
        bb.order(ByteOrder.BIG_ENDIAN);
        NIOUtils.skip(bb, 4);

        kagSize = bb.getInt();
        thisPartition = bb.getLong();
        prevPartition = bb.getLong();
        footerPartition = bb.getLong();
        headerByteCount = bb.getLong();
        indexByteCount = bb.getLong();
        indexSid = bb.getInt();
        NIOUtils.skip(bb, 8);
        bodySid = bb.getInt();
        op = UL.read(bb);
        nbEssenceContainers = bb.getInt();
    }

    public int getKagSize() {
        return kagSize;
    }

    public long getThisPartition() {
        return thisPartition;
    }

    public long getPrevPartition() {
        return prevPartition;
    }

    public long getFooterPartition() {
        return footerPartition;
    }

    public long getHeaderByteCount() {
        return headerByteCount;
    }

    public long getIndexByteCount() {
        return indexByteCount;
    }

    public int getIndexSid() {
        return indexSid;
    }

    public int getBodySid() {
        return bodySid;
    }

    public UL getOp() {
        return op;
    }

    public int getNbEssenceContainers() {
        return nbEssenceContainers;
    }
}