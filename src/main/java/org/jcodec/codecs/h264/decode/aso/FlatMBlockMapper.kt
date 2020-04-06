package org.jcodec.codecs.h264.decode.aso

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A block map that that maps macroblocks sequentially in scan order
 *
 * @author The JCodec project
 */
class FlatMBlockMapper(private val frameWidthInMbs: Int, private val firstMBAddr: Int) : Mapper {
    override fun leftAvailable(index: Int): Boolean {
        val mbAddr = index + firstMBAddr
        val atTheBorder = mbAddr % frameWidthInMbs == 0
        return !atTheBorder && mbAddr > firstMBAddr
    }

    override fun topAvailable(index: Int): Boolean {
        val mbAddr = index + firstMBAddr
        return mbAddr - frameWidthInMbs >= firstMBAddr
    }

    override fun getAddress(index: Int): Int {
        return firstMBAddr + index
    }

    override fun getMbX(index: Int): Int {
        return getAddress(index) % frameWidthInMbs
    }

    override fun getMbY(index: Int): Int {
        return getAddress(index) / frameWidthInMbs
    }

    override fun topRightAvailable(index: Int): Boolean {
        val mbAddr = index + firstMBAddr
        val atTheBorder = (mbAddr + 1) % frameWidthInMbs == 0
        return !atTheBorder && mbAddr - frameWidthInMbs + 1 >= firstMBAddr
    }

    override fun topLeftAvailable(index: Int): Boolean {
        val mbAddr = index + firstMBAddr
        val atTheBorder = mbAddr % frameWidthInMbs == 0
        return !atTheBorder && mbAddr - frameWidthInMbs - 1 >= firstMBAddr
    }

}