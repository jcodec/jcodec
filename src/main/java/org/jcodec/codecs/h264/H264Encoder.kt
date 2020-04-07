package org.jcodec.codecs.h264

import org.jcodec.codecs.h264.H264Utils.escapeNAL
import org.jcodec.codecs.h264.decode.Intra16x16PredictionBuilder.lumaPredSAD
import org.jcodec.codecs.h264.encode.*
import org.jcodec.codecs.h264.encode.MBEncoderHelper.putBlkPic
import org.jcodec.codecs.h264.encode.MBEncoderHelper.take
import org.jcodec.codecs.h264.io.CAVLC
import org.jcodec.codecs.h264.io.model.*
import org.jcodec.codecs.h264.io.write.CAVLCWriter.writeUE
import org.jcodec.codecs.h264.io.write.SliceHeaderWriter.write
import org.jcodec.common.VideoEncoder
import org.jcodec.common.io.BitWriter
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Size
import org.jcodec.common.tools.MathUtil
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * MPEG 4 AVC ( H.264 ) Encoder
 *
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 *
 * @author The JCodec project
 */
class H264Encoder(private val rc: RateControl) : VideoEncoder() {
    private var frameNumber = 0
    var keyInterval: Int
    var motionSearchRange: Int
    private var maxPOC = 0
    private var maxFrameNumber = 0
    private var sps: SeqParameterSet? = null
    private var pps: PictureParameterSet? = null
    private var mbEncoderI16x16: MBWriterI16x16? = null
    private var mbEncoderP16x16: MBWriterP16x16? = null
    private var ref: Picture? = null
    private var picOut: Picture? = null
    private var topEncoded: Array<EncodedMB?>? = null
    var isPsnrEn = false
    private val sum_se = LongArray(3)
    private val g_sum_se = LongArray(3)
    private var frameCount = 0
    private var totalSize: Long = 0
    private var context: EncodingContext? = null
    private var decoder: H264Decoder? = null
    private var enableRdo = false

    fun setEncDecMismatch(test: Boolean) {
        this.decoder = H264Decoder()
    }

    fun setEnableRdo(enableRdo: Boolean) {
        this.enableRdo = enableRdo
    }

    /**
     * Encode this picture into h.264 frame. Frame type will be selected by encoder.
     */
    override fun encodeFrame(pic: Picture, _out: ByteBuffer): EncodedFrame {
        require(pic.color == ColorSpace.YUV420J) { "Input picture color is not supported: " + pic.color }
        if (frameNumber >= keyInterval) {
            frameNumber = 0
        }
        val sliceType = if (frameNumber == 0) SliceType.I else SliceType.P
        val idr = frameNumber == 0
        val data = doEncodeFrame(pic, _out, idr, frameNumber++, sliceType)
        if (isPsnrEn) {
            savePsnrStats(data.remaining())
        }
        if (decoder != null) checkEncDecMatch(data)
        frameCount++
        return EncodedFrame(data, idr)
    }

    private fun checkEncDecMatch(data: ByteBuffer) {
        val tmp = picOut!!.createCompatible()
        val decoded = decoder!!.decodeFrame(data.duplicate(), tmp.data)
        decoded.crop = null
        decoded.color = picOut!!.color
        val mm = decoded.firstMismatch(picOut)
        if (mm != null) {
            val cw = 3 + if (mm.v2 == 0) 1 else 0
            throw RuntimeException(String.format("Encoder-decoder mismatch %d vs %d, f:%d pl:%d x:%d y:%d mbX:%d mbY:%d",
                    decoded.pixAt(mm.v0, mm.v1, mm.v2), picOut!!.pixAt(mm.v0, mm.v1, mm.v2), frameCount, mm.v2,
                    mm.v0, mm.v1, mm.v0 shr cw, mm.v1 shr cw))
        }
    }

    private fun savePsnrStats(size: Int) {
        for (p in 0..2) {
            g_sum_se[p] += sum_se[p]
            sum_se[p] = 0
        }
        totalSize += size.toLong()
    }

