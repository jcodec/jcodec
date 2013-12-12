package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * A box storing a list of synch samples
 * 
 * @author Jay Codec
 * 
 */
public class SyncSamplesBox extends FullBox {
    private int[] syncSamples;
    
    public static String fourcc() {
        return "stss";
    }

    public SyncSamplesBox() {
        super(new Header(fourcc()));
    }

    public SyncSamplesBox(int[] array) {
        this();
        this.syncSamples = array;
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

    public int[] getSyncSamples() {
        return syncSamples;
    }
}
