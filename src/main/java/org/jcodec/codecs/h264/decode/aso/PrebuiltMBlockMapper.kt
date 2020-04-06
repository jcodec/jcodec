package org.jcodec.codecs.h264.decode.aso

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A macrboblock to slice group mapper that operates on prebuilt map, passed to
 * it in the constructor
 *
 * @author The JCodec project
 */
class PrebuiltMBlockMapper(private val map: MBToSliceGroupMap, private val firstMBInSlice: Int, picWidthInMbs: Int) : Mapper {
    private val groupId: Int
    private val picWidthInMbs: Int
    private val indexOfFirstMb: Int
    override fun getAddress(mbIndex: Int): Int {
        return map.inverse[groupId]!![mbIndex + indexOfFirstMb]
    }

    override fun leftAvailable(mbIndex: Int): Boolean {
        val mbAddr = map.inverse[groupId]!![mbIndex + indexOfFirstMb]
        val leftMBAddr = mbAddr - 1
        return !(leftMBAddr < firstMBInSlice || mbAddr % picWidthInMbs == 0 || map.groups[leftMBAddr] != groupId)
    }

    override fun topAvailable(mbIndex: Int): Boolean {
        val mbAddr = map.inverse[groupId]!![mbIndex + indexOfFirstMb]
        val topMBAddr = mbAddr - picWidthInMbs
        return !(topMBAddr < firstMBInSlice || map.groups[topMBAddr] != groupId)
    }

    override fun getMbX(index: Int): Int {
        return getAddress(index) % picWidthInMbs
    }

    override fun getMbY(index: Int): Int {
        return getAddress(index) / picWidthInMbs
    }

    override fun topRightAvailable(mbIndex: Int): Boolean {
        val mbAddr = map.inverse[groupId]!![mbIndex + indexOfFirstMb]
        val topRMBAddr = mbAddr - picWidthInMbs + 1
        return !(topRMBAddr < firstMBInSlice || (mbAddr + 1) % picWidthInMbs == 0 || map.groups[topRMBAddr] != groupId)
    }

    override fun topLeftAvailable(mbIndex: Int): Boolean {
        val mbAddr = map.inverse[groupId]!![mbIndex + indexOfFirstMb]
        val topLMBAddr = mbAddr - picWidthInMbs - 1
        return !(topLMBAddr < firstMBInSlice || mbAddr % picWidthInMbs == 0 || map.groups[topLMBAddr] != groupId)
    }

    init {
        groupId = map.groups[firstMBInSlice]
        this.picWidthInMbs = picWidthInMbs
        indexOfFirstMb = map.indices[firstMBInSlice]
    }
}