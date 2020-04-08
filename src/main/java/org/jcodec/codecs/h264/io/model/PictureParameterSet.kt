package org.jcodec.codecs.h264.io.model

import org.jcodec.codecs.h264.decode.CAVLCReader
import org.jcodec.codecs.h264.io.write.CAVLCWriter
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.BitWriter
import org.jcodec.platform.Platform
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Picture Parameter Set entity of H264 bitstream
 *
 * capable to serialize / deserialize with CAVLC bitstream
 *
 * @author The JCodec project
 */
class PictureParameterSet {
    class PPSExt {
        // transform_8x8_mode_flag
        @JvmField
        var isTransform8x8ModeFlag = false
        @JvmField
        var scalingMatrix: Array<IntArray?>? = null

        // second_chroma_qp_index_offset
        @JvmField
        var secondChromaQpIndexOffset = 0

    }

    // entropy_coding_mode_flag
    @JvmField
    var isEntropyCodingModeFlag = false

    // num_ref_idx_active_minus1
    @JvmField
    var numRefIdxActiveMinus1: IntArray

    // slice_group_change_rate_minus1
    @JvmField
    var sliceGroupChangeRateMinus1 = 0

    // pic_parameter_set_id
    @JvmField
    var picParameterSetId = 0

    // seq_parameter_set_id
    @JvmField
    var seqParameterSetId = 0

    // pic_order_present_flag
    @JvmField
    var isPicOrderPresentFlag = false

    // num_slice_groups_minus1
    @JvmField
    var numSliceGroupsMinus1 = 0

    // slice_group_map_type
    @JvmField
    var sliceGroupMapType = 0

    // weighted_pred_flag
    @JvmField
    var isWeightedPredFlag = false

    // weighted_bipred_idc
    @JvmField
    var weightedBipredIdc = 0

    // pic_init_qp_minus26
    @JvmField
    var picInitQpMinus26 = 0

    // pic_init_qs_minus26
    @JvmField
    var picInitQsMinus26 = 0

    // chroma_qp_index_offset
    @JvmField
    var chromaQpIndexOffset = 0

    // deblocking_filter_control_present_flag
    @JvmField
    var isDeblockingFilterControlPresentFlag = false

    // constrained_intra_pred_flag
    @JvmField
    var isConstrainedIntraPredFlag = false

    // redundant_pic_cnt_present_flag
    @JvmField
    var isRedundantPicCntPresentFlag = false

    // top_left
    @JvmField
    var topLeft: IntArray? = null

    // bottom_right
    @JvmField
    var bottomRight: IntArray? = null

    // run_length_minus1
    @JvmField
    var runLengthMinus1: IntArray? = null

    // slice_group_change_direction_flag
    @JvmField
    var isSliceGroupChangeDirectionFlag = false