    private fun calcPsnr(sum: Long, p: Int): Double {
        val luma = if (p == 0) 1 else 0
        val pixCnt = (sps!!.picHeightInMapUnitsMinus1 + 1) * (sps!!.picWidthInMbsMinus1 + 1) shl 6 + luma * 2
        val mse = sum.toDouble() / pixCnt
        return 10 * Math.log10(255 * 255 / mse)
    }

    /**
     * Encode this picture as an IDR frame. IDR frame starts a new independently
     * decodeable video sequence
     *
     * @param pic
     * @param _out
     * @return
     */
    fun encodeIDRFrame(pic: Picture, _out: ByteBuffer): ByteBuffer {
        frameNumber = 0
        return doEncodeFrame(pic, _out, true, frameNumber, SliceType.I)
    }

    /**
     * Encode this picture as a P-frame. P-frame is an frame predicted from one or
     * more of the previosly decoded frame and is usually 10x less in size then the
     * IDR frame.
     *
     * @param pic
     * @param _out
     * @return
     */
    fun encodePFrame(pic: Picture, _out: ByteBuffer): ByteBuffer {
        frameNumber++
        return doEncodeFrame(pic, _out, true, frameNumber, SliceType.P)
    }

    fun doEncodeFrame(pic: Picture, _out: ByteBuffer, idr: Boolean, frameNumber: Int, frameType: SliceType): ByteBuffer {
        val dup = _out.duplicate()
        var maxSize = Math.min(dup.remaining(), pic.width * pic.height)
        maxSize -= maxSize ushr 6 // 1.5% to account for escaping
        val qp = rc.startPicture(pic.size, maxSize, frameType)
        if (idr) {
            sps = initSPS(Size(pic.croppedWidth, pic.croppedHeight))
            pps = initPPS()
            maxPOC = 1 shl sps!!.log2MaxPicOrderCntLsbMinus4 + 4
            maxFrameNumber = 1 shl sps!!.log2MaxFrameNumMinus4 + 4
        }
        if (idr) {
            dup.putInt(0x1)
            NALUnit(NALUnitType.SPS, 3).write(dup)
            writeSPS(dup, sps)
            dup.putInt(0x1)
            NALUnit(NALUnitType.PPS, 3).write(dup)
            writePPS(dup, pps)
        }
        val mbWidth = sps!!.picWidthInMbsMinus1 + 1
        val mbHeight = sps!!.picHeightInMapUnitsMinus1 + 1
        context = EncodingContext(mbWidth, mbHeight)
        picOut = Picture.create(mbWidth shl 4, mbHeight shl 4, ColorSpace.YUV420J)
        topEncoded = arrayOfNulls(mbWidth)
        encodeSlice(sps, pps, pic, dup, idr, frameNumber, frameType, qp)
        putLastMBLine()
        ref = picOut
        dup.flip()
        return dup
    }

    private fun writePPS(dup: ByteBuffer, pps: PictureParameterSet?) {
        val tmp = ByteBuffer.allocate(1024)
        pps!!.write(tmp)
        tmp.flip()
        escapeNAL(tmp, dup)
    }

    private fun writeSPS(dup: ByteBuffer, sps: SeqParameterSet?) {
        val tmp = ByteBuffer.allocate(1024)
        sps!!.write(tmp)
        tmp.flip()
        escapeNAL(tmp, dup)
    }

    fun initPPS(): PictureParameterSet {
        val pps = PictureParameterSet()
        pps.picInitQpMinus26 = 0 // start with qp = 26
        return pps
    }

