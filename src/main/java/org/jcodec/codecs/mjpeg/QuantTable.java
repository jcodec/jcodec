package org.jcodec.codecs.mjpeg;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author Jay Codec
 *
 */
public class QuantTable {
    private int index;
    private int[] values;

    public int[] getValues() {
        return values;
    }

    public void setValues(int[] values) {
        this.values = values;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    QuantTable(int index, int[] values) {
        this.index = index;
        this.values = values;
    }

}
