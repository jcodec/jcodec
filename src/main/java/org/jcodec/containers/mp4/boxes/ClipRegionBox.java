package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ClipRegionBox extends Box {
    
    private short rgnSize;
    private short y;
    private short x;
    private short height;
    private short width;
    
    public static String fourcc() {
        return "crgn";
    }

    public ClipRegionBox(Header atom) {
        super(atom);
    }
    
    public ClipRegionBox() {
        super(new Header(fourcc()));
    }

    public ClipRegionBox(short x, short y, short width, short height) {
        this();
        rgnSize = 10;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void parse(InputStream input) throws IOException {
        rgnSize = (short)ReaderBE.readInt16(input);
        y = (short)ReaderBE.readInt16(input);
        x = (short)ReaderBE.readInt16(input);
        height = (short)ReaderBE.readInt16(input);
        width = (short)ReaderBE.readInt16(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.writeShort(rgnSize);
        out.writeShort(y);
        out.writeShort(x);
        out.writeShort(height);
        out.writeShort(width);
    }

    public short getRgnSize() {
        return rgnSize;
    }

    public short getY() {
        return y;
    }

    public short getX() {
        return x;
    }

    public short getHeight() {
        return height;
    }

    public short getWidth() {
        return width;
    }
}