    fun initSPS(sz: Size): SeqParameterSet {
        val sps = SeqParameterSet()
        sps.picWidthInMbsMinus1 = (sz.width + 15 shr 4) - 1
        sps.picHeightInMapUnitsMinus1 = (sz.height + 15 shr 4) - 1
        sps.chromaFormatIdc = ColorSpace.YUV420J
        sps.profileIdc = 66
        sps.levelIdc = 40
        sps.numRefFrames = 1
        sps.isFrameMbsOnlyFlag = true
        sps.log2MaxFrameNumMinus4 = Math.max(0, MathUtil.log2(keyInterval) - 3)
        val codedWidth = sps.picWidthInMbsMinus1 + 1 shl 4
        val codedHeight = sps.picHeightInMapUnitsMinus1 + 1 shl 4
        sps.isFrameCroppingFlag = codedWidth != sz.width || codedHeight != sz.height
        sps.frameCropRightOffset = codedWidth - sz.width + 1 shr 1
        sps.frameCropBottomOffset = codedHeight - sz.height + 1 shr 1
        return sps
    }

    private fun encodeSlice(sps: SeqParameterSet?, pps: PictureParameterSet?, pic: Picture, dup: ByteBuffer, idr: Boolean,
                            frameNum: Int, sliceType: SliceType, sliceQp: Int) {
        var idr = idr
        var sliceQp = sliceQp
        if (idr && sliceType != SliceType.I) {
            idr = false
            Logger.warn("Illegal value of idr = true when sliceType != I")
        }
        context!!.cavlc = arrayOf(CAVLC(sps!!, pps, 2, 2), CAVLC(sps, pps, 1, 1), CAVLC(sps, pps, 1, 1))
        mbEncoderI16x16 = MBWriterI16x16()
        mbEncoderP16x16 = MBWriterP16x16(sps, ref)
        dup.putInt(0x1)
        NALUnit(if (idr) NALUnitType.IDR_SLICE else NALUnitType.NON_IDR_SLICE, 3).write(dup)
        val sh = SliceHeader()
        sh.sliceType = sliceType
        if (idr) sh.refPicMarkingIDR = RefPicMarkingIDR(false, false)
        sh.pps = pps
        sh.sps = sps
        sh.picOrderCntLsb = (frameNum shl 1) % maxPOC
        sh.frameNum = frameNum % maxFrameNumber
        sh.sliceQpDelta = sliceQp - (pps!!.picInitQpMinus26 + 26)
        var buf = ByteBuffer.allocate(pic.width * pic.height)
        var sliceData = BitWriter(buf)
        write(sh, idr, 2, sliceData)
        val estimator = MotionEstimator(ref, sps, motionSearchRange)
        context!!.prevQp = sliceQp
        val mbWidth = sps.picWidthInMbsMinus1 + 1
        val mbHeight = sps.picHeightInMapUnitsMinus1 + 1
        var mbY = 0
        var mbAddr = 0
        while (mbY < mbHeight) {
            var mbX = 0
            while (mbX < mbWidth) {
                if (sliceType == SliceType.P) {
                    writeUE(sliceData, 0) // number of skipped mbs
                }
                var mv: IntArray? = null
                if (ref != null) mv = estimator.mvEstimate(pic, mbX, mbY)
                val params = NonRdVector(mv, getLumaMode(pic, context, mbX, mbY), 0)
                val outMB = EncodedMB()
                outMB.setPos(mbX, mbY)
                var candidate: BitWriter
                var fork: EncodingContext
                var totalQpDelta = 0
                var qpDelta = rc.initialQpDelta()
                do {
                    candidate = sliceData.fork()
                    fork = context!!.fork()
                    totalQpDelta += qpDelta
                    rdMacroblock(fork, outMB, sliceType, pic, mbX, mbY, candidate, sliceQp, sliceQp + totalQpDelta, params)
                    qpDelta = rc.accept(candidate.position() - sliceData.position())
                } while (qpDelta != 0)
                estimator.mvSave(mbX, mbY, intArrayOf(outMB.mx[0], outMB.my[0], outMB.mr[0]))
                sliceData = candidate
                context = fork
                sliceQp += totalQpDelta
                context!!.update(outMB)
                if (isPsnrEn) calcMse(pic, outMB, mbX, mbY, sum_se)
                MBDeblocker().deblockMBP(outMB, if (mbX > 0) topEncoded!![mbX - 1] else null,
                        if (mbY > 0) topEncoded!![mbX] else null)
                addToReference(outMB, mbX, mbY)
                mbX++
                mbAddr++
            }
            mbY++
        }
        sliceData.write1Bit(1)
        sliceData.flush()
        buf = sliceData.buffer
        buf.flip()
        escapeNAL(buf, dup)
    }

