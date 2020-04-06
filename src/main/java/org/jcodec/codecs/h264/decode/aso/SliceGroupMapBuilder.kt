package org.jcodec.codecs.h264.decode.aso

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A helper class that builds macroblock to slice group maps needed by ASO
 * (Arbitrary Slice Order)
 *
 * @author The JCodec project
 */
object SliceGroupMapBuilder {
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
    fun buildInterleavedMap(picWidthInMbs: Int, picHeightInMbs: Int, runLength: IntArray): IntArray {
        val numSliceGroups = runLength.size
        val picSizeInMbs = picWidthInMbs * picHeightInMbs
        val groups = IntArray(picSizeInMbs)
        var i = 0
        do {
            var iGroup = 0
            while (iGroup < numSliceGroups && i < picSizeInMbs) {
                var j = 0
                while (j < runLength[iGroup] && i + j < picSizeInMbs) {
                    groups[i + j] = iGroup
                    j++
                }
                i += runLength[iGroup++]
            }
        } while (i < picSizeInMbs)
        return groups
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
    fun buildDispersedMap(picWidthInMbs: Int, picHeightInMbs: Int, numSliceGroups: Int): IntArray {
        val picSizeInMbs = picWidthInMbs * picHeightInMbs
        val groups = IntArray(picSizeInMbs)
        for (i in 0 until picSizeInMbs) {
            val group = (i % picWidthInMbs + i / picWidthInMbs * numSliceGroups / 2) % numSliceGroups
            groups[i] = group
        }
        return groups
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
     * Total number of slice groups
     * @param topLeftAddr
     * Addresses of macroblocks that are top-left corners of
     * respective slice groups
     * @param bottomRightAddr
     * Addresses macroblocks that are bottom-right corners of
     * respective slice groups
     * @return
     */
    fun buildForegroundMap(picWidthInMbs: Int, picHeightInMbs: Int, numSliceGroups: Int,
                           topLeftAddr: IntArray, bottomRightAddr: IntArray): IntArray {
        val picSizeInMbs = picWidthInMbs * picHeightInMbs
        val groups = IntArray(picSizeInMbs)
        for (i in 0 until picSizeInMbs) groups[i] = numSliceGroups - 1
        var tot = 0
        for (iGroup in numSliceGroups - 2 downTo 0) {
            val yTopLeft = topLeftAddr[iGroup] / picWidthInMbs
            val xTopLeft = topLeftAddr[iGroup] % picWidthInMbs
            val yBottomRight = bottomRightAddr[iGroup] / picWidthInMbs
            val xBottomRight = bottomRightAddr[iGroup] % picWidthInMbs
            val sz = (yBottomRight - yTopLeft + 1) * (xBottomRight - xTopLeft + 1)
            tot += sz
            val ind = 0
            for (y in yTopLeft..yBottomRight) for (x in xTopLeft..xBottomRight) {
                val mbAddr = y * picWidthInMbs + x
                groups[mbAddr] = iGroup
            }
        }
        return groups
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
     * number of macroblocks in slice group 0
     * @return
     */
    fun buildBoxOutMap(picWidthInMbs: Int, picHeightInMbs: Int, changeDirection: Boolean,
                       numberOfMbsInBox: Int): IntArray {
        val picSizeInMbs = picWidthInMbs * picHeightInMbs
        val groups = IntArray(picSizeInMbs)
        val changeDirectionInt = if (changeDirection) 1 else 0
        for (i in 0 until picSizeInMbs) groups[i] = 1
        var x = (picWidthInMbs - changeDirectionInt) / 2
        var y = (picHeightInMbs - changeDirectionInt) / 2
        var leftBound = x
        var topBound = y
        var rightBound = x
        var bottomBound = y
        var xDir = changeDirectionInt - 1
        var yDir = changeDirectionInt
        var mapUnitVacant = false
        var k = 0
        while (k < numberOfMbsInBox) {
            val mbAddr = y * picWidthInMbs + x
            mapUnitVacant = groups[mbAddr] == 1
            if (mapUnitVacant) {
                groups[mbAddr] = 0
            }
            if (xDir == -1 && x == leftBound) {
                leftBound = Max(leftBound - 1, 0)
                x = leftBound
                xDir = 0
                yDir = 2 * changeDirectionInt - 1
            } else if (xDir == 1 && x == rightBound) {
                rightBound = Min(rightBound + 1, picWidthInMbs - 1)
                x = rightBound
                xDir = 0
                yDir = 1 - 2 * changeDirectionInt
            } else if (yDir == -1 && y == topBound) {
                topBound = Max(topBound - 1, 0)
                y = topBound
                xDir = 1 - 2 * changeDirectionInt
                yDir = 0
            } else if (yDir == 1 && y == bottomBound) {
                bottomBound = Min(bottomBound + 1, picHeightInMbs - 1)
                y = bottomBound
                xDir = 2 * changeDirectionInt - 1
                yDir = 0
            } else {
                x += xDir
                y += yDir
            }
            k += if (mapUnitVacant) 1 else 0
        }
        return groups
    }

    private fun Min(i: Int, j: Int): Int {
        return if (i < j) i else j
    }

    private fun Max(i: Int, j: Int): Int {
        return if (i > j) i else j
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
    fun buildRasterScanMap(picWidthInMbs: Int, picHeightInMbs: Int, sizeOfUpperLeftGroup: Int,
                           changeDirection: Boolean): IntArray {
        val picSizeInMbs = picWidthInMbs * picHeightInMbs
        val groups = IntArray(picSizeInMbs)
        val changeDirectionInt = if (changeDirection) 1 else 0
        var i: Int
        i = 0
        while (i < sizeOfUpperLeftGroup) {
            groups[i] = changeDirectionInt
            i++
        }
        while (i < picSizeInMbs) {
            groups[i] = 1 - changeDirectionInt
            i++
        }
        return groups
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
    fun buildWipeMap(picWidthInMbs: Int, picHeightInMbs: Int, sizeOfUpperLeftGroup: Int,
                     changeDirection: Boolean): IntArray {
        val picSizeInMbs = picWidthInMbs * picHeightInMbs
        val groups = IntArray(picSizeInMbs)
        val changeDirectionInt = if (changeDirection) 1 else 0
        var k = 0
        for (j in 0 until picWidthInMbs) {
            for (i in 0 until picHeightInMbs) {
                val mbAddr = i * picWidthInMbs + j
                if (k++ < sizeOfUpperLeftGroup) {
                    groups[mbAddr] = changeDirectionInt
                } else {
                    groups[mbAddr] = 1 - changeDirectionInt
                }
            }
        }
        return groups
    }
}