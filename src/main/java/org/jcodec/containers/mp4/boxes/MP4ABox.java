package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class MP4ABox extends Box {

    private int val;

    public MP4ABox(int val) {
        super(new Header("mp4a"));
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.writeInt(val);
    }

    public void parse(InputStream input) throws IOException {
        val = (int) ReaderBE.readInt32(input);
    }
}
