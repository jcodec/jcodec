package org.jcodec.codecs.h264.decode.aso;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A helper class that builds macroblock to slice group maps needed by ASO
 * (Arbitrary Slice Order)
 * 
 * @author The JCodec project
 * 
 */
public class SliceGroupMapBuilder {

    /**
     * 
     * Interleaved slice group map. Each slice group fills a number of cells
     * equal to the appropriate run length, then followed by the next slice
     * group.
     * 
     * Example:
     * 
     * 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1,
     * 
     */
    public static int[] buildInterleavedMap(int picWidthInMbs, int picHeightInMbs, int[] runLength) {

        int numSliceGroups = runLength.length;
        int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        int[] groups = new int[picSizeInMbs];

        int i = 0;
        do {
            for (int iGroup = 0; iGroup < numSliceGroups && i < picSizeInMbs; i += runLength[iGroup++]) {
                for (int j = 0; j < runLength[iGroup] && i + j < picSizeInMbs; j++) {
                    groups[i + j] = iGroup;
                }
            }
        } while (i < picSizeInMbs);

        return groups;
    }

    /**
     * A dispersed map. Every odd line starts from the (N / 2)th group
     * 
     * Example:
     * 
     * 0, 1, 2, 3, 0, 1, 2, 3 2, 3, 0, 1, 2, 3, 0, 1 0, 1, 2, 3, 0, 1, 2, 3 2,
     * 3, 0, 1, 2, 3, 0, 1 0, 1, 2, 3, 0, 1, 2, 3 2, 3, 0, 1, 2, 3, 0, 1 0, 1,
     * 2, 3, 0, 1, 2, 3 2, 3, 0, 1, 2, 3, 0, 1 0, 1, 2, 3, 0, 1, 2, 3 2, 3, 0,
     * 1, 2, 3, 0, 1
     * 
     */
    public static int[] buildDispersedMap(int picWidthInMbs, int picHeightInMbs, int numSliceGroups) {

        int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        int[] groups = new int[picSizeInMbs];

        for (int i = 0; i < picSizeInMbs; i++) {
            int group = ((i % picWidthInMbs) + (((i / picWidthInMbs) * numSliceGroups) / 2)) % numSliceGroups;
            groups[i] = group;
        }

        return groups;
    }

    /**
     * 
     * A foreground macroblock to slice group map. Macroblocks of the last slice
     * group are the background, all the others represent rectangles covering
     * areas with top-left corner specified by topLeftAddr[group] and bottom
     * right corner specified by bottomRightAddr[group].
     * 
     * @param picWidthInMbs
     * @param picHeightInMbs
     * @param numSliceGroups
     *            Total number of slice groups
     * @param topLeftAddr
     *            Addresses of macroblocks that are top-left corners of
     *            respective slice groups
     * @param bottomRightAddr
     *            Addresses macroblocks that are bottom-right corners of
     *            respective slice groups
     * @return
     */
    public static int[] buildForegroundMap(int picWidthInMbs, int picHeightInMbs, int numSliceGroups,
            int[] topLeftAddr, int[] bottomRightAddr) {

        int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        int[] groups = new int[picSizeInMbs];

        for (int i = 0; i < picSizeInMbs; i++)
            groups[i] = numSliceGroups - 1;

        int tot = 0;
        for (int iGroup = numSliceGroups - 2; iGroup >= 0; iGroup--) {

            int yTopLeft = topLeftAddr[iGroup] / picWidthInMbs;
            int xTopLeft = topLeftAddr[iGroup] % picWidthInMbs;
            int yBottomRight = bottomRightAddr[iGroup] / picWidthInMbs;
            int xBottomRight = bottomRightAddr[iGroup] % picWidthInMbs;

            int sz = (yBottomRight - yTopLeft + 1) * (xBottomRight - xTopLeft + 1);
            tot += sz;

            int ind = 0;
            for (int y = yTopLeft; y <= yBottomRight; y++)
                for (int x = xTopLeft; x <= xBottomRight; x++) {
                    int mbAddr = y * picWidthInMbs + x;
                    groups[mbAddr] = iGroup;
                }
        }

        return groups;
    }

