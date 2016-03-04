package org.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A block map that that maps macroblocks sequentially in scan order
 * 
 * @author The JCodec project
 * 
 */
public class FlatMBlockMapper implements Mapper {
    private int frameWidthInMbs;
    private int firstMBAddr;

    public FlatMBlockMapper(int frameWidthInMbs, int firstMBAddr) {
        this.frameWidthInMbs = frameWidthInMbs;
        this.firstMBAddr = firstMBAddr;
    }

    public boolean leftAvailable(int index) {
        int mbAddr = index + firstMBAddr;
        boolean atTheBorder = mbAddr % frameWidthInMbs == 0;
        return !atTheBorder && (mbAddr > firstMBAddr);
    }

    public boolean topAvailable(int index) {
        int mbAddr = index + firstMBAddr;
        return mbAddr - frameWidthInMbs >= firstMBAddr;
    }

    public int getAddress(int index) {
        return firstMBAddr + index;
    }

    public int getMbX(int index) {
        return getAddress(index) % frameWidthInMbs;
    }

    public int getMbY(int index) {
        return getAddress(index) / frameWidthInMbs;
    }

    public boolean topRightAvailable(int index) {
        int mbAddr = index + firstMBAddr;
        boolean atTheBorder = (mbAddr + 1) % frameWidthInMbs == 0;
        return !atTheBorder && mbAddr - frameWidthInMbs + 1 >= firstMBAddr;
    }

    public boolean topLeftAvailable(int index) {
        int mbAddr = index + firstMBAddr;
        boolean atTheBorder = mbAddr % frameWidthInMbs == 0;
        return !atTheBorder && mbAddr - frameWidthInMbs - 1 >= firstMBAddr;
    }
}
