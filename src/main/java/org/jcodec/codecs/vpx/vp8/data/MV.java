package org.jcodec.codecs.vpx.vp8.data;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class MV {
    public short row;
    public short col;

    public MV() {
        row = 0;
        col = 0;
    }

    public MV(short r, short c) {
        row = r;
        col = c;
    }

    public MV(int r, int c) {
        row = (short) r;
        col = (short) c;
    }

    public MV(MV other) {
        set(other);
    }

    public void set(MV other) {
        if (other == null) {
            setZero();
        } else {
            this.row = other.row;
            this.col = other.col;
        }
    }

    public void setRC(short row, short col) {
        this.row = row;
        this.col = col;
    }

    public void setRC(int row, int col) {
        this.row = (short) row;
        this.col = (short) col;
    }

    public MV add(MV other) {
        this.row += other.row;
        this.col += other.col;
        return this;
    }

    public boolean isZero() {
        return row == 0 && col == 0;
    }

    public void setZero() {
        row = 0;
        col = 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof MV) {
            final MV other = (MV) obj;
            return other.col == col && other.row == row;
        }
        return false;
    }

    public MV copy() {
        return new MV(this);
    }

    public MV div8() {
        return new MV((short) (row >> 3), (short) (col >> 3));
    }

    public MV mul8() {
        return new MV((short) (row << 3), (short) (col << 3));
    }
}