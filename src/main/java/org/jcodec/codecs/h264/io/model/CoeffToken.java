package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class CoeffToken {
    // A constant for not available
    public static CoeffToken NA = new CoeffToken(0, 0);

    public int totalCoeff;
    public int trailingOnes;

    public CoeffToken(int totalCoeff, int trailingOnes) {
        this.totalCoeff = totalCoeff;
        this.trailingOnes = trailingOnes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + totalCoeff;
        result = prime * result + trailingOnes;
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
        CoeffToken other = (CoeffToken) obj;
        if (totalCoeff != other.totalCoeff)
            return false;
        if (trailingOnes != other.trailingOnes)
            return false;
        return true;
    }

}