    private fun getLumaMode(pic: Picture, ctx: EncodingContext?, mbX: Int, mbY: Int): Int {
        val patch = ByteArray(256)
        take(pic.getPlaneData(0), pic.getPlaneWidth(0), pic.getPlaneHeight(0), mbX shl 4, mbY shl 4,
                patch, 16, 16)
        var minSad = Int.MAX_VALUE
        var predMode = -1
        for (predType in 0..3) {
            val sad = lumaPredSAD(predType, mbX != 0, mbY != 0, ctx!!.leftRow[0],
                    ctx.topLine[0], ctx.topLeft[0], mbX shl 4, patch)
            if (sad < minSad) {
                minSad = sad
                predMode = predType
            }
        }
        return predMode
    }

    private fun calcMse(pic: Picture, out: EncodedMB, mbX: Int, mbY: Int, out_se: LongArray) {
        val patch = ByteArray(256)
        for (p in 0..2) {
            val outPix = out.pixels.data[p]
            val luma = if (p == 0) 1 else 0
            take(pic.getPlaneData(p), pic.getPlaneWidth(p), pic.getPlaneHeight(p), mbX shl 3 + luma,
                    mbY shl 3 + luma, patch, 8 shl luma, 8 shl luma)
            for (i in 0 until (64 shl luma * 2)) {
                val q = outPix[i] - patch[i]
                out_se[p] = out_se[p] + q * q
            }
        }
    }

    class RdVector(var mbType: MBType, var qp: Int)

    class NonRdVector(var mv: IntArray?, var lumaPred: Int, var chrPred: Int)

