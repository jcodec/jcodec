package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A leaf box
 * 
 * A box containing data, no children
 * 
 * @author Jay Codec
 * 
 */
public class LeafBox extends Box {
    private byte[] data;

    public LeafBox(Header atom) {
        super(atom);
    }

    public LeafBox(Header atom, byte[] data) {
        super(atom);
        this.data = data;
    }

    public void parse(InputStream input) throws IOException {
        data = new byte[(int) header.getBodySize()];

        input.read(data);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    protected void doWrite(DataOutput out) throws IOException {
        out.write(data);
    }
}
