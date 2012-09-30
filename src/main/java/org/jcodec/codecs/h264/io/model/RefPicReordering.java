package org.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains reordering instructions for reference picture list
 * 
 * @author Jay Codec
 * 
 */
public class RefPicReordering {

    public static enum InstrType {
        FORWARD, BACKWARD, LONG_TERM
    };

    public static class ReorderOp {
        private InstrType type;
        private int param;

        public ReorderOp(InstrType type, int param) {
            this.type = type;
            this.param = param;
        }

        public InstrType getType() {
            return type;
        }

        public int getParam() {
            return param;
        }
    }

    private ReorderOp[] instructions;

    public RefPicReordering(ReorderOp[] instructions) {
        this.instructions = instructions;
    }

    public ReorderOp[] getInstructions() {
        return instructions;
    }
}
