package org.jcodec.codecs.h264.io.write;

import org.jcodec.codecs.h264.io.model.MBlockIntraNxN;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class MBlockWriter {

    public void writeMBlockIntraNxN(MBlockIntraNxN mblock, CAVLCWriter writer) {
        // if (pps.extended != null && pps.extended.transform_8x8_mode_flag) {
        // writer
        // .writeBool(transform_size_8x8_flag,
        // "transform_size_8x8_flag");
        // }
        // if (!transform_size_8x8_flag)
        // write4x4(writer);
        // else
        // write8x8(writer);
        //
        // // writer.writeUE(intra_chroma_pred_mode, "intra_chroma_pred_mode");
        //
        // writeCodedBlockPattern(writer, transform_size_8x8_flag);
        //
        // writeResidual(writer, transform_size_8x8_flag);
    }

    private void write8x8(CAVLCWriter writer) {
        // TODO: do it!!
        throw new UnsupportedOperationException("Unsupported");
    }

    private void write4x4(CAVLCWriter writer) {
        // for (int luma4x4BlkIdx = 0; luma4x4BlkIdx < 16; luma4x4BlkIdx++) {
        // int predIntra4x4PredMode = 0;// intra4x4_pred_mode[luma4x4BlkIdx -
        // // 1];
        // boolean prev_intra4x4_pred_mode_flag =
        // reader.readBool("MBP: prev_intra4x4_pred_mode_flag");
        // if (!prev_intra4x4_pred_mode_flag) {
        // int rem_intra4x4_pred_mode = (int) reader.readNBit(3,
        // "MB: rem_intra4x4_pred_mode");
        // if (rem_intra4x4_pred_mode < predIntra4x4PredMode)
        // intra4x4_pred_mode[luma4x4BlkIdx] = rem_intra4x4_pred_mode;
        // else
        // intra4x4_pred_mode[luma4x4BlkIdx] = rem_intra4x4_pred_mode + 1;
        //
        // } else {
        // intra4x4_pred_mode[luma4x4BlkIdx] = predIntra4x4PredMode;
        // }
        // }
    }

    // private int derivePrediction4x4(BlockLocation loc) {
    // SpatialHelper sh = new SpatialHelper(sps, pps);
    // NeightbouringBlockLocations nbrs = sh
    // .findNeighbouringLuma4x4BlockLocation(loc);

    // – The variable dcPredModePredictedFlag is derived as follows.
    // – the macroblock with address mbAddrA is available and coded in
    // Inter
    // prediction mode and
    // constrained_intra_pred_flag is equal to 1
    // – the macroblock with address mbAddrB is available and coded in
    // Inter
    // prediction mode and
    // constrained_intra_pred_flag is equal to 1
    // if (pps.constrained_intra_pred_flag)
    // throw new UnsupportedOperationException(
    // "constrained_intra_pred_flag not supported here");
    //
    // boolean dcPredModePredictedFlag = nbrs.blockA == null
    // || nbrs.blockB == null;
    // return 0;

    // – For N being either replaced by A or B, the variables
    // intraMxMPredModeN are derived as follows.
    // – If dcPredModePredictedFlag is equal to 1 or the macroblock with
    // address mbAddrN is not coded in Intra_4x4
    // or Intra_8x8 macroblock prediction mode, intraMxMPredModeN is set
    // equal to 2 (Intra_4x4_DC prediction
    // mode).
    // – Otherwise (dcPredModePredictedFlag is equal to 0 and (the
    // macroblock with address mbAddrN is coded in
    // Intra_4x4 macroblock prediction mode or the macroblock with address
    // mbAddrN is coded in Intra_8x8
    // macroblock prediction mode)), the following applies.
    // – If the macroblock with address mbAddrN is coded in Intra_4x4
    // macroblock mode,
    // intraMxMPredModeN is set equal to Intra4x4PredMode[ luma4x4BlkIdxN ],
    // where Intra4x4PredMode
    // is the variable array assigned to the macroblock mbAddrN.
    // – Otherwise (the macroblock with address mbAddrN is coded in
    // Intra_8x8 macroblock mode),
    // intraMxMPredModeN is set equal to Intra8x8PredMode[ luma4x4BlkIdxN >>
    // 2 ], where
    // Intra8x8PredMode is the variable array assigned to the macroblock
    // mbAddrN.
    // – Intra4x4PredMode[ luma4x4BlkIdx ] is derived by applying the
    // following procedure.
    // predIntra4x4PredMode = Min( intraMxMPredModeA, intraMxMPredModeB )
    // if( prev_intra4x4_pred_mode_flag[ luma4x4BlkIdx ] )
    // Intra4x4PredMode[ luma4x4BlkIdx ] = predIntra4x4PredMode
    // else ( 8-42)
    // if( rem_intra4x4_pred_mode[ luma4x4BlkIdx ] < predIntra4x4PredMode )
    // Intra4x4PredMode[ luma4x4BlkIdx ] = rem_intra4x4_pred_mode[
    // luma4x4BlkIdx ]
    // else
    // Intra4x4PredMode[ luma4x4BlkIdx ] = rem_intra4x4_pred_mode[
    // luma4x4BlkIdx ] + 1
    // }

    int derivePrediction8x8() {
        return 0;
        // The process specified in subclause 6.4.8.2 is invoked with
        // luma8x8BlkIdx given as input and the output is assigned
        // to mbAddrA, luma8x8BlkIdxA, mbAddrB, and luma8x8BlkIdxB.
        // – The variable dcPredModePredictedFlag is derived as follows.
        // – If any of the following conditions are true,
        // dcPredModePredictedFlag is set equal to 1
        // – the macroblock with address mbAddrA is not available
        // – the macroblock with address mbAddrB is not available
        // – the macroblock with address mbAddrA is available and coded in
        // Inter
        // prediction mode and
        // constrained_intra_pred_flag is equal to 1
        // – the macroblock with address mbAddrB is available and coded in
        // Inter
        // prediction mode and
        // constrained_intra_pred_flag is equal to 1
        // – Otherwise, dcPredModePredictedFlag is set equal to 0.
        // – For N being either replaced by A or B, the variables
        // intraMxMPredModeN are derived as follows.
        // – If dcPredModePredictedFlag is equal to 1 or (the macroblock with
        // address mbAddrN is not coded in
        // Intra_4x4 macroblock prediction mode and the macroblock with address
        // mbAddrN is not coded in Intra_8x8
        // macroblock prediction mode), intraMxMPredModeN is set equal to 2
        // (Intra_8x8_DC prediction mode).
        // – Otherwise (dcPredModePredictedFlag is equal to 0 and (the
        // macroblock with address mbAddrN is coded in
        // Intra_4x4 macroblock prediction mode or the macroblock with address
        // mbAddrN is coded in Intra_8x8
        // macroblock prediction mode)), the following applies.
        // – If the macroblock with address mbAddrN is coded in Intra_8x8
        // macroblock mode,
        // intraMxMPredModeN is set equal to Intra8x8PredMode[ luma8x8BlkIdxN ],
        // where Intra8x8PredMode
        // is the variable array assigned to the macroblock mbAddrN.
        // – Otherwise (the macroblock with address mbAddrN is coded in
        // Intra_4x4 macroblock mode),
        // intraMxMPredModeN is derived by the following procedure, where
        // Intra4x4PredMode is the variable
        // array assigned to the macroblock mbAddrN.
        // intraMxMPredModeN = Intra4x4PredMode[ luma8x8BlkIdxN * 4 + n ] (
        // 8-71)
        // where the variable n is derived as follows
        // – If N is equal to A, depending on the variable MbaffFrameFlag, the
        // variable luma8x8BlkIdx, the
        // current macroblock, and the macroblock mbAddrN, the following
        // applies.
        // – If MbaffFrameFlag is equal to 1, the current macroblock is a
        // frame
        // coded macroblock, the
        // macroblock mbAddrN is a field coded macroblock, and luma8x8BlkIdx is
        // equal to 2, n is set
        // equal to 3.
        // – Otherwise (MbaffFrameFlag is equal to 0 or the current macroblock
        // is a field coded
        // macroblock or the macroblock mbAddrN is a frame coded macroblock or
        // luma8x8BlkIdx is
        // not equal to 2), n is set equal to 1.
        // – Otherwise (N is equal to B), n is set equal to 2.
        // – Finally, given intraMxMPredModeA and intraMxMPredModeB, the
        // variable Intra8x8PredMode[ luma8x8BlkIdx ]
        // is derived by applying the following procedure.
        // predIntra8x8PredMode = Min( intraMxMPredModeA, intraMxMPredModeB )
        // if( prev_intra8x8_pred_mode_flag[ luma8x8BlkIdx ] )
        // Intra8x8PredMode[ luma8x8BlkIdx ] = predIntra8x8PredMode
        // else ( 8-72)
        // if( rem_intra8x8_pred_mode[ luma8x8BlkIdx ] < predIntra8x8PredMode )
        // Intra8x8PredMode[ luma8x8BlkIdx ] = rem_intra8x8_pred_mode[
        // luma8x8BlkIdx ]
        // else
        // Intra8x8PredMode[ luma8x8BlkIdx ] = rem_intra8x8_pred_mode[
        // luma8x8BlkIdx ] + 1
    }
}