    // slice_group_id
    @JvmField
    var sliceGroupId: IntArray? = null
    @JvmField
    var extended: PPSExt? = null
    fun write(out: ByteBuffer) {
        val writer = BitWriter(out)
        CAVLCWriter.writeUEtrace(writer, picParameterSetId, "PPS: pic_parameter_set_id")
        CAVLCWriter.writeUEtrace(writer, seqParameterSetId, "PPS: seq_parameter_set_id")
        CAVLCWriter.writeBool(writer, isEntropyCodingModeFlag, "PPS: entropy_coding_mode_flag")
        CAVLCWriter.writeBool(writer, isPicOrderPresentFlag, "PPS: pic_order_present_flag")
        CAVLCWriter.writeUEtrace(writer, numSliceGroupsMinus1, "PPS: num_slice_groups_minus1")
        if (numSliceGroupsMinus1 > 0) {
            CAVLCWriter.writeUEtrace(writer, sliceGroupMapType, "PPS: slice_group_map_type")
            if (sliceGroupMapType == 0) {
                for (iGroup in 0..numSliceGroupsMinus1) {
                    CAVLCWriter.writeUEtrace(writer, runLengthMinus1!![iGroup], "PPS: ")
                }
            } else if (sliceGroupMapType == 2) {
                for (iGroup in 0 until numSliceGroupsMinus1) {
                    CAVLCWriter.writeUEtrace(writer, topLeft!![iGroup], "PPS: ")
                    CAVLCWriter.writeUEtrace(writer, bottomRight!![iGroup], "PPS: ")
                }
            } else if (sliceGroupMapType == 3 || sliceGroupMapType == 4 || sliceGroupMapType == 5) {
                CAVLCWriter.writeBool(writer, isSliceGroupChangeDirectionFlag, "PPS: slice_group_change_direction_flag")
                CAVLCWriter.writeUEtrace(writer, sliceGroupChangeRateMinus1, "PPS: slice_group_change_rate_minus1")
            } else if (sliceGroupMapType == 6) {
                val NumberBitsPerSliceGroupId: Int
                NumberBitsPerSliceGroupId = if (numSliceGroupsMinus1 + 1 > 4) 3 else if (numSliceGroupsMinus1 + 1 > 2) 2 else 1
                CAVLCWriter.writeUEtrace(writer, sliceGroupId!!.size, "PPS: ")
                for (i in 0..sliceGroupId!!.size) {
                    CAVLCWriter.writeU(writer, sliceGroupId!![i], NumberBitsPerSliceGroupId)
                }
            }
        }
        CAVLCWriter.writeUEtrace(writer, numRefIdxActiveMinus1[0], "PPS: num_ref_idx_l0_active_minus1")
        CAVLCWriter.writeUEtrace(writer, numRefIdxActiveMinus1[1], "PPS: num_ref_idx_l1_active_minus1")
        CAVLCWriter.writeBool(writer, isWeightedPredFlag, "PPS: weighted_pred_flag")
        CAVLCWriter.writeNBit(writer, weightedBipredIdc.toLong(), 2, "PPS: weighted_bipred_idc")
        CAVLCWriter.writeSEtrace(writer, picInitQpMinus26, "PPS: pic_init_qp_minus26")
        CAVLCWriter.writeSEtrace(writer, picInitQsMinus26, "PPS: pic_init_qs_minus26")
        CAVLCWriter.writeSEtrace(writer, chromaQpIndexOffset, "PPS: chroma_qp_index_offset")
        CAVLCWriter.writeBool(writer, isDeblockingFilterControlPresentFlag, "PPS: deblocking_filter_control_present_flag")
        CAVLCWriter.writeBool(writer, isConstrainedIntraPredFlag, "PPS: constrained_intra_pred_flag")
        CAVLCWriter.writeBool(writer, isRedundantPicCntPresentFlag, "PPS: redundant_pic_cnt_present_flag")
        if (extended != null) {
            CAVLCWriter.writeBool(writer, extended!!.isTransform8x8ModeFlag, "PPS: transform_8x8_mode_flag")
            CAVLCWriter.writeBool(writer, extended!!.scalingMatrix != null, "PPS: scalindMatrix")
            if (extended!!.scalingMatrix != null) {
                for (i in 0 until 6 + 2 * if (extended!!.isTransform8x8ModeFlag) 1 else 0) {
                    CAVLCWriter.writeBool(writer, extended!!.scalingMatrix!![i] != null, "PPS: ")
                    if (extended!!.scalingMatrix!![i] != null) {
                        SeqParameterSet.writeScalingList(writer, extended!!.scalingMatrix!!, i)
                    }
                }
            }
            CAVLCWriter.writeSEtrace(writer, extended!!.secondChromaQpIndexOffset, "PPS: ")
        }
        CAVLCWriter.writeTrailingBits(writer)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + Arrays.hashCode(bottomRight)
        result = prime * result + chromaQpIndexOffset
        result = prime * result + if (isConstrainedIntraPredFlag) 1231 else 1237
        result = prime * result + if (isDeblockingFilterControlPresentFlag) 1231 else 1237
        result = prime * result + if (isEntropyCodingModeFlag) 1231 else 1237
        result = prime * result + if (extended == null) 0 else extended.hashCode()
        result = prime * result + numRefIdxActiveMinus1[0]
        result = prime * result + numRefIdxActiveMinus1[1]
        result = prime * result + numSliceGroupsMinus1
        result = prime * result + picInitQpMinus26
        result = prime * result + picInitQsMinus26
        result = prime * result + if (isPicOrderPresentFlag) 1231 else 1237
        result = prime * result + picParameterSetId
        result = prime * result + if (isRedundantPicCntPresentFlag) 1231 else 1237
        result = prime * result + Arrays.hashCode(runLengthMinus1)
        result = prime * result + seqParameterSetId
        result = prime * result + if (isSliceGroupChangeDirectionFlag) 1231 else 1237
        result = prime * result + sliceGroupChangeRateMinus1
        result = prime * result + Arrays.hashCode(sliceGroupId)
        result = prime * result + sliceGroupMapType
        result = prime * result + Arrays.hashCode(topLeft)
        result = prime * result + weightedBipredIdc
        result = prime * result + if (isWeightedPredFlag) 1231 else 1237
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as PictureParameterSet
        if (!Platform.arrayEqualsInt(bottomRight, other.bottomRight)) return false
        if (chromaQpIndexOffset != other.chromaQpIndexOffset) return false
        if (isConstrainedIntraPredFlag != other.isConstrainedIntraPredFlag) return false
        if (isDeblockingFilterControlPresentFlag != other.isDeblockingFilterControlPresentFlag) return false
        if (isEntropyCodingModeFlag != other.isEntropyCodingModeFlag) return false
        if (extended == null) {
            if (other.extended != null) return false
        } else if (extended != other.extended) return false
        if (numRefIdxActiveMinus1[0] != other.numRefIdxActiveMinus1[0]) return false
        if (numRefIdxActiveMinus1[1] != other.numRefIdxActiveMinus1[1]) return false
        if (numSliceGroupsMinus1 != other.numSliceGroupsMinus1) return false
        if (picInitQpMinus26 != other.picInitQpMinus26) return false
        if (picInitQsMinus26 != other.picInitQsMinus26) return false
        if (isPicOrderPresentFlag != other.isPicOrderPresentFlag) return false
        if (picParameterSetId != other.picParameterSetId) return false
        if (isRedundantPicCntPresentFlag != other.isRedundantPicCntPresentFlag) return false
        if (!Platform.arrayEqualsInt(runLengthMinus1, other.runLengthMinus1)) return false
        if (seqParameterSetId != other.seqParameterSetId) return false
        if (isSliceGroupChangeDirectionFlag != other.isSliceGroupChangeDirectionFlag) return false
        if (sliceGroupChangeRateMinus1 != other.sliceGroupChangeRateMinus1) return false
        if (!Platform.arrayEqualsInt(sliceGroupId, other.sliceGroupId)) return false
        if (sliceGroupMapType != other.sliceGroupMapType) return false
        if (!Platform.arrayEqualsInt(topLeft, other.topLeft)) return false
        if (weightedBipredIdc != other.weightedBipredIdc) return false
        return if (isWeightedPredFlag != other.isWeightedPredFlag) false else true
    }

