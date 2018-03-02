package org.jcodec.codecs.h264.io.model;
import static org.jcodec.codecs.h264.decode.CAVLCReader.moreRBSPData;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readBool;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readNBit;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readSE;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readU;
import static org.jcodec.codecs.h264.decode.CAVLCReader.readUEtrace;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeBool;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeNBit;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeSEtrace;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeTrailingBits;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeU;
import static org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUEtrace;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.platform.Platform;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Picture Parameter Set entity of H264 bitstream
 * 
 * capable to serialize / deserialize with CAVLC bitstream
 * 
 * @author The JCodec project
 * 
 */
public class PictureParameterSet {

    public static class PPSExt {
        public boolean transform8x8ModeFlag;
        public int[][] scalingMatrix;
        public int secondChromaQpIndexOffset;

        public boolean isTransform8x8ModeFlag() {
            // transform_8x8_mode_flag
            return transform8x8ModeFlag;
        }

        public int[][] getScalingMatrix() {
            return scalingMatrix;
        }

        public int getSecondChromaQpIndexOffset() {
            // second_chroma_qp_index_offset
            return secondChromaQpIndexOffset;
        }
    }

    // entropy_coding_mode_flag
    public boolean entropyCodingModeFlag;
    // num_ref_idx_active_minus1
    public int[] numRefIdxActiveMinus1;
    // slice_group_change_rate_minus1
    public int sliceGroupChangeRateMinus1;
    // pic_parameter_set_id
    public int picParameterSetId;
    // seq_parameter_set_id
    public int seqParameterSetId;
    // pic_order_present_flag
    public boolean picOrderPresentFlag;
    // num_slice_groups_minus1
    public int numSliceGroupsMinus1;
    // slice_group_map_type
    public int sliceGroupMapType;
    // weighted_pred_flag
    public boolean weightedPredFlag;
    // weighted_bipred_idc
    public int weightedBipredIdc;
    // pic_init_qp_minus26
    public int picInitQpMinus26;
    // pic_init_qs_minus26
    public int picInitQsMinus26;
    // chroma_qp_index_offset
    public int chromaQpIndexOffset;
    // deblocking_filter_control_present_flag
    public boolean deblockingFilterControlPresentFlag;
    // constrained_intra_pred_flag
    public boolean constrainedIntraPredFlag;
    // redundant_pic_cnt_present_flag
    public boolean redundantPicCntPresentFlag;
    // top_left
    public int[] topLeft;
    // bottom_right
    public int[] bottomRight;
    // run_length_minus1
    public int[] runLengthMinus1;
    // slice_group_change_direction_flag
    public boolean sliceGroupChangeDirectionFlag;
    // slice_group_id
    public int[] sliceGroupId;
    public PPSExt extended;
    
    public PictureParameterSet() {
        this.numRefIdxActiveMinus1 = new int[2];
    }
    
