package org.jcodec.codecs.h264.io.model;

import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A block of residual data
 * 
 * @author Jay Codec
 * 
 */
public class ResidualBlock {
    private int[] coeffs;

    public static class BlockType {
        public static final BlockType BLOCK_LUMA_4x4 = new BlockType("Luma4x4", 16);
        public static final BlockType BLOCK_LUMA_8x8 = new BlockType("Luma8x8", 64);
        public static final BlockType BLOCK_CHROMA_DC = new BlockType("ChromaDC", 16);
        public static final BlockType BLOCK_CHROMA_AC = new BlockType("ChromaAC", 15);
        public static final BlockType BLOCK_LUMA_16x16_DC = new BlockType("Luma16x16DC", 16);
        public static final BlockType BLOCK_LUMA_16x16_AC = new BlockType("Luma16x16AC", 15);

        private String label;
        private int maxCoeffs;

        private BlockType(String label, int maxCoeffs) {
            this.label = label;
            this.maxCoeffs = maxCoeffs;
        }

        public String getLabel() {
            return this.label;
        }

        public int getMaxCoeffs() {
            return maxCoeffs;
        }

    }

    public ResidualBlock(int[] coeffs) {
        this.coeffs = coeffs;
    }

    public int[] getCoeffs() {
        return coeffs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(coeffs);
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
        ResidualBlock other = (ResidualBlock) obj;
        if (!Arrays.equals(coeffs, other.coeffs))
            return false;
        return true;
    }
}