    fun copy(): PictureParameterSet {
        val buf = ByteBuffer.allocate(2048)
        write(buf)
        buf.flip()
        return read(buf)
    }

    companion object {
        @JvmStatic
        fun read(`is`: ByteBuffer): PictureParameterSet {
            val _in = BitReader.createBitReader(`is`)
            val pps = PictureParameterSet()
            pps.picParameterSetId = CAVLCReader.readUEtrace(_in, "PPS: pic_parameter_set_id")
            pps.seqParameterSetId = CAVLCReader.readUEtrace(_in, "PPS: seq_parameter_set_id")
            pps.isEntropyCodingModeFlag = CAVLCReader.readBool(_in, "PPS: entropy_coding_mode_flag")
            pps.isPicOrderPresentFlag = CAVLCReader.readBool(_in, "PPS: pic_order_present_flag")
            pps.numSliceGroupsMinus1 = CAVLCReader.readUEtrace(_in, "PPS: num_slice_groups_minus1")
            if (pps.numSliceGroupsMinus1 > 0) {
                pps.sliceGroupMapType = CAVLCReader.readUEtrace(_in, "PPS: slice_group_map_type")
                pps.topLeft = IntArray(pps.numSliceGroupsMinus1 + 1)
                pps.bottomRight = IntArray(pps.numSliceGroupsMinus1 + 1)
                pps.runLengthMinus1 = IntArray(pps.numSliceGroupsMinus1 + 1)
                if (pps.sliceGroupMapType == 0) for (iGroup in 0..pps.numSliceGroupsMinus1) pps.runLengthMinus1!![iGroup] = CAVLCReader.readUEtrace(_in, "PPS: run_length_minus1") else if (pps.sliceGroupMapType == 2) for (iGroup in 0 until pps.numSliceGroupsMinus1) {
                    pps.topLeft!![iGroup] = CAVLCReader.readUEtrace(_in, "PPS: top_left")
                    pps.bottomRight!![iGroup] = CAVLCReader.readUEtrace(_in, "PPS: bottom_right")
                } else if (pps.sliceGroupMapType == 3 || pps.sliceGroupMapType == 4 || pps.sliceGroupMapType == 5) {
                    pps.isSliceGroupChangeDirectionFlag = CAVLCReader.readBool(_in, "PPS: slice_group_change_direction_flag")
                    pps.sliceGroupChangeRateMinus1 = CAVLCReader.readUEtrace(_in, "PPS: slice_group_change_rate_minus1")
                } else if (pps.sliceGroupMapType == 6) {
                    val NumberBitsPerSliceGroupId: Int
                    NumberBitsPerSliceGroupId = if (pps.numSliceGroupsMinus1 + 1 > 4) 3 else if (pps.numSliceGroupsMinus1 + 1 > 2) 2 else 1
                    val pic_size_in_map_units_minus1 = CAVLCReader.readUEtrace(_in, "PPS: pic_size_in_map_units_minus1")
                    pps.sliceGroupId = IntArray(pic_size_in_map_units_minus1 + 1)
                    for (i in 0..pic_size_in_map_units_minus1) {
                        pps.sliceGroupId!![i] = CAVLCReader.readU(_in, NumberBitsPerSliceGroupId, "PPS: slice_group_id [$i]f")
                    }
                }
            }
            pps.numRefIdxActiveMinus1 = intArrayOf(CAVLCReader.readUEtrace(_in, "PPS: num_ref_idx_l0_active_minus1"), CAVLCReader.readUEtrace(_in, "PPS: num_ref_idx_l1_active_minus1"))
            pps.isWeightedPredFlag = CAVLCReader.readBool(_in, "PPS: weighted_pred_flag")
            pps.weightedBipredIdc = CAVLCReader.readNBit(_in, 2, "PPS: weighted_bipred_idc")
            pps.picInitQpMinus26 = CAVLCReader.readSE(_in, "PPS: pic_init_qp_minus26")
            pps.picInitQsMinus26 = CAVLCReader.readSE(_in, "PPS: pic_init_qs_minus26")
            pps.chromaQpIndexOffset = CAVLCReader.readSE(_in, "PPS: chroma_qp_index_offset")
            pps.isDeblockingFilterControlPresentFlag = CAVLCReader.readBool(_in, "PPS: deblocking_filter_control_present_flag")
            pps.isConstrainedIntraPredFlag = CAVLCReader.readBool(_in, "PPS: constrained_intra_pred_flag")
            pps.isRedundantPicCntPresentFlag = CAVLCReader.readBool(_in, "PPS: redundant_pic_cnt_present_flag")
            if (CAVLCReader.moreRBSPData(_in)) {
                pps.extended = PPSExt()
                pps.extended!!.isTransform8x8ModeFlag = CAVLCReader.readBool(_in, "PPS: transform_8x8_mode_flag")
                val pic_scaling_matrix_present_flag = CAVLCReader.readBool(_in, "PPS: pic_scaling_matrix_present_flag")
                if (pic_scaling_matrix_present_flag) {
                    pps.extended!!.scalingMatrix = arrayOfNulls(8)
                    for (i in 0 until 6 + 2 * if (pps.extended!!.isTransform8x8ModeFlag) 1 else 0) {
                        val scalingListSize = if (i < 6) 16 else 64
                        if (CAVLCReader.readBool(_in, "PPS: pic_scaling_list_present_flag")) {
                            pps.extended!!.scalingMatrix!![i] = SeqParameterSet.readScalingList(_in, scalingListSize)
                        }
                    }
                }
                pps.extended!!.secondChromaQpIndexOffset = CAVLCReader.readSE(_in, "PPS: second_chroma_qp_index_offset")
            }
            return pps
        }
    }

    init {
        numRefIdxActiveMinus1 = IntArray(2)
    }
}