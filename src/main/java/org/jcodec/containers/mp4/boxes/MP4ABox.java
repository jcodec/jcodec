package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

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

    protected void doWrite(ByteBuffer out) {
        out.putInt(val);
    }

    public void parse(ByteBuffer input) {
        val = input.getInt();
    }
}