    private fun rdMacroblock(ctx: EncodingContext, outMB: EncodedMB, sliceType: SliceType, pic: Picture, mbX: Int, mbY: Int,
                             candidate: BitWriter, sliceQp: Int, mbQp: Int, params: NonRdVector) {
        if (!enableRdo) {
            val vector = if (sliceType == SliceType.P) RdVector(MBType.P_16x16, sliceQp) else RdVector(MBType.I_16x16, Math.max(sliceQp - 8, 12))
            encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, sliceQp, params, vector)
            return
        }
        val cands: MutableList<RdVector> = LinkedList()
        cands.add(RdVector(MBType.I_16x16, Math.max(sliceQp - 8, 12)))
        if (sliceType == SliceType.P) {
            cands.add(RdVector(MBType.P_16x16, sliceQp))
        }
        var bestRd = Long.MAX_VALUE
        var bestVector: RdVector? = null
        for (rdVector in cands) {
            val candCtx = ctx.fork()
            val candBits = candidate.fork()
            val rdCost = tryVector(candCtx, sliceType, pic, mbX, mbY, candBits, sliceQp, params, rdVector)
            if (rdCost < bestRd) {
                bestRd = rdCost
                bestVector = rdVector
            }
        }
        encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, sliceQp, params, bestVector)
    }

    private fun tryVector(ctx: EncodingContext, sliceType: SliceType, pic: Picture, mbX: Int, mbY: Int, candidate: BitWriter,
                          sliceQp: Int, params: NonRdVector, vector: RdVector): Long {
        val start = candidate.position()
        val outMB = EncodedMB()
        outMB.setPos(mbX, mbY)
        encodeCand(ctx, outMB, sliceType, pic, mbX, mbY, candidate, sliceQp, params, vector)
        val se = LongArray(3)
        calcMse(pic, outMB, mbX, mbY, se)
        val mse = (se[0] + se[1] + se[2]) / 384
        val bits = candidate.position() - start
        return rdCost(mse, bits, H264Const.lambda[sliceQp])
    }

    private fun rdCost(mse: Long, bits: Int, lambda: Int): Long {
        return mse + (lambda * bits shr 8)
    }

    private fun encodeCand(ctx: EncodingContext, outMB: EncodedMB, sliceType: SliceType, pic: Picture, mbX: Int, mbY: Int,
                           candidate: BitWriter, sliceQp: Int, params: NonRdVector, vector: RdVector?) {
        if (vector!!.mbType == MBType.I_16x16) {
            val cbpChroma = mbEncoderI16x16!!.getCbpChroma(pic, mbX, mbY)
            val cbpLuma = mbEncoderI16x16!!.getCbpLuma(pic, mbX, mbY)
            val i16x16TypeOffset = cbpLuma / 15 * 12 + cbpChroma * 4 + params.lumaPred
            val mbTypeOffset = if (sliceType == SliceType.P) 5 else 0
            writeUE(candidate, mbTypeOffset + vector.mbType.code() + i16x16TypeOffset)
        } else {
            writeUE(candidate, vector.mbType.code())
        }
        if (vector.mbType == MBType.I_16x16) {
            mbEncoderI16x16!!.encodeMacroblock(ctx, pic, mbX, mbY, candidate, outMB, vector.qp, params)
        } else if (vector.mbType == MBType.P_16x16) {
            mbEncoderP16x16!!.encodeMacroblock(ctx, pic, mbX, mbY, candidate, outMB, vector.qp, params)
        } else throw RuntimeException("Macroblock of type " + vector.mbType + " is not supported.")
    }

    private fun addToReference(outMB: EncodedMB, mbX: Int, mbY: Int) {
        if (mbY > 0) putBlkPic(picOut!!, topEncoded!![mbX]!!.pixels, mbX shl 4, mbY - 1 shl 4)
        topEncoded!![mbX] = outMB
    }

    private fun putLastMBLine() {
        val mbWidth = sps!!.picWidthInMbsMinus1 + 1
        val mbHeight = sps!!.picHeightInMapUnitsMinus1 + 1
        for (mbX in 0 until mbWidth) putBlkPic(picOut!!, topEncoded!![mbX]!!.pixels, mbX shl 4, mbHeight - 1 shl 4)
    }

    override fun getSupportedColorSpaces(): Array<ColorSpace> {
        return arrayOf(ColorSpace.YUV420J)
    }

    override fun estimateBufferSize(frame: Picture): Int {
        return Math.max(1 shl 16, frame.width * frame.height)
    }

    override fun finish() {
        if (isPsnrEn) {
            val fc = frameCount + 1
            val avgSum = (g_sum_se[0] + g_sum_se[1] * 4 + g_sum_se[2] * 4) / 3
            val avgPsnr = calcPsnr(avgSum / fc, 0)
            val yPsnr = calcPsnr(g_sum_se[0] / fc, 0)
            val uPsnr = calcPsnr(g_sum_se[1] / fc, 1)
            val vPsnr = calcPsnr(g_sum_se[2] / fc, 2)
            Logger.info(String.format("PSNR AVG:%.3f Y:%.3f U:%.3f V:%.3f kbps:%.3f", avgPsnr, yPsnr, uPsnr, vPsnr,
                    (8 * 25 * (totalSize / fc)).toDouble() / 1000))
        }
    }

    companion object {
        // private static final int QP = 20;
        private const val KEY_INTERVAL_DEFAULT = 25
        private const val MOTION_SEARCH_RANGE_DEFAULT = 16
        @JvmStatic
        fun createH264Encoder(): H264Encoder {
            return H264Encoder(CQPRateControl(24))
        }
    }

    init {
        keyInterval = KEY_INTERVAL_DEFAULT
        motionSearchRange = MOTION_SEARCH_RANGE_DEFAULT
    }
}