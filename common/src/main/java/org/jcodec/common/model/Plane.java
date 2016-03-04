package org.jcodec.common.model;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class Plane {
    int[] data;
    Size size;

    public Plane(int[] data, Size size) {
        super();
        this.data = data;
        this.size = size;
    }

    public int[] getData() {
        return data;
    }

    public Size getSize() {
        return size;
    }
}
