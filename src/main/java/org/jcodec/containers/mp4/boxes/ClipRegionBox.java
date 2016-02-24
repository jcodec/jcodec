package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
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

    public static ClipRegionBox createClipRegionBox(short x, short y, short width, short height) {
        ClipRegionBox b = new ClipRegionBox(new Header(fourcc()));
        b.rgnSize = 10;
        b.x = x;
        b.y = y;
        b.width = width;
        b.height = height;
        return b;
    }

    public ClipRegionBox(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer input) {
        rgnSize = input.getShort();
        y = input.getShort();
        x = input.getShort();
        height = input.getShort();
        width = input.getShort();
    }

    protected void doWrite(ByteBuffer out) {
        out.putShort(rgnSize);
        out.putShort(y);
        out.putShort(x);
        out.putShort(height);
        out.putShort(width);
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