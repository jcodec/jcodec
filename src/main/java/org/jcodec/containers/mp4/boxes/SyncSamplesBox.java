package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A box storing a list of synch samples
 * 
 * @author The JCodec project
 * 
 */
public class SyncSamplesBox extends FullBox {
    public static final String STSS = "stss";
    protected int[] syncSamples;

    public static SyncSamplesBox createSyncSamplesBox(int[] array) {
        SyncSamplesBox stss = new SyncSamplesBox(new Header(STSS));
        stss.syncSamples = array;
        return stss;
    }

    public SyncSamplesBox(Header header) {
        super(header);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        int len = input.getInt();
        syncSamples = new int[len];
        for (int i = 0; i < len; i++) {
            syncSamples[i] = input.getInt();
        }
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(syncSamples.length);
        for (int i = 0; i < syncSamples.length; i++)
            out.putInt((int) syncSamples[i]);
    }

    @Override
    public int estimateSize() {
        return 16 + syncSamples.length * 4;
    }
    
    public int[] getSyncSamples() {
        return syncSamples;
    }
}