    /**
     * A boxout macroblock to slice group mapping. Only applicable when there's
     * exactly 2 slice groups. Slice group 1 is a background, while slice group
     * 0 is a box in the middle of the frame.
     * 
     * @param picWidthInMbs
     * @param picHeightInMbs
     * @param changeDirection
     * @param numberOfMbsInBox
     *            number of macroblocks in slice group 0
     * @return
     */
    public static int[] buildBoxOutMap(int picWidthInMbs, int picHeightInMbs, boolean changeDirection,
            int numberOfMbsInBox) {

        int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        int[] groups = new int[picSizeInMbs];
        int changeDirectionInt = changeDirection ? 1 : 0;

        for (int i = 0; i < picSizeInMbs; i++)
            groups[i] = 1;

        int x = (picWidthInMbs - changeDirectionInt) / 2;
        int y = (picHeightInMbs - changeDirectionInt) / 2;
        int leftBound = x;
        int topBound = y;
        int rightBound = x;
        int bottomBound = y;
        int xDir = changeDirectionInt - 1;
        int yDir = changeDirectionInt;

        boolean mapUnitVacant = false;
        for (int k = 0; k < numberOfMbsInBox; k += (mapUnitVacant ? 1 : 0)) {
            int mbAddr = y * picWidthInMbs + x;
            mapUnitVacant = (groups[mbAddr] == 1);
            if (mapUnitVacant) {
                groups[mbAddr] = 0;
            }
            if (xDir == -1 && x == leftBound) {
                leftBound = Max(leftBound - 1, 0);
                x = leftBound;
                xDir = 0;
                yDir = 2 * changeDirectionInt - 1;
            } else if (xDir == 1 && x == rightBound) {
                rightBound = Min(rightBound + 1, picWidthInMbs - 1);
                x = rightBound;
                xDir = 0;
                yDir = 1 - 2 * changeDirectionInt;
            } else if (yDir == -1 && y == topBound) {
                topBound = Max(topBound - 1, 0);
                y = topBound;
                xDir = 1 - 2 * changeDirectionInt;
                yDir = 0;
            } else if (yDir == 1 && y == bottomBound) {
                bottomBound = Min(bottomBound + 1, picHeightInMbs - 1);
                y = bottomBound;
                xDir = 2 * changeDirectionInt - 1;
                yDir = 0;
            } else {
                x += xDir;
                y += yDir;
            }
        }

        return groups;
    }

    private static int Min(int i, int j) {
        return i < j ? i : j;
    }

    private static int Max(int i, int j) {
        return i > j ? i : j;
    }

    /**
     * 
     * A macroblock to slice group map that fills frame in raster scan.
     * 
     * @param picWidthInMbs
     * @param picHeightInMbs
     * @param sizeOfUpperLeftGroup
     * @param changeDirection
     * @return
     */
    public static int[] buildRasterScanMap(int picWidthInMbs, int picHeightInMbs, int sizeOfUpperLeftGroup,
            boolean changeDirection) {

        int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        int[] groups = new int[picSizeInMbs];
        int changeDirectionInt = changeDirection ? 1 : 0;

        int i;
        for (i = 0; i < sizeOfUpperLeftGroup; i++) {
            groups[i] = changeDirectionInt;
        }

        for (; i < picSizeInMbs; i++) {
            groups[i] = 1 - changeDirectionInt;
        }

        return groups;
    }

    /**
     * A macroblock to slice group map that fills frame column by column
     * 
     * @param picWidthInMbs
     * @param picHeightInMbs
     * @param sizeOfUpperLeftGroup
     * @param changeDirection
     * @return
     */
    public static int[] buildWipeMap(int picWidthInMbs, int picHeightInMbs, int sizeOfUpperLeftGroup,
            boolean changeDirection) {

        int picSizeInMbs = picWidthInMbs * picHeightInMbs;
        int[] groups = new int[picSizeInMbs];
        int changeDirectionInt = changeDirection ? 1 : 0;

        int k = 0;
        for (int j = 0; j < picWidthInMbs; j++) {
            for (int i = 0; i < picHeightInMbs; i++) {
                int mbAddr = i * picWidthInMbs + j;
                if (k++ < sizeOfUpperLeftGroup) {
                    groups[mbAddr] = changeDirectionInt;
                } else {
                    groups[mbAddr] = 1 - changeDirectionInt;
                }
            }
        }

        return groups;
    }
}
