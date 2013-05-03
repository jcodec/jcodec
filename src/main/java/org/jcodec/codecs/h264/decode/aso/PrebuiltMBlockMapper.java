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
public class PrebuiltMBlockMapper implements Mapper {

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

    public int getAddress(int mbIndex) {
        return map.getInverse()[groupId][mbIndex + indexOfFirstMb];
    }

    public boolean leftAvailable(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int leftMBAddr = mbAddr - 1;

        return !((leftMBAddr < firstMBInSlice) || ((mbAddr % picWidthInMbs) == 0) || (map.getGroups()[leftMBAddr] != groupId));
    }

    public boolean topAvailable(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int topMBAddr = mbAddr - picWidthInMbs;

        return !((topMBAddr < firstMBInSlice) || (map.getGroups()[topMBAddr] != groupId));
    }

    public int getMbX(int index) {
        return getAddress(index) % picWidthInMbs;
    }

    public int getMbY(int index) {
        return getAddress(index) / picWidthInMbs;
    }

    public boolean topRightAvailable(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int topRMBAddr = mbAddr - picWidthInMbs + 1;

        return !((topRMBAddr < firstMBInSlice) || (((mbAddr + 1) % picWidthInMbs) == 0) || (map.getGroups()[topRMBAddr] != groupId));
    }

    public boolean topLeftAvailable(int mbIndex) {
        int mbAddr = map.getInverse()[groupId][mbIndex + indexOfFirstMb];
        int topLMBAddr = mbAddr - picWidthInMbs - 1;

        return !((topLMBAddr < firstMBInSlice) || ((mbAddr % picWidthInMbs) == 0) || (map.getGroups()[topLMBAddr] != groupId));
    }
}