package org.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A block map that that maps macroblocks sequentially in scan order
 * 
 * @author Jay Codec
 * 
 */
public class FlatMBlockMapper implements MBlockMapper {
    private int frameWidthInMbs;
    private int firstMBAddr;

    public FlatMBlockMapper(int frameWidthInMbs, int firstMBAddr) {
        this.frameWidthInMbs = frameWidthInMbs;
        this.firstMBAddr = firstMBAddr;
    }

    public int getLeftMBIdx(int index) {
        int mbAddr = index + firstMBAddr;
        boolean atTheBorder = mbAddr % frameWidthInMbs == 0;
        boolean leftInSlice = !atTheBorder && (mbAddr > firstMBAddr);

        return leftInSlice ? (mbAddr - 1 - firstMBAddr) : -1;
    }

    public int getTopLeftMBIndex(int index) {
        int mbAddr = index + firstMBAddr;
        boolean atTheBorder = mbAddr % frameWidthInMbs == 0;
        boolean topLeftInSlice = !atTheBorder && (mbAddr - frameWidthInMbs > firstMBAddr);

        return topLeftInSlice ? (mbAddr - frameWidthInMbs - 1 - firstMBAddr) : -1;
    }

    public int getTopMBIdx(int index) {
        int mbAddr = index + firstMBAddr;
        boolean topInSlice = mbAddr - frameWidthInMbs >= firstMBAddr;

        return topInSlice ? (mbAddr - frameWidthInMbs - firstMBAddr) : -1;
    }

    public int getTopRightMBIndex(int index) {
        int mbAddr = index + firstMBAddr;

        boolean atTheBorder = (mbAddr % frameWidthInMbs) == frameWidthInMbs - 1;
        boolean topRightInSlice = !atTheBorder && (mbAddr - frameWidthInMbs + 1 >= firstMBAddr);

        return topRightInSlice ? (mbAddr - frameWidthInMbs + 1 - firstMBAddr) : -1;
    }

    public int[] getAddresses(int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = firstMBAddr + i;
        }

        return result;
    }

}
