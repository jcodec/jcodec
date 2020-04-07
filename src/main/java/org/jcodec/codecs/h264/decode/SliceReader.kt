package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.common.biari.MDecoder
import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.decode.CAVLCReader.moreRBSPData
import org.jcodec.codecs.h264.decode.CAVLCReader.readBool
import org.jcodec.codecs.h264.decode.CAVLCReader.readNBit
import org.jcodec.codecs.h264.decode.CAVLCReader.readSE
import org.jcodec.codecs.h264.decode.CAVLCReader.readTE
import org.jcodec.codecs.h264.decode.CAVLCReader.readUEtrace
import org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint
import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.codecs.h264.io.CABAC
import org.jcodec.codecs.h264.io.CAVLC
import org.jcodec.codecs.h264.io.model.*
import org.jcodec.common.io.BitReader
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.ColorSpace

/**
 * Contains methods for reading high-level symbols out of H.264 bitstream
 *
 * @author The JCodec Project
 */
class SliceReader(private val activePps: PictureParameterSet, private val cabac: CABAC, private val cavlc: Array<CAVLC>, private val mDecoder: MDecoder?, private val reader: BitReader,
                  private val mapper: Mapper, val sliceHeader: SliceHeader, val nALUnit: NALUnit) {
    private var prevMbSkipped = false
    private var mbIdx = 0
    private var prevMBType: MBType? = null
    private var mbSkipRun = 0
    private var endOfData = false

    // State
    var topMBType: Array<MBType?>
    var leftMBType: MBType? = null
    var leftCBPLuma = 0
    var topCBPLuma: IntArray
    var leftCBPChroma = 0
    var topCBPChroma: IntArray
    var chromaFormat: ColorSpace?
    var transform8x8: Boolean
    var numRef: IntArray
    var tf8x8Left = false
    var tf8x8Top: BooleanArray
    var i4x4PredTop: IntArray
    var i4x4PredLeft: IntArray
    var predModeLeft: Array<PartPred?>
    var predModeTop: Array<PartPred?>
    fun readMacroblock(mBlock: MBlock): Boolean {
        val mbWidth = sliceHeader.sps!!.picWidthInMbsMinus1 + 1
        val mbHeight = sliceHeader.sps!!.picHeightInMapUnitsMinus1 + 1
        if (endOfData && mbSkipRun == 0 || mbIdx >= mbWidth * mbHeight) return false
        mBlock.mbIdx = mbIdx
        mBlock.prevMbType = prevMBType
        val mbaffFrameFlag = sliceHeader.sps!!.isMbAdaptiveFrameFieldFlag && !sliceHeader.fieldPicFlag
        if (sliceHeader.sliceType!!.isInter && !activePps.isEntropyCodingModeFlag) {
            if (!prevMbSkipped && mbSkipRun == 0) {
                mbSkipRun = readUEtrace(reader, "mb_skip_run")
                if (!moreRBSPData(reader)) {
                    endOfData = true
                }
            }
            if (mbSkipRun > 0) {
                --mbSkipRun
                val mbAddr = mapper.getAddress(mbIdx)
                prevMbSkipped = true
                prevMBType = null
                debugPrint("---------------------- MB (%d,%d) ---------------------", mbAddr % mbWidth,
                        mbAddr / mbWidth)
                mBlock.skipped = true
                val mbX = mapper.getMbX(mBlock.mbIdx)
                leftMBType = null
                topMBType[mbX] = leftMBType
                val blk8x8X = mbX shl 1
                predModeTop[blk8x8X + 1] = PartPred.L0
                predModeTop[blk8x8X] = predModeTop[blk8x8X + 1]
                predModeLeft[1] = predModeTop[blk8x8X]
                predModeLeft[0] = predModeLeft[1]
                ++mbIdx
                return true
            } else {
                prevMbSkipped = false
            }
        }
        val mbAddr = mapper.getAddress(mbIdx)
        val mbX = mbAddr % mbWidth
        val mbY = mbAddr / mbWidth
        debugPrint("---------------------- MB (%d,%d) ---------------------", mbX, mbY)
        if (sliceHeader.sliceType!!.isIntra
                || !activePps.isEntropyCodingModeFlag || !readMBSkipFlag(sliceHeader.sliceType, mapper.leftAvailable(mbIdx),
                        mapper.topAvailable(mbIdx), mbX)) {
            var mb_field_decoding_flag = false
            if (mbaffFrameFlag && (mbIdx % 2 == 0 || mbIdx % 2 == 1 && prevMbSkipped)) {
                mb_field_decoding_flag = readBool(reader, "mb_field_decoding_flag")
            }
            mBlock.fieldDecoding = mb_field_decoding_flag
            readMBlock(mBlock, sliceHeader.sliceType)
            prevMBType = mBlock.curMbType
        } else {
            prevMBType = null
            prevMbSkipped = true
            mBlock.skipped = true
            val blk8x8X = mbX shl 1
            predModeTop[blk8x8X + 1] = PartPred.L0
            predModeTop[blk8x8X] = predModeTop[blk8x8X + 1]
            predModeLeft[1] = predModeTop[blk8x8X]
            predModeLeft[0] = predModeLeft[1]
        }
        endOfData = (activePps.isEntropyCodingModeFlag && mDecoder!!.decodeFinalBin() == 1
                || !activePps.isEntropyCodingModeFlag && !moreRBSPData(reader))
        ++mbIdx
        leftMBType = mBlock.curMbType
        topMBType[mapper.getMbX(mBlock.mbIdx)] = leftMBType
        return true
    }

    fun readMBQpDelta(prevMbType: MBType?): Int {
        val mbQPDelta: Int
        mbQPDelta = if (!activePps.isEntropyCodingModeFlag) {
            readSE(reader, "mb_qp_delta")
        } else {
            cabac.readMBQpDelta(mDecoder!!, prevMbType)
        }
        return mbQPDelta
    }

    fun readChromaPredMode(mbX: Int, leftAvailable: Boolean, topAvailable: Boolean): Int {
        val chromaPredictionMode: Int
        chromaPredictionMode = if (!activePps.isEntropyCodingModeFlag) {
            readUEtrace(reader, "MBP: intra_chroma_pred_mode")
        } else {
            cabac.readIntraChromaPredMode(mDecoder!!, mbX, leftMBType, topMBType[mbX],
                    leftAvailable, topAvailable)
        }
        return chromaPredictionMode
    }

    fun readTransform8x8Flag(leftAvailable: Boolean, topAvailable: Boolean, leftType: MBType?, topType: MBType?,
                             is8x8Left: Boolean, is8x8Top: Boolean): Boolean {
        return if (!activePps.isEntropyCodingModeFlag) readBool(reader, "transform_size_8x8_flag") else cabac.readTransform8x8Flag(mDecoder!!, leftAvailable, topAvailable, leftType, topType, is8x8Left,
                is8x8Top)
    }

    protected fun readCodedBlockPatternIntra(leftAvailable: Boolean, topAvailable: Boolean, leftCBP: Int, topCBP: Int,
                                             leftMB: MBType?, topMB: MBType?): Int {
        return if (!activePps.isEntropyCodingModeFlag) H264Const.CODED_BLOCK_PATTERN_INTRA_COLOR[readUEtrace(reader, "coded_block_pattern")] else cabac.codedBlockPatternIntra(mDecoder!!, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB)
    }

    protected fun readCodedBlockPatternInter(leftAvailable: Boolean, topAvailable: Boolean, leftCBP: Int, topCBP: Int,
                                             leftMB: MBType?, topMB: MBType?): Int {
        return if (!activePps.isEntropyCodingModeFlag) {
            val code = readUEtrace(reader, "coded_block_pattern")
            H264Const.CODED_BLOCK_PATTERN_INTER_COLOR[code]
        } else cabac.codedBlockPatternIntra(mDecoder!!, leftAvailable, topAvailable, leftCBP, topCBP, leftMB, topMB)
    }

    fun readRefIdx(leftAvailable: Boolean, topAvailable: Boolean, leftType: MBType?, topType: MBType?, leftPred: PartPred?,
                   topPred: PartPred?, curPred: PartPred?, mbX: Int, partX: Int, partY: Int, partW: Int, partH: Int, list: Int): Int {
        return if (!activePps.isEntropyCodingModeFlag) readTE(reader, numRef[list] - 1) else cabac.readRefIdx(mDecoder!!, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                curPred!!, mbX, partX, partY, partW, partH, list)
    }

    fun readMVD(comp: Int, leftAvailable: Boolean, topAvailable: Boolean, leftType: MBType?, topType: MBType?,
                leftPred: PartPred?, topPred: PartPred?, curPred: PartPred?, mbX: Int, partX: Int, partY: Int, partW: Int, partH: Int,
                list: Int): Int {
        return if (!activePps.isEntropyCodingModeFlag) readSE(reader, "mvd_l0_x") else cabac.readMVD(mDecoder!!, comp, leftAvailable, topAvailable, leftType, topType, leftPred, topPred,
                curPred!!, mbX, partX, partY, partW, partH, list)
    }

    fun readPredictionI4x4Block(leftAvailable: Boolean, topAvailable: Boolean, leftMBType: MBType?, topMBType: MBType?,
                                blkX: Int, blkY: Int, mbX: Int): Int {
        var mode = 2
        if ((leftAvailable || blkX > 0) && (topAvailable || blkY > 0)) {
            val predModeB = if (topMBType == MBType.I_NxN || blkY > 0) i4x4PredTop[(mbX shl 2) + blkX] else 2
            val predModeA = if (leftMBType == MBType.I_NxN || blkX > 0) i4x4PredLeft[blkY] else 2
            mode = Math.min(predModeB, predModeA)
        }
        if (!prev4x4PredMode()) {
            val rem_intra4x4_pred_mode = rem4x4PredMode()
            mode = rem_intra4x4_pred_mode + if (rem_intra4x4_pred_mode < mode) 0 else 1
        }
        i4x4PredLeft[blkY] = mode
        i4x4PredTop[(mbX shl 2) + blkX] = i4x4PredLeft[blkY]
        return mode
    }

    fun rem4x4PredMode(): Int {
        return if (!activePps.isEntropyCodingModeFlag) readNBit(reader, 3, "MB: rem_intra4x4_pred_mode") else cabac.rem4x4PredMode(mDecoder!!)
    }

    fun prev4x4PredMode(): Boolean {
        return if (!activePps.isEntropyCodingModeFlag) readBool(reader, "MBP: prev_intra4x4_pred_mode_flag") else cabac.prev4x4PredModeFlag(mDecoder!!)
    }

    fun read16x16DC(leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, dc: IntArray?) {
        if (!activePps.isEntropyCodingModeFlag) cavlc[0].readLumaDCBlock(reader, dc, mbX, leftAvailable, leftMBType, topAvailable, topMBType[mbX],
                CoeffTransformer.zigzag4x4) else {
            if (cabac.readCodedBlockFlagLumaDC(mDecoder!!, mbX, leftMBType, topMBType[mbX], leftAvailable, topAvailable,
                            MBType.I_16x16) == 1) cabac.readCoeffs(mDecoder, CABAC.BlockType.LUMA_16_DC, dc!!, 0, 16, CoeffTransformer.zigzag4x4,
                    H264Const.identityMapping16, H264Const.identityMapping16)
        }
    }

    /**
     * Reads AC block of macroblock encoded as I_16x16, returns number of
     * non-zero coefficients
     *
     * @return
     */
    fun read16x16AC(leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, cbpLuma: Int, ac: IntArray?, blkOffLeft: Int,
                    blkOffTop: Int, blkX: Int, blkY: Int): Int {
        if (!activePps.isEntropyCodingModeFlag) {
            return cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    if (blkOffLeft == 0) leftMBType else MBType.I_16x16, blkOffTop != 0 || topAvailable,
                    if (blkOffTop == 0) topMBType[mbX] else MBType.I_16x16, 1, 15, CoeffTransformer.zigzag4x4)
        } else {
            if (cabac.readCodedBlockFlagLumaAC(mDecoder!!, CABAC.BlockType.LUMA_15_AC, blkX, blkOffTop, 0, leftMBType,
                            topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma, MBType.I_16x16) == 1) return cabac.readCoeffs(mDecoder, CABAC.BlockType.LUMA_15_AC, ac!!, 1, 15, CoeffTransformer.zigzag4x4,
                    H264Const.identityMapping16, H264Const.identityMapping16)
        }
        return 0
    }

    /**
     * Reads AC block of a macroblock, return number of non-zero coefficients
     *
     * @return
     */
    fun readResidualAC(leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, curMbType: MBType?, cbpLuma: Int,
                       blkOffLeft: Int, blkOffTop: Int, blkX: Int, blkY: Int, ac: IntArray?): Int {
        if (!activePps.isEntropyCodingModeFlag) {
            return if (reader.remaining() <= 0) 0 else cavlc[0].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    if (blkOffLeft == 0) leftMBType else curMbType, blkOffTop != 0 || topAvailable,
                    if (blkOffTop == 0) topMBType[mbX] else curMbType, 0, 16, CoeffTransformer.zigzag4x4)
        } else {
            if (cabac.readCodedBlockFlagLumaAC(mDecoder!!, CABAC.BlockType.LUMA_16, blkX, blkOffTop, 0, leftMBType,
                            topMBType[mbX], leftAvailable, topAvailable, leftCBPLuma, topCBPLuma[mbX], cbpLuma, curMbType!!) == 1) return cabac.readCoeffs(mDecoder, CABAC.BlockType.LUMA_16, ac!!, 0, 16, CoeffTransformer.zigzag4x4,
                    H264Const.identityMapping16, H264Const.identityMapping16)
        }
        return 0
    }

    fun setZeroCoeff(comp: Int, blkX: Int, blkOffTop: Int) {
        cavlc[comp].setZeroCoeff(blkX, blkOffTop)
    }

    fun savePrevCBP(codedBlockPattern: Int) {
        if (activePps.isEntropyCodingModeFlag) cabac.setPrevCBP(codedBlockPattern)
    }

    fun readLumaAC(leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, curMbType: MBType?, blkX: Int, j: Int,
                   ac16: IntArray?, blkOffLeft: Int, blkOffTop: Int): Int {
        return cavlc[0].readACBlock(reader, ac16, blkX + (j and 1), blkOffTop, blkOffLeft != 0 || leftAvailable,
                if (blkOffLeft == 0) leftMBType else curMbType, blkOffTop != 0 || topAvailable,
                if (blkOffTop == 0) topMBType[mbX] else curMbType, 0, 16, H264Const.identityMapping16)
    }

    /**
     * Reads luma AC coeffiecients for 8x8 blocks, returns number of non-zero
     * coefficients
     *
     * @return
     */
    fun readLumaAC8x8(blkX: Int, blkY: Int, ac: IntArray?): Int {
        val readCoeffs = cabac.readCoeffs(mDecoder!!, CABAC.BlockType.LUMA_64, ac!!, 0, 64, CoeffTransformer.zigzag8x8,
                H264Const.sig_coeff_map_8x8, H264Const.last_sig_coeff_map_8x8)
        cabac.setCodedBlock(blkX, blkY)
        cabac.setCodedBlock(blkX + 1, blkY)
        cabac.setCodedBlock(blkX, blkY + 1)
        cabac.setCodedBlock(blkX + 1, blkY + 1)
        return readCoeffs
    }

    fun readSubMBTypeP(): Int {
        return if (!activePps.isEntropyCodingModeFlag) readUEtrace(reader, "SUB: sub_mb_type") else cabac.readSubMbTypeP(mDecoder!!)
    }

    fun readSubMBTypeB(): Int {
        return if (!activePps.isEntropyCodingModeFlag) readUEtrace(reader, "SUB: sub_mb_type") else cabac.readSubMbTypeB(mDecoder!!)
    }

    fun readChromaDC(mbX: Int, leftAvailable: Boolean, topAvailable: Boolean, dc: IntArray?, comp: Int, curMbType: MBType?) {
        if (!activePps.isEntropyCodingModeFlag) cavlc[comp].readChromaDCBlock(reader, dc!!, leftAvailable, topAvailable) else {
            if (cabac.readCodedBlockFlagChromaDC(mDecoder!!, mbX, comp, leftMBType, topMBType[mbX], leftAvailable,
                            topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType!!) == 1) cabac.readCoeffs(mDecoder, CABAC.BlockType.CHROMA_DC, dc!!, 0, 4, H264Const.identityMapping16, H264Const.identityMapping16,
                    H264Const.identityMapping16)
        }
    }

    fun readChromaAC(leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, comp: Int, curMbType: MBType?,
                     ac: IntArray?, blkOffLeft: Int, blkOffTop: Int, blkX: Int) {
        if (!activePps.isEntropyCodingModeFlag) {
            if (reader.remaining() <= 0) return
            cavlc[comp].readACBlock(reader, ac, blkX, blkOffTop, blkOffLeft != 0 || leftAvailable,
                    if (blkOffLeft == 0) leftMBType else curMbType, blkOffTop != 0 || topAvailable,
                    if (blkOffTop == 0) topMBType[mbX] else curMbType, 1, 15, CoeffTransformer.zigzag4x4)
        } else {
            if (cabac.readCodedBlockFlagChromaAC(mDecoder!!, blkX, blkOffTop, comp, leftMBType, topMBType[mbX],
                            leftAvailable, topAvailable, leftCBPChroma, topCBPChroma[mbX], curMbType!!) == 1) cabac.readCoeffs(mDecoder, CABAC.BlockType.CHROMA_AC, ac!!, 1, 15, CoeffTransformer.zigzag4x4,
                    H264Const.identityMapping16, H264Const.identityMapping16)
        }
    }

    fun decodeMBTypeI(mbIdx: Int, leftAvailable: Boolean, topAvailable: Boolean, leftMBType: MBType?, topMBType: MBType?): Int {
        val mbType: Int
        mbType = if (!activePps.isEntropyCodingModeFlag) readUEtrace(reader, "MB: mb_type") else cabac.readMBTypeI(mDecoder!!, leftMBType, topMBType, leftAvailable, topAvailable)
        return mbType
    }

    fun readMBTypeP(): Int {
        val mbType: Int
        mbType = if (!activePps.isEntropyCodingModeFlag) readUEtrace(reader, "MB: mb_type") else cabac.readMBTypeP(mDecoder!!)
        return mbType
    }

    fun readMBTypeB(mbIdx: Int, leftAvailable: Boolean, topAvailable: Boolean, leftMBType: MBType?, topMBType: MBType?): Int {
        val mbType: Int
        mbType = if (!activePps.isEntropyCodingModeFlag) readUEtrace(reader, "MB: mb_type") else cabac.readMBTypeB(mDecoder!!, leftMBType, topMBType, leftAvailable, topAvailable)
        return mbType
    }

    fun readMBSkipFlag(slType: SliceType?, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int): Boolean {
        return cabac.readMBSkipFlag(mDecoder!!, slType!!, leftAvailable, topAvailable, mbX)
    }

    fun readIntra16x16(mbType: Int, mBlock: MBlock) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        mBlock.cbp(mbType / 12 * 15, mbType / 4 % 3)
        mBlock.luma16x16Mode = mbType % 4
        mBlock.chromaPredictionMode = readChromaPredMode(mbX, leftAvailable, topAvailable)
        mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType)
        read16x16DC(leftAvailable, topAvailable, mbX, mBlock.dc)
        for (i in 0..15) {
            val blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i]
            val blkOffTop = H264Const.MB_BLK_OFF_TOP[i]
            val blkX = (mbX shl 2) + blkOffLeft
            val blkY = (mbY shl 2) + blkOffTop
            if (mBlock.cbpLuma() and (1 shl (i shr 2)) != 0) {
                mBlock.nCoeff[i] = read16x16AC(leftAvailable, topAvailable, mbX, mBlock.cbpLuma(), mBlock.ac[0][i],
                        blkOffLeft, blkOffTop, blkX, blkY)
            } else {
                if (!sliceHeader.pps!!.isEntropyCodingModeFlag) setZeroCoeff(0, blkX, blkOffTop)
            }
        }
        if (chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX)
        }
    }

    fun readMBlockBDirect(mBlock: MBlock) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val lAvb = mapper.leftAvailable(mBlock.mbIdx)
        val tAvb = mapper.topAvailable(mBlock.mbIdx)
        mBlock._cbp = readCodedBlockPatternInter(lAvb, tAvb, leftCBPLuma or (leftCBPChroma shl 4), topCBPLuma[mbX]
                or (topCBPChroma[mbX] shl 4), leftMBType, topMBType[mbX])
        mBlock.transform8x8Used = false
        if (transform8x8 && mBlock.cbpLuma() != 0 && sliceHeader.sps!!.isDirect8x8InferenceFlag) {
            mBlock.transform8x8Used = readTransform8x8Flag(lAvb, tAvb, leftMBType, topMBType[mbX], tf8x8Left,
                    tf8x8Top[mbX])
        }
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType)
        }
        readResidualLuma(mBlock, lAvb, tAvb, mbX, mbY)
        readChromaResidual(mBlock, lAvb, tAvb, mbX)
        predModeLeft[1] = PartPred.Direct
        predModeLeft[0] = predModeLeft[1]
        predModeTop[(mbX shl 1) + 1] = predModeLeft[0]
        predModeTop[mbX shl 1] = predModeTop[(mbX shl 1) + 1]
    }

    fun readInter16x16(p0: PartPred?, mBlock: MBlock) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        for (list in 0..1) {
            if (H264Const.usesList(p0, list) && numRef[list] > 1) mBlock.pb16x16.refIdx[list] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[mbX shl 1], p0, mbX, 0, 0, 4, 4, list)
        }
        for (list in 0..1) {
            readPredictionInter16x16(mBlock, mbX, leftAvailable, topAvailable, list, p0)
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY)
        predModeTop[(mbX shl 1) + 1] = p0
        predModeTop[mbX shl 1] = predModeTop[(mbX shl 1) + 1]
        predModeLeft[1] = predModeTop[mbX shl 1]
        predModeLeft[0] = predModeLeft[1]
    }

    private fun readPredInter8x16(mBlock: MBlock, mbX: Int, leftAvailable: Boolean, topAvailable: Boolean, list: Int,
                                  p0: PartPred?, p1: PartPred?) {
        val blk8x8X = mbX shl 1
        if (H264Const.usesList(p0, list)) {
            mBlock.pb168x168.mvdX1[list] = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list)
            mBlock.pb168x168.mvdY1[list] = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 2, 4, list)
        }
        if (H264Const.usesList(p1, list)) {
            mBlock.pb168x168.mvdX2[list] = readMVD(0, true, topAvailable, MBType.P_8x16, topMBType[mbX], p0,
                    predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list)
            mBlock.pb168x168.mvdY2[list] = readMVD(1, true, topAvailable, MBType.P_8x16, topMBType[mbX], p0,
                    predModeTop[blk8x8X + 1], p1, mbX, 2, 0, 2, 4, list)
        }
    }

    private fun readPredictionInter16x8(mBlock: MBlock, mbX: Int, leftAvailable: Boolean, topAvailable: Boolean,
                                        p0: PartPred?, p1: PartPred?, list: Int) {
        val blk8x8X = mbX shl 1
        if (H264Const.usesList(p0, list)) {
            mBlock.pb168x168.mvdX1[list] = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list)
            mBlock.pb168x168.mvdY1[list] = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[blk8x8X], p0, mbX, 0, 0, 4, 2, list)
        }
        if (H264Const.usesList(p1, list)) {
            mBlock.pb168x168.mvdX2[list] = readMVD(0, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1],
                    p0, p1, mbX, 0, 2, 4, 2, list)
            mBlock.pb168x168.mvdY2[list] = readMVD(1, leftAvailable, true, leftMBType, MBType.P_16x8, predModeLeft[1],
                    p0, p1, mbX, 0, 2, 4, 2, list)
        }
    }

    fun readInter16x8(p0: PartPred?, p1: PartPred?, mBlock: MBlock) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        for (list in 0..1) {
            if (H264Const.usesList(p0, list) && numRef[list] > 1) mBlock.pb168x168.refIdx1[list] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[mbX shl 1], p0, mbX, 0, 0, 4, 2, list)
            if (H264Const.usesList(p1, list) && numRef[list] > 1) mBlock.pb168x168.refIdx2[list] = readRefIdx(leftAvailable, true, leftMBType, mBlock.curMbType,
                    predModeLeft[1], p0, p1, mbX, 0, 2, 4, 2, list)
        }
        for (list in 0..1) {
            readPredictionInter16x8(mBlock, mbX, leftAvailable, topAvailable, p0, p1, list)
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY)
        predModeLeft[0] = p0
        predModeTop[(mbX shl 1) + 1] = p1
        predModeTop[mbX shl 1] = predModeTop[(mbX shl 1) + 1]
        predModeLeft[1] = predModeTop[mbX shl 1]
    }

    fun readIntra8x16(p0: PartPred?, p1: PartPred?, mBlock: MBlock) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        for (list in 0..1) {
            if (H264Const.usesList(p0, list) && numRef[list] > 1) mBlock.pb168x168.refIdx1[list] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[mbX shl 1], p0, mbX, 0, 0, 2, 4, list)
            if (H264Const.usesList(p1, list) && numRef[list] > 1) mBlock.pb168x168.refIdx2[list] = readRefIdx(true, topAvailable, mBlock.curMbType, topMBType[mbX], p0,
                    predModeTop[(mbX shl 1) + 1], p1, mbX, 2, 0, 2, 4, list)
        }
        for (list in 0..1) {
            readPredInter8x16(mBlock, mbX, leftAvailable, topAvailable, list, p0, p1)
        }
        readResidualInter(mBlock, leftAvailable, topAvailable, mbX, mbY)
        predModeTop[mbX shl 1] = p0
        predModeLeft[1] = p1
        predModeLeft[0] = predModeLeft[1]
        predModeTop[(mbX shl 1) + 1] = predModeLeft[0]
    }

    private fun readPredictionInter16x16(mBlock: MBlock, mbX: Int, leftAvailable: Boolean, topAvailable: Boolean,
                                         list: Int, curPred: PartPred?) {
        val blk8x8X = mbX shl 1
        if (H264Const.usesList(curPred, list)) {
            mBlock.pb16x16.mvdX[list] = readMVD(0, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list)
            mBlock.pb16x16.mvdY[list] = readMVD(1, leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[blk8x8X], curPred, mbX, 0, 0, 4, 4, list)
        }
    }

    private fun readResidualInter(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int) {
        mBlock._cbp = readCodedBlockPatternInter(leftAvailable, topAvailable, leftCBPLuma or (leftCBPChroma shl 4),
                topCBPLuma[mbX] or (topCBPChroma[mbX] shl 4), leftMBType, topMBType[mbX])
        mBlock.transform8x8Used = false
        if (mBlock.cbpLuma() != 0 && transform8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    tf8x8Left, tf8x8Top[mbX])
        }
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType)
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY)
        if (chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX)
        }
    }

    fun readMBlock8x8(mBlock: MBlock) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        val noSubMBLessThen8x8: Boolean
        noSubMBLessThen8x8 = if (mBlock.curMbType == MBType.P_8x8 || mBlock.curMbType == MBType.P_8x8ref0) {
            readPrediction8x8P(mBlock, mbX, leftAvailable, topAvailable)
            mBlock.pb8x8.subMbTypes[0] == 0 && mBlock.pb8x8.subMbTypes[1] == 0 && mBlock.pb8x8.subMbTypes[2] == 0 && mBlock.pb8x8.subMbTypes[3] == 0
        } else {
            readPrediction8x8B(mBlock, mbX, leftAvailable, topAvailable)
            H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[0]] == 0 && H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[1]] == 0 && H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[2]] == 0 && H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[3]] == 0
        }
        mBlock._cbp = readCodedBlockPatternInter(leftAvailable, topAvailable, leftCBPLuma or (leftCBPChroma shl 4),
                topCBPLuma[mbX] or (topCBPChroma[mbX] shl 4), leftMBType, topMBType[mbX])
        mBlock.transform8x8Used = false
        if (transform8x8 && mBlock.cbpLuma() != 0 && noSubMBLessThen8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    tf8x8Left, tf8x8Top[mbX])
        }
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType)
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY)
        readChromaResidual(mBlock, leftAvailable, topAvailable, mbX)
    }

    private fun readPrediction8x8P(mBlock: MBlock, mbX: Int, leftAvailable: Boolean, topAvailable: Boolean) {
        for (i in 0..3) {
            mBlock.pb8x8.subMbTypes[i] = readSubMBTypeP()
        }
        if (numRef[0] > 1 && mBlock.curMbType != MBType.P_8x8ref0) {
            mBlock.pb8x8.refIdx[0][0] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX], PartPred.L0, PartPred.L0, PartPred.L0,
                    mbX, 0, 0, 2, 2, 0)
            mBlock.pb8x8.refIdx[0][1] = readRefIdx(true, topAvailable, MBType.P_8x8, topMBType[mbX], PartPred.L0, PartPred.L0, PartPred.L0, mbX, 2, 0, 2,
                    2, 0)
            mBlock.pb8x8.refIdx[0][2] = readRefIdx(leftAvailable, true, leftMBType, MBType.P_8x8, PartPred.L0, PartPred.L0, PartPred.L0, mbX, 0, 2, 2, 2,
                    0)
            mBlock.pb8x8.refIdx[0][3] = readRefIdx(true, true, MBType.P_8x8, MBType.P_8x8, PartPred.L0, PartPred.L0, PartPred.L0, mbX, 2, 2, 2, 2, 0)
        }
        readSubMb8x8(mBlock, 0, mBlock.pb8x8.subMbTypes[0], topAvailable, leftAvailable, 0, 0, mbX, leftMBType,
                topMBType[mbX], MBType.P_8x8, PartPred.L0, PartPred.L0, PartPred.L0, 0)
        readSubMb8x8(mBlock, 1, mBlock.pb8x8.subMbTypes[1], topAvailable, true, 2, 0, mbX, MBType.P_8x8, topMBType[mbX],
                MBType.P_8x8, PartPred.L0, PartPred.L0, PartPred.L0, 0)
        readSubMb8x8(mBlock, 2, mBlock.pb8x8.subMbTypes[2], true, leftAvailable, 0, 2, mbX, leftMBType, MBType.P_8x8, MBType.P_8x8, PartPred.L0, PartPred.L0, PartPred.L0, 0)
        readSubMb8x8(mBlock, 3, mBlock.pb8x8.subMbTypes[3], true, true, 2, 2, mbX, MBType.P_8x8, MBType.P_8x8, MBType.P_8x8, PartPred.L0, PartPred.L0, PartPred.L0, 0)
        val blk8x8X = mbX shl 1
        predModeTop[blk8x8X + 1] = PartPred.L0
        predModeTop[blk8x8X] = predModeTop[blk8x8X + 1]
        predModeLeft[1] = predModeTop[blk8x8X]
        predModeLeft[0] = predModeLeft[1]
    }

    private fun readPrediction8x8B(mBlock: MBlock, mbX: Int, leftAvailable: Boolean, topAvailable: Boolean) {
        val p = arrayOfNulls<PartPred>(4)
        for (i in 0..3) {
            mBlock.pb8x8.subMbTypes[i] = readSubMBTypeB()
            p[i] = H264Const.bPartPredModes[mBlock.pb8x8.subMbTypes[i]]
        }
        for (list in 0..1) {
            if (numRef[list] <= 1) continue
            if (H264Const.usesList(p[0], list)) mBlock.pb8x8.refIdx[list][0] = readRefIdx(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    predModeLeft[0], predModeTop[mbX shl 1], p[0], mbX, 0, 0, 2, 2, list)
            if (H264Const.usesList(p[1], list)) mBlock.pb8x8.refIdx[list][1] = readRefIdx(true, topAvailable, MBType.B_8x8, topMBType[mbX], p[0],
                    predModeTop[(mbX shl 1) + 1], p[1], mbX, 2, 0, 2, 2, list)
            if (H264Const.usesList(p[2], list)) mBlock.pb8x8.refIdx[list][2] = readRefIdx(leftAvailable, true, leftMBType, MBType.B_8x8, predModeLeft[1],
                    p[0], p[2], mbX, 0, 2, 2, 2, list)
            if (H264Const.usesList(p[3], list)) mBlock.pb8x8.refIdx[list][3] = readRefIdx(true, true, MBType.B_8x8, MBType.B_8x8, p[2], p[1], p[3], mbX, 2, 2, 2, 2,
                    list)
        }
        debugPrint("Pred: " + p[0] + ", " + p[1] + ", " + p[2] + ", " + p[3])
        val blk8x8X = mbX shl 1
        for (list in 0..1) {
            if (H264Const.usesList(p[0], list)) {
                readSubMb8x8(mBlock, 0, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[0]], topAvailable, leftAvailable, 0, 0,
                        mbX, leftMBType, topMBType[mbX], MBType.B_8x8, predModeLeft[0], predModeTop[blk8x8X], p[0], list)
            }
            if (H264Const.usesList(p[1], list)) {
                readSubMb8x8(mBlock, 1, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[1]], topAvailable, true, 2, 0, mbX, MBType.B_8x8,
                        topMBType[mbX], MBType.B_8x8, p[0], predModeTop[blk8x8X + 1], p[1], list)
            }
            if (H264Const.usesList(p[2], list)) {
                readSubMb8x8(mBlock, 2, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[2]], true, leftAvailable, 0, 2, mbX,
                        leftMBType, MBType.B_8x8, MBType.B_8x8, predModeLeft[1], p[0], p[2], list)
            }
            if (H264Const.usesList(p[3], list)) {
                readSubMb8x8(mBlock, 3, H264Const.bSubMbTypes[mBlock.pb8x8.subMbTypes[3]], true, true, 2, 2, mbX, MBType.B_8x8, MBType.B_8x8,
                        MBType.B_8x8, p[2], p[1], p[3], list)
            }
        }
        predModeLeft[0] = p[1]
        predModeTop[blk8x8X] = p[2]
        predModeTop[blk8x8X + 1] = p[3]
        predModeLeft[1] = predModeTop[blk8x8X + 1]
    }

    private fun readSubMb8x8(mBlock: MBlock, partNo: Int, subMbType: Int, tAvb: Boolean, lAvb: Boolean, blk8x8X: Int,
                             blk8x8Y: Int, mbX: Int, leftMBType: MBType?, topMBType: MBType?, curMBType: MBType, leftPred: PartPred?,
                             topPred: PartPred?, partPred: PartPred?, list: Int) {
        when (subMbType) {
            3 -> readSub4x4(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list)
            2 -> readSub4x8(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list)
            1 -> readSub8x4(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, curMBType, leftPred,
                    topPred, partPred, list)
            0 -> readSub8x8(mBlock, partNo, tAvb, lAvb, blk8x8X, blk8x8Y, mbX, leftMBType, topMBType, leftPred, topPred,
                    partPred, list)
        }
    }

    private fun readSub8x8(mBlock: MBlock, partNo: Int, tAvb: Boolean, lAvb: Boolean, blk8x8X: Int, blk8x8Y: Int, mbX: Int,
                           leftMBType: MBType?, topMBType: MBType?, leftPred: PartPred?, topPred: PartPred?, partPred: PartPred?, list: Int) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 2, 2, list)
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 2, 2, list)
        debugPrint("mvd: (%d, %d)", mBlock.pb8x8.mvdX1[list][partNo], mBlock.pb8x8.mvdY1[list][partNo])
    }

    private fun readSub8x4(mBlock: MBlock, partNo: Int, tAvb: Boolean, lAvb: Boolean, blk8x8X: Int, blk8x8Y: Int, mbX: Int,
                           leftMBType: MBType?, topMBType: MBType?, curMBType: MBType, leftPred: PartPred?, topPred: PartPred?,
                           partPred: PartPred?, list: Int) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 2, 1, list)
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 2, 1, list)
        mBlock.pb8x8.mvdX2[list][partNo] = readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
                mbX, blk8x8X, blk8x8Y + 1, 2, 1, list)
        mBlock.pb8x8.mvdY2[list][partNo] = readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
                mbX, blk8x8X, blk8x8Y + 1, 2, 1, list)
    }

    private fun readSub4x8(mBlock: MBlock, partNo: Int, tAvb: Boolean, lAvb: Boolean, blk8x8X: Int, blk8x8Y: Int, mbX: Int,
                           leftMBType: MBType?, topMBType: MBType?, curMBType: MBType, leftPred: PartPred?, topPred: PartPred?,
                           partPred: PartPred?, list: Int) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 1, 2, list)
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 1, 2, list)
        mBlock.pb8x8.mvdX2[list][partNo] = readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
                mbX, blk8x8X + 1, blk8x8Y, 1, 2, list)
        mBlock.pb8x8.mvdY2[list][partNo] = readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
                mbX, blk8x8X + 1, blk8x8Y, 1, 2, list)
    }

    private fun readSub4x4(mBlock: MBlock, partNo: Int, tAvb: Boolean, lAvb: Boolean, blk8x8X: Int, blk8x8Y: Int, mbX: Int,
                           leftMBType: MBType?, topMBType: MBType?, curMBType: MBType, leftPred: PartPred?, topPred: PartPred?,
                           partPred: PartPred?, list: Int) {
        mBlock.pb8x8.mvdX1[list][partNo] = readMVD(0, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 1, 1, list)
        mBlock.pb8x8.mvdY1[list][partNo] = readMVD(1, lAvb, tAvb, leftMBType, topMBType, leftPred, topPred, partPred,
                mbX, blk8x8X, blk8x8Y, 1, 1, list)
        mBlock.pb8x8.mvdX2[list][partNo] = readMVD(0, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
                mbX, blk8x8X + 1, blk8x8Y, 1, 1, list)
        mBlock.pb8x8.mvdY2[list][partNo] = readMVD(1, true, tAvb, curMBType, topMBType, partPred, topPred, partPred,
                mbX, blk8x8X + 1, blk8x8Y, 1, 1, list)
        mBlock.pb8x8.mvdX3[list][partNo] = readMVD(0, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
                mbX, blk8x8X, blk8x8Y + 1, 1, 1, list)
        mBlock.pb8x8.mvdY3[list][partNo] = readMVD(1, lAvb, true, leftMBType, curMBType, leftPred, partPred, partPred,
                mbX, blk8x8X, blk8x8Y + 1, 1, 1, list)
        mBlock.pb8x8.mvdX4[list][partNo] = readMVD(0, true, true, curMBType, curMBType, partPred, partPred, partPred,
                mbX, blk8x8X + 1, blk8x8Y + 1, 1, 1, list)
        mBlock.pb8x8.mvdY4[list][partNo] = readMVD(1, true, true, curMBType, curMBType, partPred, partPred, partPred,
                mbX, blk8x8X + 1, blk8x8Y + 1, 1, 1, list)
    }

    fun readIntraNxN(mBlock: MBlock) {
        val mbX = mapper.getMbX(mBlock.mbIdx)
        val mbY = mapper.getMbY(mBlock.mbIdx)
        val leftAvailable = mapper.leftAvailable(mBlock.mbIdx)
        val topAvailable = mapper.topAvailable(mBlock.mbIdx)
        mBlock.transform8x8Used = false
        if (transform8x8) {
            mBlock.transform8x8Used = readTransform8x8Flag(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                    tf8x8Left, tf8x8Top[mbX])
        }
        if (!mBlock.transform8x8Used) {
            for (i in 0..15) {
                val blkX = H264Const.MB_BLK_OFF_LEFT[i]
                val blkY = H264Const.MB_BLK_OFF_TOP[i]
                mBlock.lumaModes[i] = readPredictionI4x4Block(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        blkX, blkY, mbX)
            }
        } else {
            for (i in 0..3) {
                val blkX = i and 1 shl 1
                val blkY = i and 2
                mBlock.lumaModes[i] = readPredictionI4x4Block(leftAvailable, topAvailable, leftMBType, topMBType[mbX],
                        blkX, blkY, mbX)
                i4x4PredLeft[blkY + 1] = i4x4PredLeft[blkY]
                i4x4PredTop[(mbX shl 2) + blkX + 1] = i4x4PredTop[(mbX shl 2) + blkX]
            }
        }
        mBlock.chromaPredictionMode = readChromaPredMode(mbX, leftAvailable, topAvailable)
        mBlock._cbp = readCodedBlockPatternIntra(leftAvailable, topAvailable, leftCBPLuma or (leftCBPChroma shl 4),
                topCBPLuma[mbX] or (topCBPChroma[mbX] shl 4), leftMBType, topMBType[mbX])
        if (mBlock.cbpLuma() > 0 || mBlock.cbpChroma() > 0) {
            mBlock.mbQPDelta = readMBQpDelta(mBlock.prevMbType)
        }
        readResidualLuma(mBlock, leftAvailable, topAvailable, mbX, mbY)
        if (chromaFormat != ColorSpace.MONO) {
            readChromaResidual(mBlock, leftAvailable, topAvailable, mbX)
        }
    }

    fun readResidualLuma(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int) {
        if (!mBlock.transform8x8Used) {
            readLuma(mBlock, leftAvailable, topAvailable, mbX, mbY)
        } else if (sliceHeader.pps!!.isEntropyCodingModeFlag) {
            readLuma8x8CABAC(mBlock, mbX, mbY)
        } else {
            readLuma8x8CAVLC(mBlock, leftAvailable, topAvailable, mbX, mbY)
        }
    }

    private fun readLuma(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int) {
        for (i in 0..15) {
            val blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i]
            val blkOffTop = H264Const.MB_BLK_OFF_TOP[i]
            val blkX = (mbX shl 2) + blkOffLeft
            val blkY = (mbY shl 2) + blkOffTop
            if (mBlock.cbpLuma() and (1 shl (i shr 2)) == 0) {
                if (!sliceHeader.pps!!.isEntropyCodingModeFlag) setZeroCoeff(0, blkX, blkOffTop)
                continue
            }
            mBlock.nCoeff[i] = readResidualAC(leftAvailable, topAvailable, mbX, mBlock.curMbType, mBlock.cbpLuma(),
                    blkOffLeft, blkOffTop, blkX, blkY, mBlock.ac[0][i])
        }
        savePrevCBP(mBlock._cbp)
    }

    private fun readLuma8x8CABAC(mBlock: MBlock, mbX: Int, mbY: Int) {
        for (i in 0..3) {
            val blkOffLeft = i and 1 shl 1
            val blkOffTop = i and 2
            val blkX = (mbX shl 2) + blkOffLeft
            val blkY = (mbY shl 2) + blkOffTop
            if (mBlock.cbpLuma() and (1 shl i) == 0) {
                continue
            }
            val nCoeff = readLumaAC8x8(blkX, blkY, mBlock.ac[0][i])
            val blk4x4Offset = i shl 2
            mBlock.nCoeff[blk4x4Offset + 3] = nCoeff
            mBlock.nCoeff[blk4x4Offset + 2] = mBlock.nCoeff[blk4x4Offset + 3]
            mBlock.nCoeff[blk4x4Offset + 1] = mBlock.nCoeff[blk4x4Offset + 2]
            mBlock.nCoeff[blk4x4Offset] = mBlock.nCoeff[blk4x4Offset + 1]
        }
        savePrevCBP(mBlock._cbp)
    }

    private fun readLuma8x8CAVLC(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, mbY: Int) {
        for (i in 0..3) {
            val blk8x8OffLeft = i and 1 shl 1
            val blk8x8OffTop = i and 2
            val blkX = (mbX shl 2) + blk8x8OffLeft
            val blkY = (mbY shl 2) + blk8x8OffTop
            if (mBlock.cbpLuma() and (1 shl i) == 0) {
                setZeroCoeff(0, blkX, blk8x8OffTop)
                setZeroCoeff(0, blkX + 1, blk8x8OffTop)
                setZeroCoeff(0, blkX, blk8x8OffTop + 1)
                setZeroCoeff(0, blkX + 1, blk8x8OffTop + 1)
                continue
            }
            var coeffs = 0
            for (j in 0..3) {
                val ac16 = IntArray(16)
                val blkOffLeft = blk8x8OffLeft + (j and 1)
                val blkOffTop = blk8x8OffTop + (j shr 1)
                coeffs += readLumaAC(leftAvailable, topAvailable, mbX, mBlock.curMbType, blkX, j, ac16, blkOffLeft,
                        blkOffTop)
                for (k in 0..15) mBlock.ac[0][i][CoeffTransformer.zigzag8x8[(k shl 2) + j]] = ac16[k]
            }
            val blk4x4Offset = i shl 2
            mBlock.nCoeff[blk4x4Offset + 3] = coeffs
            mBlock.nCoeff[blk4x4Offset + 2] = mBlock.nCoeff[blk4x4Offset + 3]
            mBlock.nCoeff[blk4x4Offset + 1] = mBlock.nCoeff[blk4x4Offset + 2]
            mBlock.nCoeff[blk4x4Offset] = mBlock.nCoeff[blk4x4Offset + 1]
        }
    }

    fun readChromaResidual(mBlock: MBlock, leftAvailable: Boolean, topAvailable: Boolean, mbX: Int) {
        if (mBlock.cbpChroma() != 0) {
            if (mBlock.cbpChroma() and 3 > 0) {
                readChromaDC(mbX, leftAvailable, topAvailable, mBlock.dc1, 1, mBlock.curMbType)
                readChromaDC(mbX, leftAvailable, topAvailable, mBlock.dc2, 2, mBlock.curMbType)
            }
            _readChromaAC(leftAvailable, topAvailable, mbX, mBlock.dc1, 1, mBlock.curMbType,
                    mBlock.cbpChroma() and 2 > 0, mBlock.ac[1])
            _readChromaAC(leftAvailable, topAvailable, mbX, mBlock.dc2, 2, mBlock.curMbType,
                    mBlock.cbpChroma() and 2 > 0, mBlock.ac[2])
        } else if (!sliceHeader.pps!!.isEntropyCodingModeFlag) {
            setZeroCoeff(1, mbX shl 1, 0)
            setZeroCoeff(1, (mbX shl 1) + 1, 1)
            setZeroCoeff(2, mbX shl 1, 0)
            setZeroCoeff(2, (mbX shl 1) + 1, 1)
        }
    }

    private fun _readChromaAC(leftAvailable: Boolean, topAvailable: Boolean, mbX: Int, dc: IntArray, comp: Int,
                              curMbType: MBType?, codedAC: Boolean, residualOut: Array<IntArray>) {
        for (i in dc.indices) {
            val ac = residualOut[i]
            val blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i]
            val blkOffTop = H264Const.MB_BLK_OFF_TOP[i]
            val blkX = (mbX shl 1) + blkOffLeft
            if (codedAC) {
                readChromaAC(leftAvailable, topAvailable, mbX, comp, curMbType, ac, blkOffLeft, blkOffTop, blkX)
            } else {
                if (!sliceHeader.pps!!.isEntropyCodingModeFlag) setZeroCoeff(comp, blkX, blkOffTop)
            }
        }
    }

    private fun readIPCM(mBlock: MBlock) {
        reader.align()
        for (i in 0..255) {
            mBlock.ipcm.samplesLuma[i] = reader.readNBit(8)
        }
        val MbWidthC = 16 shr chromaFormat!!.compWidth[1]
        val MbHeightC = 16 shr chromaFormat!!.compHeight[1]
        for (i in 0 until 2 * MbWidthC * MbHeightC) {
            mBlock.ipcm.samplesChroma[i] = reader.readNBit(8)
        }
    }

    fun readMBlock(mBlock: MBlock, sliceType: SliceType?) {
        if (sliceType == SliceType.I) {
            readMBlockI(mBlock)
        } else if (sliceType == SliceType.P) {
            readMBlockP(mBlock)
        } else {
            readMBlockB(mBlock)
        }
        val mbX = mapper.getMbX(mBlock.mbIdx)
        leftCBPLuma = mBlock.cbpLuma()
        topCBPLuma[mbX] = leftCBPLuma
        leftCBPChroma = mBlock.cbpChroma()
        topCBPChroma[mbX] = leftCBPChroma
        tf8x8Top[mbX] = mBlock.transform8x8Used
        tf8x8Left = tf8x8Top[mbX]
    }

    private fun readMBlockI(mBlock: MBlock) {
        mBlock.mbType = decodeMBTypeI(mBlock.mbIdx, mapper.leftAvailable(mBlock.mbIdx),
                mapper.topAvailable(mBlock.mbIdx), leftMBType, topMBType[mapper.getMbX(mBlock.mbIdx)])
        readMBlockIInt(mBlock, mBlock.mbType)
    }

    private fun readMBlockIInt(mBlock: MBlock, mbType: Int) {
        if (mbType == 0) {
            mBlock.curMbType = MBType.I_NxN
            readIntraNxN(mBlock)
        } else if (mbType >= 1 && mbType <= 24) {
            mBlock.curMbType = MBType.I_16x16
            readIntra16x16(mbType - 1, mBlock)
        } else {
            Logger.warn("IPCM macroblock found. Not tested, may cause unpredictable behavior.")
            mBlock.curMbType = MBType.I_PCM
            readIPCM(mBlock)
        }
    }

    private fun readMBlockP(mBlock: MBlock) {
        mBlock.mbType = readMBTypeP()
        when (mBlock.mbType) {
            0 -> {
                mBlock.curMbType = MBType.P_16x16
                readInter16x16(PartPred.L0, mBlock)
            }
            1 -> {
                mBlock.curMbType = MBType.P_16x8
                readInter16x8(PartPred.L0, PartPred.L0, mBlock)
            }
            2 -> {
                mBlock.curMbType = MBType.P_8x16
                readIntra8x16(PartPred.L0, PartPred.L0, mBlock)
            }
            3 -> {
                mBlock.curMbType = MBType.P_8x8
                readMBlock8x8(mBlock)
            }
            4 -> {
                mBlock.curMbType = MBType.P_8x8ref0
                readMBlock8x8(mBlock)
            }
            else -> readMBlockIInt(mBlock, mBlock.mbType - 5)
        }
    }

    private fun readMBlockB(mBlock: MBlock) {
        mBlock.mbType = readMBTypeB(mBlock.mbIdx, mapper.leftAvailable(mBlock.mbIdx),
                mapper.topAvailable(mBlock.mbIdx), leftMBType, topMBType[mapper.getMbX(mBlock.mbIdx)])
        if (mBlock.mbType >= 23) {
            readMBlockIInt(mBlock, mBlock.mbType - 23)
        } else {
            mBlock.curMbType = H264Const.bMbTypes[mBlock.mbType]
            if (mBlock.mbType == 0) {
                readMBlockBDirect(mBlock)
            } else if (mBlock.mbType <= 3) {
                readInter16x16(H264Const.bPredModes[mBlock.mbType][0], mBlock)
            } else if (mBlock.mbType == 22) {
                readMBlock8x8(mBlock)
            } else if (mBlock.mbType and 1 == 0) {
                readInter16x8(H264Const.bPredModes[mBlock.mbType][0], H264Const.bPredModes[mBlock.mbType][1], mBlock)
            } else {
                readIntra8x16(H264Const.bPredModes[mBlock.mbType][0], H264Const.bPredModes[mBlock.mbType][1], mBlock)
            }
        }
    }

    init {
        val mbWidth = sliceHeader.sps!!.picWidthInMbsMinus1 + 1
        topMBType = arrayOfNulls(mbWidth)
        topCBPLuma = IntArray(mbWidth)
        topCBPChroma = IntArray(mbWidth)
        chromaFormat = sliceHeader.sps!!.chromaFormatIdc
        transform8x8 = if (sliceHeader.pps!!.extended == null) false else sliceHeader.pps!!.extended!!.isTransform8x8ModeFlag
        numRef = if (sliceHeader.numRefIdxActiveOverrideFlag) intArrayOf(sliceHeader.numRefIdxActiveMinus1[0] + 1, sliceHeader.numRefIdxActiveMinus1[1] + 1) else intArrayOf(sliceHeader.pps!!.numRefIdxActiveMinus1[0] + 1, sliceHeader.pps!!.numRefIdxActiveMinus1[1] + 1)
        tf8x8Top = BooleanArray(mbWidth)
        predModeLeft = arrayOfNulls(2)
        predModeTop = arrayOfNulls(mbWidth shl 1)
        i4x4PredLeft = IntArray(4)
        i4x4PredTop = IntArray(mbWidth shl 2)
    }
}