    public static PictureParameterSet read(ByteBuffer is) {
        BitReader _in = BitReader.createBitReader(is);
        PictureParameterSet pps = new PictureParameterSet();

        pps.picParameterSetId = readUEtrace(_in, "PPS: pic_parameter_set_id");
        pps.seqParameterSetId = readUEtrace(_in, "PPS: seq_parameter_set_id");
        pps.entropyCodingModeFlag = readBool(_in, "PPS: entropy_coding_mode_flag");
        pps.picOrderPresentFlag = readBool(_in, "PPS: pic_order_present_flag");
        pps.numSliceGroupsMinus1 = readUEtrace(_in, "PPS: num_slice_groups_minus1");
        if (pps.numSliceGroupsMinus1 > 0) {
            pps.sliceGroupMapType = readUEtrace(_in, "PPS: slice_group_map_type");
            pps.topLeft = new int[pps.numSliceGroupsMinus1 + 1];
            pps.bottomRight = new int[pps.numSliceGroupsMinus1 + 1];
            pps.runLengthMinus1 = new int[pps.numSliceGroupsMinus1 + 1];
            if (pps.sliceGroupMapType == 0)
                for (int iGroup = 0; iGroup <= pps.numSliceGroupsMinus1; iGroup++)
                    pps.runLengthMinus1[iGroup] = readUEtrace(_in, "PPS: run_length_minus1");
            else if (pps.sliceGroupMapType == 2)
                for (int iGroup = 0; iGroup < pps.numSliceGroupsMinus1; iGroup++) {
                    pps.topLeft[iGroup] = readUEtrace(_in, "PPS: top_left");
                    pps.bottomRight[iGroup] = readUEtrace(_in, "PPS: bottom_right");
                }
            else if (pps.sliceGroupMapType == 3 || pps.sliceGroupMapType == 4 || pps.sliceGroupMapType == 5) {
                pps.sliceGroupChangeDirectionFlag = readBool(_in, "PPS: slice_group_change_direction_flag");
                pps.sliceGroupChangeRateMinus1 = readUEtrace(_in, "PPS: slice_group_change_rate_minus1");
            } else if (pps.sliceGroupMapType == 6) {
                int NumberBitsPerSliceGroupId;
                if (pps.numSliceGroupsMinus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (pps.numSliceGroupsMinus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                int pic_size_in_map_units_minus1 = readUEtrace(_in, "PPS: pic_size_in_map_units_minus1");
                pps.sliceGroupId = new int[pic_size_in_map_units_minus1 + 1];
                for (int i = 0; i <= pic_size_in_map_units_minus1; i++) {
                    pps.sliceGroupId[i] = readU(_in, NumberBitsPerSliceGroupId, "PPS: slice_group_id [" + i + "]f");
                }
            }
        }
        pps.numRefIdxActiveMinus1 = new int[] {readUEtrace(_in, "PPS: num_ref_idx_l0_active_minus1"), readUEtrace(_in, "PPS: num_ref_idx_l1_active_minus1")};
        pps.weightedPredFlag = readBool(_in, "PPS: weighted_pred_flag");
        pps.weightedBipredIdc = readNBit(_in, 2, "PPS: weighted_bipred_idc");
        pps.picInitQpMinus26 = readSE(_in, "PPS: pic_init_qp_minus26");
        pps.picInitQsMinus26 = readSE(_in, "PPS: pic_init_qs_minus26");
        pps.chromaQpIndexOffset = readSE(_in, "PPS: chroma_qp_index_offset");
        pps.deblockingFilterControlPresentFlag = readBool(_in, "PPS: deblocking_filter_control_present_flag");
        pps.constrainedIntraPredFlag = readBool(_in, "PPS: constrained_intra_pred_flag");
        pps.redundantPicCntPresentFlag = readBool(_in, "PPS: redundant_pic_cnt_present_flag");
        if (moreRBSPData(_in)) {
            pps.extended = new PictureParameterSet.PPSExt();
            pps.extended.transform8x8ModeFlag = readBool(_in, "PPS: transform_8x8_mode_flag");
            boolean pic_scaling_matrix_present_flag = readBool(_in, "PPS: pic_scaling_matrix_present_flag");
            if (pic_scaling_matrix_present_flag) {
                pps.extended.scalingMatrix = new int[8][];
                for (int i = 0; i < 6 + 2 * (pps.extended.transform8x8ModeFlag ? 1 : 0); i++) {
                    int scalingListSize = i < 6 ? 16 : 64;
                    if (readBool(_in, "PPS: pic_scaling_list_present_flag")) {
                        pps.extended.scalingMatrix[i] = SeqParameterSet.readScalingList(_in, scalingListSize);
                    }
                }
            }
            pps.extended.secondChromaQpIndexOffset = readSE(_in, "PPS: second_chroma_qp_index_offset");
        }

        return pps;
    }

    public void write(ByteBuffer out) {
        BitWriter writer = new BitWriter(out);

        writeUEtrace(writer, picParameterSetId, "PPS: pic_parameter_set_id");
        writeUEtrace(writer, seqParameterSetId, "PPS: seq_parameter_set_id");
        writeBool(writer, entropyCodingModeFlag, "PPS: entropy_coding_mode_flag");
        writeBool(writer, picOrderPresentFlag, "PPS: pic_order_present_flag");
        writeUEtrace(writer, numSliceGroupsMinus1, "PPS: num_slice_groups_minus1");
        if (numSliceGroupsMinus1 > 0) {
            writeUEtrace(writer, sliceGroupMapType, "PPS: slice_group_map_type");
            int[] top_left = new int[1];
            int[] bottom_right = new int[1];
            int[] run_length_minus1 = new int[1];
            if (sliceGroupMapType == 0) {
                for (int iGroup = 0; iGroup <= numSliceGroupsMinus1; iGroup++) {
                    writeUEtrace(writer, run_length_minus1[iGroup], "PPS: ");
                }
            } else if (sliceGroupMapType == 2) {
                for (int iGroup = 0; iGroup < numSliceGroupsMinus1; iGroup++) {
                    writeUEtrace(writer, top_left[iGroup], "PPS: ");
                    writeUEtrace(writer, bottom_right[iGroup], "PPS: ");
                }
            } else if (sliceGroupMapType == 3 || sliceGroupMapType == 4 || sliceGroupMapType == 5) {
                writeBool(writer, sliceGroupChangeDirectionFlag, "PPS: slice_group_change_direction_flag");
                writeUEtrace(writer, sliceGroupChangeRateMinus1, "PPS: slice_group_change_rate_minus1");
            } else if (sliceGroupMapType == 6) {
                int NumberBitsPerSliceGroupId;
                if (numSliceGroupsMinus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (numSliceGroupsMinus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                writeUEtrace(writer, sliceGroupId.length, "PPS: ");
                for (int i = 0; i <= sliceGroupId.length; i++) {
                    writeU(writer, sliceGroupId[i], NumberBitsPerSliceGroupId);
                }
            }
        }
        writeUEtrace(writer, numRefIdxActiveMinus1[0], "PPS: num_ref_idx_l0_active_minus1");
        writeUEtrace(writer, numRefIdxActiveMinus1[1], "PPS: num_ref_idx_l1_active_minus1");
        writeBool(writer, weightedPredFlag, "PPS: weighted_pred_flag");
        writeNBit(writer, weightedBipredIdc, 2, "PPS: weighted_bipred_idc");
        writeSEtrace(writer, picInitQpMinus26, "PPS: pic_init_qp_minus26");
        writeSEtrace(writer, picInitQsMinus26, "PPS: pic_init_qs_minus26");
        writeSEtrace(writer, chromaQpIndexOffset, "PPS: chroma_qp_index_offset");
        writeBool(writer, deblockingFilterControlPresentFlag, "PPS: deblocking_filter_control_present_flag");
        writeBool(writer, constrainedIntraPredFlag, "PPS: constrained_intra_pred_flag");
        writeBool(writer, redundantPicCntPresentFlag, "PPS: redundant_pic_cnt_present_flag");
        if (extended != null) {
            writeBool(writer, extended.transform8x8ModeFlag, "PPS: transform_8x8_mode_flag");
            writeBool(writer, extended.scalingMatrix != null, "PPS: scalindMatrix");
            if (extended.scalingMatrix != null) {
                for (int i = 0; i < 6 + 2 * (extended.transform8x8ModeFlag ? 1 : 0); i++) {
                    writeBool(writer, extended.scalingMatrix[i] != null, "PPS: ");
                    if (extended.scalingMatrix[i] != null) {
                        SeqParameterSet.writeScalingList(writer, extended.scalingMatrix, i);
                    }
                }
            }
            writeSEtrace(writer, extended.secondChromaQpIndexOffset, "PPS: ");
        }

        writeTrailingBits(writer);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bottomRight);
        result = prime * result + chromaQpIndexOffset;
        result = prime * result + (constrainedIntraPredFlag ? 1231 : 1237);
        result = prime * result + (deblockingFilterControlPresentFlag ? 1231 : 1237);
        result = prime * result + (entropyCodingModeFlag ? 1231 : 1237);
        result = prime * result + ((extended == null) ? 0 : extended.hashCode());
        result = prime * result + numRefIdxActiveMinus1[0];
        result = prime * result + numRefIdxActiveMinus1[1];
        result = prime * result + numSliceGroupsMinus1;
        result = prime * result + picInitQpMinus26;
        result = prime * result + picInitQsMinus26;
        result = prime * result + (picOrderPresentFlag ? 1231 : 1237);
        result = prime * result + picParameterSetId;
        result = prime * result + (redundantPicCntPresentFlag ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(runLengthMinus1);
        result = prime * result + seqParameterSetId;
        result = prime * result + (sliceGroupChangeDirectionFlag ? 1231 : 1237);
        result = prime * result + sliceGroupChangeRateMinus1;
        result = prime * result + Arrays.hashCode(sliceGroupId);
        result = prime * result + sliceGroupMapType;
        result = prime * result + Arrays.hashCode(topLeft);
        result = prime * result + weightedBipredIdc;
        result = prime * result + (weightedPredFlag ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PictureParameterSet other = (PictureParameterSet) obj;
        if (!Platform.arrayEqualsInt(bottomRight, other.bottomRight))
            return false;
        if (chromaQpIndexOffset != other.chromaQpIndexOffset)
            return false;
        if (constrainedIntraPredFlag != other.constrainedIntraPredFlag)
            return false;
        if (deblockingFilterControlPresentFlag != other.deblockingFilterControlPresentFlag)
            return false;
        if (entropyCodingModeFlag != other.entropyCodingModeFlag)
            return false;
        if (extended == null) {
            if (other.extended != null)
                return false;
        } else if (!extended.equals(other.extended))
            return false;
        if (numRefIdxActiveMinus1[0] != other.numRefIdxActiveMinus1[0])
            return false;
        if (numRefIdxActiveMinus1[1] != other.numRefIdxActiveMinus1[1])
            return false;
        if (numSliceGroupsMinus1 != other.numSliceGroupsMinus1)
            return false;
        if (picInitQpMinus26 != other.picInitQpMinus26)
            return false;
        if (picInitQsMinus26 != other.picInitQsMinus26)
            return false;
        if (picOrderPresentFlag != other.picOrderPresentFlag)
            return false;
        if (picParameterSetId != other.picParameterSetId)
            return false;
        if (redundantPicCntPresentFlag != other.redundantPicCntPresentFlag)
            return false;
        if (!Platform.arrayEqualsInt(runLengthMinus1, other.runLengthMinus1))
            return false;
        if (seqParameterSetId != other.seqParameterSetId)
            return false;
        if (sliceGroupChangeDirectionFlag != other.sliceGroupChangeDirectionFlag)
            return false;
        if (sliceGroupChangeRateMinus1 != other.sliceGroupChangeRateMinus1)
            return false;
        if (!Platform.arrayEqualsInt(sliceGroupId, other.sliceGroupId))
            return false;
        if (sliceGroupMapType != other.sliceGroupMapType)
            return false;
        if (!Platform.arrayEqualsInt(topLeft, other.topLeft))
            return false;
        if (weightedBipredIdc != other.weightedBipredIdc)
            return false;
        if (weightedPredFlag != other.weightedPredFlag)
            return false;
        return true;
    }

    public PictureParameterSet copy() {
        ByteBuffer buf = ByteBuffer.allocate(2048);
        write(buf);
        buf.flip();
        return read(buf);
    }

    public boolean isEntropyCodingModeFlag() {
        return entropyCodingModeFlag;
    }

    public int[] getNumRefIdxActiveMinus1() {
        return numRefIdxActiveMinus1;
    }

    public int getSliceGroupChangeRateMinus1() {
        return sliceGroupChangeRateMinus1;
    }

    public int getPicParameterSetId() {
        return picParameterSetId;
    }

    public int getSeqParameterSetId() {
        return seqParameterSetId;
    }

    public boolean isPicOrderPresentFlag() {
        return picOrderPresentFlag;
    }

    public int getNumSliceGroupsMinus1() {
        return numSliceGroupsMinus1;
    }

    public int getSliceGroupMapType() {
        return sliceGroupMapType;
    }

    public boolean isWeightedPredFlag() {
        return weightedPredFlag;
    }

    public int getWeightedBipredIdc() {
        return weightedBipredIdc;
    }

    public int getPicInitQpMinus26() {
        return picInitQpMinus26;
    }

    public int getPicInitQsMinus26() {
        return picInitQsMinus26;
    }

    public int getChromaQpIndexOffset() {
        return chromaQpIndexOffset;
    }

    public boolean isDeblockingFilterControlPresentFlag() {
        return deblockingFilterControlPresentFlag;
    }

    public boolean isConstrainedIntraPredFlag() {
        return constrainedIntraPredFlag;
    }

    public boolean isRedundantPicCntPresentFlag() {
        return redundantPicCntPresentFlag;
    }

    public int[] getTopLeft() {
        return topLeft;
    }

    public int[] getBottomRight() {
        return bottomRight;
    }

    public int[] getRunLengthMinus1() {
        return runLengthMinus1;
    }

    public boolean isSliceGroupChangeDirectionFlag() {
        return sliceGroupChangeDirectionFlag;
    }

    public int[] getSliceGroupId() {
        return sliceGroupId;
    }

    public PPSExt getExtended() {
        return extended;
    }
}
