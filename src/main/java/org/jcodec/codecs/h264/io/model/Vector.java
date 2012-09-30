package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Vector structure used for storing motion vectors
 * 
 * @author Jay Codec
 * 
 */
public class Vector {
    private int x;
    private int y;
    private int refId;

    public Vector(int x, int y, int refId) {
        this.x = x;
        this.y = y;
        this.refId = refId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRefId() {
        return refId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + refId;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vector other = (Vector) obj;
        if (refId != other.refId)
            return false;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Vector [x=" + x + ", y=" + y + ", ref=" + refId + "]";
    }
}
