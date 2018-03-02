package org.jcodec.containers.mp4.boxes;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MP4ABox extends Box {

    public MP4ABox(Header header) {
        super(header);
    }

    private int val;

    protected void doWrite(ByteBuffer out) {
        out.putInt(val);
    }

    public void parse(ByteBuffer input) {
        val = input.getInt();
    }

    @Override
    public int estimateSize() {
        return 12;
    }
}
