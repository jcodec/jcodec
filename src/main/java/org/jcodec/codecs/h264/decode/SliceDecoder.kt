package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.h264.H264Const
import org.jcodec.codecs.h264.H264Const.PartPred
import org.jcodec.codecs.h264.decode.MBlockDecoderUtils.debugPrint
import org.jcodec.codecs.h264.decode.aso.MapManager
import org.jcodec.codecs.h264.decode.aso.Mapper
import org.jcodec.codecs.h264.io.model.Frame
import org.jcodec.codecs.h264.io.model.MBType
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.codecs.h264.io.model.SliceType
import org.jcodec.common.IntObjectMap
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Picture

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A decoder for an individual slice
 *
 * @author The JCodec project
 */
class SliceDecoder(private val activeSps: SeqParameterSet, private val sRefs: Array<Frame?>,
                   private val lRefs: IntObjectMap<Frame>, private val di: DeblockerInput, private val frameOut: Frame) {
    private var mapper: Mapper? = null
    private var decoderIntra16x16: MBlockDecoderIntra16x16? = null
    private var decoderIntraNxN: MBlockDecoderIntraNxN? = null
    private var decoderInter: MBlockDecoderInter? = null
    private var decoderInter8x8: MBlockDecoderInter8x8? = null
    private var skipDecoder: MBlockSkipDecoder? = null
    private var decoderBDirect: MBlockDecoderBDirect? = null
    private var refListManager: RefListManager? = null
    private var decoderIPCM: MBlockDecoderIPCM? = null
    private var parser: SliceReader? = null
    private var decoderState: DecoderState? = null
    fun decodeFromReader(sliceReader: SliceReader?) {
        parser = sliceReader
        initContext()
        debugPrint("============%d============= ", frameOut.pOC)
        val refList = refListManager!!.refList
        decodeMacroblocks(refList)
    }

    private fun initContext() {
        val sh = parser!!.sliceHeader
        decoderState = DecoderState(sh)
        mapper = MapManager(sh.sps!!, sh.pps!!).getMapper(sh)
        decoderIntra16x16 = MBlockDecoderIntra16x16(mapper!!, sh, di, frameOut.pOC, decoderState)
        decoderIntraNxN = MBlockDecoderIntraNxN(mapper!!, sh, di, frameOut.pOC, decoderState)
        decoderInter = MBlockDecoderInter(mapper!!, sh, di, frameOut.pOC, decoderState)
        decoderBDirect = MBlockDecoderBDirect(mapper!!, sh, di, frameOut.pOC, decoderState)
        decoderInter8x8 = MBlockDecoderInter8x8(mapper!!, decoderBDirect!!, sh, di, frameOut.pOC, decoderState)
        skipDecoder = MBlockSkipDecoder(mapper!!, decoderBDirect!!, sh, di, frameOut.pOC, decoderState)
        decoderIPCM = MBlockDecoderIPCM(mapper!!, decoderState!!)
        refListManager = RefListManager(sh, sRefs, lRefs, frameOut)
    }

    private fun decodeMacroblocks(refList: Array<Array<Frame?>?>?) {
        val mb = Picture.create(16, 16, activeSps.chromaFormatIdc)
        val mbWidth = activeSps.picWidthInMbsMinus1 + 1
        val mBlock = MBlock(activeSps.chromaFormatIdc!!)
        while (parser!!.readMacroblock(mBlock)) {
            decode(mBlock, parser!!.sliceHeader.sliceType, mb, refList)
            val mbAddr = mapper!!.getAddress(mBlock.mbIdx)
            val mbX = mbAddr % mbWidth
            val mbY = mbAddr / mbWidth
            putMacroblock(frameOut, mb, mbX, mbY)
            di.shs[mbAddr] = parser!!.sliceHeader
            di.refsUsed[mbAddr] = refList
            fillCoeff(mBlock, mbX, mbY)
            mb.fill(0)
            mBlock.clear()
        }
    }

    private fun fillCoeff(mBlock: MBlock, mbX: Int, mbY: Int) {
        for (i in 0..15) {
            val blkOffLeft = H264Const.MB_BLK_OFF_LEFT[i]
            val blkOffTop = H264Const.MB_BLK_OFF_TOP[i]
            val blkX = (mbX shl 2) + blkOffLeft
            val blkY = (mbY shl 2) + blkOffTop
            di.nCoeff[blkY][blkX] = mBlock.nCoeff[i]
        }
    }

    fun decode(mBlock: MBlock, sliceType: SliceType?, mb: Picture, references: Array<Array<Frame?>?>?) {
        if (mBlock.skipped) {
            skipDecoder!!.decodeSkip(mBlock, references, mb, sliceType!!)
        } else if (sliceType == SliceType.I) {
            decodeMBlockI(mBlock, mb)
        } else if (sliceType == SliceType.P) {
            decodeMBlockP(mBlock, mb, references)
        } else {
            decodeMBlockB(mBlock, mb, references)
        }
    }

    private fun decodeMBlockI(mBlock: MBlock, mb: Picture) {
        decodeMBlockIInt(mBlock, mb)
    }

    private fun decodeMBlockIInt(mBlock: MBlock, mb: Picture) {
        if (mBlock.curMbType == MBType.I_NxN) {
            decoderIntraNxN!!.decode(mBlock, mb)
        } else if (mBlock.curMbType == MBType.I_16x16) {
            decoderIntra16x16!!.decode(mBlock, mb)
        } else {
            Logger.warn("IPCM macroblock found. Not tested, may cause unpredictable behavior.")
            decoderIPCM!!.decode(mBlock, mb)
        }
    }

    private fun decodeMBlockP(mBlock: MBlock, mb: Picture, references: Array<Array<Frame?>?>?) {
        if (MBType.P_16x16 == mBlock.curMbType) {
            decoderInter!!.decode16x16(mBlock, mb, references, PartPred.L0)
        } else if (MBType.P_16x8 == mBlock.curMbType) {
            decoderInter!!.decode16x8(mBlock, mb, references, PartPred.L0, PartPred.L0)
        } else if (MBType.P_8x16 == mBlock.curMbType) {
            decoderInter!!.decode8x16(mBlock, mb, references, PartPred.L0, PartPred.L0)
        } else if (MBType.P_8x8 == mBlock.curMbType) {
            decoderInter8x8!!.decode(mBlock, references, mb, SliceType.P, false)
        } else if (MBType.P_8x8ref0 == mBlock.curMbType) {
            decoderInter8x8!!.decode(mBlock, references, mb, SliceType.P, true)
        } else {
            decodeMBlockIInt(mBlock, mb)
        }
    }

    private fun decodeMBlockB(mBlock: MBlock, mb: Picture, references: Array<Array<Frame?>?>?) {
        if (mBlock.curMbType!!.isIntra) {
            decodeMBlockIInt(mBlock, mb)
        } else {
            if (mBlock.curMbType == MBType.B_Direct_16x16) {
                decoderBDirect!!.decode(mBlock, mb, references)
            } else if (mBlock.mbType <= 3) {
                decoderInter!!.decode16x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0])
            } else if (mBlock.mbType == 22) {
                decoderInter8x8!!.decode(mBlock, references, mb, SliceType.B, false)
            } else if (mBlock.mbType and 1 == 0) {
                decoderInter!!.decode16x8(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                        H264Const.bPredModes[mBlock.mbType][1])
            } else {
                decoderInter!!.decode8x16(mBlock, mb, references, H264Const.bPredModes[mBlock.mbType][0],
                        H264Const.bPredModes[mBlock.mbType][1])
            }
        }
    }

    companion object {
        private fun putMacroblock(tgt: Picture, decoded: Picture, mbX: Int, mbY: Int) {
            val luma = tgt.getPlaneData(0)
            val stride = tgt.getPlaneWidth(0)
            val cb = tgt.getPlaneData(1)
            val cr = tgt.getPlaneData(2)
            val strideChroma = tgt.getPlaneWidth(1)
            var dOff = 0
            val mbx16 = mbX * 16
            val mby16 = mbY * 16
            val decodedY = decoded.getPlaneData(0)
            for (i in 0..15) {
                System.arraycopy(decodedY, dOff, luma, (mby16 + i) * stride + mbx16, 16)
                dOff += 16
            }
            val mbx8 = mbX * 8
            val mby8 = mbY * 8
            val decodedCb = decoded.getPlaneData(1)
            val decodedCr = decoded.getPlaneData(2)
            for (i in 0..7) {
                val decodePos = i shl 3
                val chromaPos = (mby8 + i) * strideChroma + mbx8
                System.arraycopy(decodedCb, decodePos, cb, chromaPos, 8)
                System.arraycopy(decodedCr, decodePos, cr, chromaPos, 8)
            }
        }
    }

}