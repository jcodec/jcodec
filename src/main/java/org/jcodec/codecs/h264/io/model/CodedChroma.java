package org.jcodec.codecs.h264.io.model;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Residual chroma layer of macroblock
 * 
 * @author Jay Codec
 * 
 */
public class CodedChroma {
    private ResidualBlock cbDC;
    private ResidualBlock[] cbAC;
    private ResidualBlock crDC;
    private ResidualBlock[] crAC;
    private CoeffToken[] coeffTokenCb;
    private CoeffToken[] coeffTokenCr;

    public CodedChroma(ResidualBlock cbDC, ResidualBlock[] cbAC, ResidualBlock crDC, ResidualBlock[] crAC,
            CoeffToken[] coeffTokenCb, CoeffToken[] coeffTokenCr) {
        this.cbDC = cbDC;
        this.cbAC = cbAC;
        this.crDC = crDC;
        this.crAC = crAC;
        this.coeffTokenCb = coeffTokenCb;
        this.coeffTokenCr = coeffTokenCr;
    }

    public ResidualBlock getCbDC() {
        return cbDC;
    }

    public ResidualBlock[] getCbAC() {
        return cbAC;
    }

    public ResidualBlock getCrDC() {
        return crDC;
    }

    public ResidualBlock[] getCrAC() {
        return crAC;
    }

    public CoeffToken[] getCoeffTokenCb() {
        return coeffTokenCb;
    }

    public CoeffToken[] getCoeffTokenCr() {
        return coeffTokenCr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(cbAC);
        result = prime * result + ((cbDC == null) ? 0 : cbDC.hashCode());
        result = prime * result + Arrays.hashCode(coeffTokenCb);
        result = prime * result + Arrays.hashCode(coeffTokenCr);
        result = prime * result + Arrays.hashCode(crAC);
        result = prime * result + ((crDC == null) ? 0 : crDC.hashCode());
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
        CodedChroma other = (CodedChroma) obj;
        if (!Arrays.equals(cbAC, other.cbAC))
            return false;
        if (cbDC == null) {
            if (other.cbDC != null)
                return false;
        } else if (!cbDC.equals(other.cbDC))
            return false;
        if (!Arrays.equals(coeffTokenCb, other.coeffTokenCb))
            return false;
        if (!Arrays.equals(coeffTokenCr, other.coeffTokenCr))
            return false;
        if (!Arrays.equals(crAC, other.crAC))
            return false;
        if (crDC == null) {
            if (other.crDC != null)
                return false;
        } else if (!crDC.equals(other.crDC))
            return false;
        return true;
    }
}
