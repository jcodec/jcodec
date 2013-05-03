package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class FullBox extends Box {
    
    public FullBox(Header atom) {
        super(atom);
    }

    protected byte version;
    protected int flags;

    public void parse(ByteBuffer input) {
        int vf = input.getInt();
        version = (byte)((vf >> 24) & 0xff);
        flags = vf & 0xffffff;
    }
    
    protected void doWrite(ByteBuffer out) {
        out.putInt((version << 24) | (flags & 0xffffff));
    }

    public byte getVersion() {
        return version;
    }

    public int getFlags() {
        return flags;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }
}