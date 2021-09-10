package org.jcodec.codecs.vpx.vp8.pointerhelper;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */public class PositionableIntArrPointer extends ReadOnlyIntArrPointer {
    private int savedPos;

    public PositionableIntArrPointer(short[] data, int pos) {
        super(data, pos);
        savedPos = pos;
    }

    public PositionableIntArrPointer(PositionableIntArrPointer other) {
        super(other);
        savedPos = other.pos;
    }

    public void dec() {
        pos--;
    }

    public short getAndInc() {
        return arr[pos++];
    }

    public void inc() {
        pos++;
    }

    public void incBy(int r) {
        pos += r;
    }

    public void rewind() {
        pos = 0;
    }

    public void rewindToSaved() {
        pos = savedPos;
    }

    public void savePos() {
        savedPos = pos;
    }

    public void setPos(int p) {
        pos = p;
    }
    
    public static PositionableIntArrPointer makePositionable(ReadOnlyIntArrPointer other) {
        return new PositionableIntArrPointer(other.arr, other.pos);
    }

    public static PositionableIntArrPointer makePositionableAndInc(ReadOnlyIntArrPointer other, int incby) {
        return new PositionableIntArrPointer(other.arr, other.pos + incby);
    }

}
