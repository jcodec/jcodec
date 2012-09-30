package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

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

    public void parse(InputStream input) throws IOException {
        super.parse(input);
        int len = (int) ReaderBE.readInt32(input);
        syncSamples = new int[len];
        for (int i = 0; i < len; i++) {
            syncSamples[i] = (int)ReaderBE.readInt32(input);
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(syncSamples.length);
        for (int i = 0; i < syncSamples.length; i++)
            out.writeInt((int) syncSamples[i]);
    }

    public int[] getSyncSamples() {
        return syncSamples;
    }
}
