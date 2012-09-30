package org.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A macrboblock to slice group mapper that operates on prebuilt map, passed to
 * it in the constructor
 * 
 * @author Jay Codec
 * 
 */
public class PrebuiltMBlockMapper implements MBlockMapper {

    private MBToSliceGroupMap map;
    private int firstMBInSlice;
    private int groupId;
    private int picWidthInMbs;
    private int indexOfFirstMb;

    public PrebuiltMBlockMapper(MBToSliceGroupMap map, int firstMBInSlice, int picWidthInMbs) {
        this.map = map;
        this.firstMBInSlice = firstMBInSlice;
        this.groupId = map.getGroups()[firstMBInSlice];
        this.picWidthInMbs = picWidthInMbs;
        this.indexOfFirstMb = map.getIndices()[firstMBInSlice];
    }

    public int[] getAddresses(int count) {

        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = map.getInverse()[groupId][i + indexOfFirstMb];
        }

        return result;
    }

    public int getLeftMBIdx(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int leftMBAddr = mbAddr - 1;

        if ((leftMBAddr < firstMBInSlice) || (mbAddr % picWidthInMbs == 0) || (map.getGroups()[leftMBAddr] != groupId))
            return -1;

        return map.getIndices()[leftMBAddr] - indexOfFirstMb;
    }

    public int getTopLeftMBIndex(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int topLeftMBAddr = mbAddr - picWidthInMbs - 1;

        if ((topLeftMBAddr < firstMBInSlice) || (mbAddr % picWidthInMbs == 0)
                || (map.getGroups()[topLeftMBAddr] != groupId))
            return -1;

        return map.getIndices()[topLeftMBAddr] - indexOfFirstMb;
    }

    public int getTopMBIdx(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int topMBAddr = mbAddr - picWidthInMbs;

        if ((topMBAddr < firstMBInSlice) || (map.getGroups()[topMBAddr] != groupId))
            return -1;

        return map.getIndices()[topMBAddr] - indexOfFirstMb;
    }

    public int getTopRightMBIndex(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int topRightMBAddr = mbAddr - picWidthInMbs + 1;

        if ((topRightMBAddr < firstMBInSlice) || (mbAddr % picWidthInMbs == picWidthInMbs - 1)
                || (map.getGroups()[topRightMBAddr] != groupId))
            return -1;

        return map.getIndices()[topRightMBAddr] - indexOfFirstMb;
    }
}