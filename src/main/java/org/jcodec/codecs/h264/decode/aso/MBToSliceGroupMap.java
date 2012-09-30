package org.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Contains a mapping of macroblocks to slice groups. Groups is an array of
 * group slice group indices having a dimension picWidthInMbs x picHeightInMbs
 * 
 * @author Jay Codec
 * 
 */
public class MBToSliceGroupMap {
    private int[] groups;
    private int[] indices;
    private int[][] inverse;

    public MBToSliceGroupMap(int[] groups, int[] indices, int[][] inverse) {
        this.groups = groups;
        this.indices = indices;
        this.inverse = inverse;
    }

    public int[] getGroups() {
        return groups;
    }

    public int[] getIndices() {
        return indices;
    }

    public int[][] getInverse() {
        return inverse;
    }
}
