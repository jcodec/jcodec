package org.jcodec.codecs.h264.decode.aso

import org.jcodec.codecs.h264.io.model.PictureParameterSet
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.codecs.h264.io.model.SeqParameterSet.Companion.getPicHeightInMbs
import org.jcodec.codecs.h264.io.model.SliceHeader

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MapManager(private val sps: SeqParameterSet, private val pps: PictureParameterSet) {
    private var mbToSliceGroupMap: MBToSliceGroupMap?
    private var prevSliceGroupChangeCycle = 0
    private fun buildMap(sps: SeqParameterSet, pps: PictureParameterSet): MBToSliceGroupMap? {
        val numGroups = pps.numSliceGroupsMinus1 + 1
        if (numGroups > 1) {
            val map: IntArray?
            val picWidthInMbs = sps.picWidthInMbsMinus1 + 1
            val picHeightInMbs = getPicHeightInMbs(sps)
            if (pps.sliceGroupMapType == 0) {
                val runLength = IntArray(numGroups)
                for (i in 0 until numGroups) {
                    runLength[i] = pps.runLengthMinus1!![i] + 1
                }
                map = SliceGroupMapBuilder.buildInterleavedMap(picWidthInMbs, picHeightInMbs, runLength)
            } else if (pps.sliceGroupMapType == 1) {
                map = SliceGroupMapBuilder.buildDispersedMap(picWidthInMbs, picHeightInMbs, numGroups)
            } else if (pps.sliceGroupMapType == 2) {
                map = SliceGroupMapBuilder.buildForegroundMap(picWidthInMbs, picHeightInMbs, numGroups, pps.topLeft!!,
                        pps.bottomRight!!)
            } else if (pps.sliceGroupMapType >= 3 && pps.sliceGroupMapType <= 5) {
                return null
            } else if (pps.sliceGroupMapType == 6) {
                map = pps.sliceGroupId
            } else {
                throw RuntimeException("Unsupported slice group map type")
            }
            return buildMapIndices(map, numGroups)
        }
        return null
    }

    private fun buildMapIndices(map: IntArray?, numGroups: Int): MBToSliceGroupMap {
        var ind = IntArray(numGroups)
        val indices = IntArray(map!!.size)
        for (i in map.indices) {
            indices[i] = ind[map[i]]++
        }
        val inverse = arrayOfNulls<IntArray>(numGroups)
        for (i in 0 until numGroups) {
            inverse[i] = IntArray(ind[i])
        }
        ind = IntArray(numGroups)
        for (i in map.indices) {
            val sliceGroup = map[i]
            inverse[sliceGroup]!![ind[sliceGroup]++] = i
        }
        return MBToSliceGroupMap(map, indices, inverse)
    }

    private fun updateMap(sh: SliceHeader) {
        val mapType = pps.sliceGroupMapType
        val numGroups = pps.numSliceGroupsMinus1 + 1
        if (numGroups > 1 && mapType >= 3 && mapType <= 5 && (sh.sliceGroupChangeCycle != prevSliceGroupChangeCycle || mbToSliceGroupMap == null)) {
            prevSliceGroupChangeCycle = sh.sliceGroupChangeCycle
            val picWidthInMbs = sps.picWidthInMbsMinus1 + 1
            val picHeightInMbs = getPicHeightInMbs(sps)
            val picSizeInMapUnits = picWidthInMbs * picHeightInMbs
            var mapUnitsInSliceGroup0 = sh.sliceGroupChangeCycle * (pps.sliceGroupChangeRateMinus1 + 1)
            mapUnitsInSliceGroup0 = if (mapUnitsInSliceGroup0 > picSizeInMapUnits) picSizeInMapUnits else mapUnitsInSliceGroup0
            val sizeOfUpperLeftGroup = if (pps.isSliceGroupChangeDirectionFlag) picSizeInMapUnits - mapUnitsInSliceGroup0 else mapUnitsInSliceGroup0
            val map: IntArray
            map = if (mapType == 3) {
                SliceGroupMapBuilder.buildBoxOutMap(picWidthInMbs, picHeightInMbs,
                        pps.isSliceGroupChangeDirectionFlag, mapUnitsInSliceGroup0)
            } else if (mapType == 4) {
                SliceGroupMapBuilder.buildRasterScanMap(picWidthInMbs, picHeightInMbs, sizeOfUpperLeftGroup,
                        pps.isSliceGroupChangeDirectionFlag)
            } else {
                SliceGroupMapBuilder.buildWipeMap(picWidthInMbs, picHeightInMbs, sizeOfUpperLeftGroup,
                        pps.isSliceGroupChangeDirectionFlag)
            }
            mbToSliceGroupMap = buildMapIndices(map, numGroups)
        }
    }

    fun getMapper(sh: SliceHeader): Mapper {
        updateMap(sh)
        val firstMBInSlice = sh.firstMbInSlice
        return if (pps.numSliceGroupsMinus1 > 0) {
            PrebuiltMBlockMapper(mbToSliceGroupMap!!, firstMBInSlice, sps.picWidthInMbsMinus1 + 1)
        } else {
            FlatMBlockMapper(sps.picWidthInMbsMinus1 + 1, firstMBInSlice)
        }
    }

    init {
        mbToSliceGroupMap = buildMap(sps, pps)
    }
}