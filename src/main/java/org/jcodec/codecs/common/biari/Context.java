package org.jcodec.codecs.common.biari;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Context model for table-based binary arithmetic encoders/decoders
 * 
 * Stores probability state table index and current MPS symbol value
 * 
 * @author Jay Codec
 * 
 */
public class Context {
    private int stateIdx;
    private int mps;

    public Context(int state, int mps) {
        this.stateIdx = state;
        this.mps = mps;
    }

    public int getState() {
        return stateIdx;
    }

    public int getMps() {
        return mps;
    }

    public void setMps(int mps) {
        this.mps = mps;
    }

    public void setState(int state) {
        this.stateIdx = state;
    }
}
