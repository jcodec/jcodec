package org.jcodec.codecs.h264

import org.jcodec.codecs.h264.H264Utils.MvList2D
import org.jcodec.codecs.h264.decode.DeblockerInput
import org.jcodec.codecs.h264.decode.FrameReader
import org.jcodec.codecs.h264.decode.SliceDecoder
import org.jcodec.codecs.h264.decode.SliceHeaderReader.readPart1
import org.jcodec.codecs.h264.decode.SliceReader
import org.jcodec.codecs.h264.decode.deblock.DeblockingFilter
import org.jcodec.codecs.h264.io.model.*
import org.jcodec.codecs.h264.io.model.Frame.Companion.createFrame
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType
import org.jcodec.codecs.h264.io.model.SeqParameterSet.Companion.getPicHeightInMbs
import org.jcodec.common.IntObjectMap
import org.jcodec.common.UsedViaReflection
import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.VideoDecoder
import org.jcodec.common.io.BitReader
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Rect
import org.jcodec.common.tools.MathUtil
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * MPEG 4 AVC ( H.264 ) Decoder
 *
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 *
 * @author The JCodec project
 */
class H264Decoder : VideoDecoder() {
    private var sRefs: Array<Frame?>? = null
    private var lRefs: IntObjectMap<Frame>? = null
    private val pictureBuffer: MutableList<Frame>
    private val poc: POCManager
    private val reader: FrameReader
    private var tp: ExecutorService? = null
    private val threaded: Boolean
    override fun decodeFrame(data: ByteBuffer, buffer: Array<ByteArray>): Frame {
        return decodeFrameFromNals(H264Utils.splitFrame(data), buffer.map { it as ByteArray? }.toTypedArray())
    }

    fun decodeFrameFromNals(nalUnits: List<ByteBuffer?>?, buffer: Array<ByteArray?>?): Frame {
        return FrameDecoder(this).decodeFrame(nalUnits, buffer)!!
    }

    private class SliceDecoderRunnable(private val fdec: FrameDecoder, private val sliceReader: SliceReader, private val result: Frame) : Runnable {
        override fun run() {
            SliceDecoder(fdec.activeSps!!, fdec.dec.sRefs!!, fdec.dec.lRefs!!, fdec.di!!, result)
                    .decodeFromReader(sliceReader)
        }

    }

    internal class FrameDecoder(val dec: H264Decoder) {
        var activeSps: SeqParameterSet? = null
        private var filter: DeblockingFilter? = null
        private var firstSliceHeader: SliceHeader? = null
        private var firstNu: NALUnit? = null
        var di: DeblockerInput? = null
        fun decodeFrame(nalUnits: List<ByteBuffer?>?, buffer: Array<ByteArray?>?): Frame? {
            val sliceReaders = dec.reader.readFrame(nalUnits!!)
            if (sliceReaders == null || sliceReaders.size == 0) return null
            val result = init(sliceReaders[0], buffer)
            if (dec.threaded && sliceReaders.size > 1) {
                val futures: MutableList<Future<*>> = ArrayList()
                for (sliceReader in sliceReaders) {
                    futures.add(dec.tp!!.submit(SliceDecoderRunnable(this, sliceReader, result)))
                }
                for (future in futures) {
                    waitForSure(future)
                }
            } else {
                for (sliceReader in sliceReaders) {
                    SliceDecoder(activeSps!!, dec.sRefs!!, dec.lRefs!!, di!!, result).decodeFromReader(sliceReader)
                }
            }
            filter!!.deblockFrame(result)
            updateReferences(result)
            return result
        }

        private fun waitForSure(future: Future<*>) {
            while (true) {
                try {
                    future.get()
                    break
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }

        private fun updateReferences(picture: Frame) {
            if (firstNu!!.nal_ref_idc != 0) {
                if (firstNu!!.type == NALUnitType.IDR_SLICE) {
                    performIDRMarking(firstSliceHeader!!.refPicMarkingIDR, picture)
                } else {
                    performMarking(firstSliceHeader!!.refPicMarkingNonIDR, picture)
                }
            }
        }

        private fun init(sliceReader: SliceReader, buffer: Array<ByteArray?>?): Frame {
            firstNu = sliceReader.nALUnit
            firstSliceHeader = sliceReader.sliceHeader
            activeSps = firstSliceHeader!!.sps
            validateSupportedFeatures(firstSliceHeader!!.sps, firstSliceHeader!!.pps)
            val picWidthInMbs = activeSps!!.picWidthInMbsMinus1 + 1
            if (dec.sRefs == null) {
                dec.sRefs = arrayOfNulls(1 shl firstSliceHeader!!.sps!!.log2MaxFrameNumMinus4 + 4)
                dec.lRefs = IntObjectMap()
            }
            di = DeblockerInput(activeSps!!)
            val result = createFrame(activeSps, buffer, firstSliceHeader!!.frameNum, firstSliceHeader!!.sliceType,
                    di!!.mvs, di!!.refsUsed, dec.poc.calcPOC(firstSliceHeader!!, firstNu!!))
            filter = DeblockingFilter(picWidthInMbs, activeSps!!.bitDepthChromaMinus8 + 8, di!!)
            return result
        }

        private fun validateSupportedFeatures(sps: SeqParameterSet?, pps: PictureParameterSet?) {
            if (sps!!.isMbAdaptiveFrameFieldFlag) throw RuntimeException("Unsupported h264 feature: MBAFF.")
            if (sps.bitDepthLumaMinus8 != 0 || sps.bitDepthChromaMinus8 != 0) throw RuntimeException("Unsupported h264 feature: High bit depth.")
            if (sps.chromaFormatIdc != ColorSpace.YUV420J) throw RuntimeException("Unsupported h264 feature: " + sps.chromaFormatIdc + " color.")
            if (!sps.isFrameMbsOnlyFlag || sps.isFieldPicFlag) throw RuntimeException("Unsupported h264 feature: interlace.")
            if (pps!!.isConstrainedIntraPredFlag) throw RuntimeException("Unsupported h264 feature: constrained intra prediction.")
            //            if (sps.getScalingMatrix() != null || pps.extended != null && pps.extended.getScalingMatrix() != null)
//                throw new RuntimeException("Unsupported h264 feature: scaling list.");
            if (sps.isQpprimeYZeroTransformBypassFlag) throw RuntimeException("Unsupported h264 feature: qprime zero transform bypass.")
            if (sps.profileIdc != H264Const.PROFILE_BASELINE && sps.profileIdc != H264Const.PROFILE_MAIN && sps.profileIdc != H264Const.PROFILE_HIGH) throw RuntimeException("Unsupported h264 feature: " + sps.profileIdc + " profile.")
        }

        fun performIDRMarking(refPicMarkingIDR: RefPicMarkingIDR?, picture: Frame) {
            clearAll()
            dec.pictureBuffer.clear()
            val saved = saveRef(picture)
            if (refPicMarkingIDR!!.isUseForlongTerm) {
                dec.lRefs!!.put(0, saved)
                saved.isShortTerm = false
            } else dec.sRefs!![firstSliceHeader!!.frameNum] = saved
        }

        private fun saveRef(decoded: Frame): Frame {
            val frame = if (dec.pictureBuffer.size > 0) dec.pictureBuffer.removeAt(0) else createFrame(decoded)
            frame.copyFromFrame(decoded)
            return frame
        }

        private fun releaseRef(picture: Frame?) {
            if (picture != null) {
                dec.pictureBuffer.add(picture)
            }
        }

        fun clearAll() {
            for (i in dec.sRefs!!.indices) {
                releaseRef(dec.sRefs!![i])
                dec.sRefs!![i] = null
            }
            val keys = dec.lRefs!!.keys()
            for (i in keys.indices) {
                releaseRef(dec.lRefs!![keys[i]])
            }
            dec.lRefs!!.clear()
        }

        fun performMarking(refPicMarking: RefPicMarking?, picture: Frame) {
            var saved: Frame? = saveRef(picture)
            if (refPicMarking != null) {
                val instructions = refPicMarking.instructions
                for (i in instructions.indices) {
                    val instr = instructions[i]
                    when (instr.type) {
                        InstrType.REMOVE_SHORT -> unrefShortTerm(instr.arg1)
                        InstrType.REMOVE_LONG -> unrefLongTerm(instr.arg1)
                        InstrType.CONVERT_INTO_LONG -> convert(instr.arg1, instr.arg2)
                        InstrType.TRUNK_LONG -> truncateLongTerm(instr.arg1 - 1)
                        InstrType.CLEAR -> clearAll()
                        InstrType.MARK_LONG -> {
                            saveLong(saved, instr.arg1)
                            saved = null
                        }
                    }
                }
            }
            saved?.let { saveShort(it) }
            val maxFrames = 1 shl activeSps!!.log2MaxFrameNumMinus4 + 4
            if (refPicMarking == null) {
                val maxShort = Math.max(1, activeSps!!.numRefFrames - dec.lRefs!!.size())
                var min = Int.MAX_VALUE
                var num = 0
                var minFn = 0
                for (i in dec.sRefs!!.indices) {
                    if (dec.sRefs!![i] != null) {
                        val fnWrap = unwrap(firstSliceHeader!!.frameNum, dec.sRefs!![i]!!.frameNo, maxFrames)
                        if (fnWrap < min) {
                            min = fnWrap
                            minFn = dec.sRefs!![i]!!.frameNo
                        }
                        num++
                    }
                }
                if (num > maxShort) {
                    releaseRef(dec.sRefs!![minFn])
                    dec.sRefs!![minFn] = null
                }
            }
        }

        private fun unwrap(thisFrameNo: Int, refFrameNo: Int, maxFrames: Int): Int {
            return if (refFrameNo > thisFrameNo) refFrameNo - maxFrames else refFrameNo
        }

        private fun saveShort(saved: Frame) {
            dec.sRefs!![firstSliceHeader!!.frameNum] = saved
        }

        private fun saveLong(saved: Frame?, longNo: Int) {
            val prev = dec.lRefs!![longNo]
            prev?.let { releaseRef(it) }
            saved!!.isShortTerm = false
            dec.lRefs!!.put(longNo, saved)
        }

        private fun truncateLongTerm(maxLongNo: Int) {
            val keys = dec.lRefs!!.keys()
            for (i in keys.indices) {
                if (keys[i] > maxLongNo) {
                    releaseRef(dec.lRefs!![keys[i]])
                    dec.lRefs!!.remove(keys[i])
                }
            }
        }

        private fun convert(shortNo: Int, longNo: Int) {
            val ind = MathUtil.wrap(firstSliceHeader!!.frameNum - shortNo,
                    1 shl firstSliceHeader!!.sps!!.log2MaxFrameNumMinus4 + 4)
            releaseRef(dec.lRefs!![longNo])
            dec.lRefs!!.put(longNo, dec.sRefs!![ind])
            dec.sRefs!![ind] = null
            dec.lRefs!![longNo]!!.isShortTerm = false
        }

        private fun unrefLongTerm(longNo: Int) {
            releaseRef(dec.lRefs!![longNo])
            dec.lRefs!!.remove(longNo)
        }

        private fun unrefShortTerm(shortNo: Int) {
            val ind = MathUtil.wrap(firstSliceHeader!!.frameNum - shortNo,
                    1 shl firstSliceHeader!!.sps!!.log2MaxFrameNumMinus4 + 4)
            releaseRef(dec.sRefs!![ind])
            dec.sRefs!![ind] = null
        }

    }

    fun addSps(spsList: List<ByteBuffer?>?) {
        reader.addSpsList(spsList!!)
    }

    fun addPps(ppsList: List<ByteBuffer?>?) {
        reader.addPpsList(ppsList!!)
    }

    override fun getCodecMeta(data: ByteBuffer): VideoCodecMeta? {
        val rawSPS = H264Utils.getRawSPS(data.duplicate())
        val rawPPS = H264Utils.getRawPPS(data.duplicate())
        if (rawSPS.size == 0) {
            Logger.warn("Can not extract metadata from the packet not containing an SPS.")
            return null
        }
        val sps = SeqParameterSet.read(rawSPS[0])
        val size = H264Utils.getPicSize(sps)
        //, H264Utils.saveCodecPrivate(rawSPS, rawPPS)
        return VideoCodecMeta.createSimpleVideoCodecMeta(size, ColorSpace.YUV420)
    }

    companion object {
        /**
         * Constructs this decoder from a portion of a stream that contains AnnexB
         * delimited (00 00 00 01) SPS/PPS NAL units. SPS/PPS NAL units are 0x67 and
         * 0x68 respectfully.
         *
         * @param codecPrivate
         */
        @JvmStatic
        fun createH264DecoderFromCodecPrivate(codecPrivate: ByteBuffer): H264Decoder {
            val d = H264Decoder()
            for (bb in H264Utils.splitFrame(codecPrivate.duplicate())) {
                val nu = NALUnit.read(bb!!)
                if (nu.type == NALUnitType.SPS) {
                    d.reader.addSps(bb)
                } else if (nu.type == NALUnitType.PPS) {
                    d.reader.addPps(bb)
                }
            }
            return d
        }

        fun createFrame(sps: SeqParameterSet?, buffer: Array<ByteArray?>?, frameNum: Int, frameType: SliceType?,
                        mvs: MvList2D?, refsUsed: Array<Array<Array<Frame?>?>?>?, POC: Int): Frame {
            val width = sps!!.picWidthInMbsMinus1 + 1 shl 4
            val height = getPicHeightInMbs(sps) shl 4
            var crop: Rect? = null
            if (sps.isFrameCroppingFlag) {
                val sX = sps.frameCropLeftOffset shl 1
                val sY = sps.frameCropTopOffset shl 1
                val w = width - (sps.frameCropRightOffset shl 1) - sX
                val h = height - (sps.frameCropBottomOffset shl 1) - sY
                crop = Rect(sX, sY, w, h)
            }
            return Frame(width, height, buffer, ColorSpace.YUV420, crop, frameNum, frameType, mvs!!, refsUsed!!, POC)
        }

        @UsedViaReflection
        fun probe(data: ByteBuffer): Int {
            var validSps = false
            var validPps = false
            var validSh = false
            for (nalUnit in H264Utils.splitFrame(data.duplicate())) {
                val marker = NALUnit.read(nalUnit!!)
                if (marker.type == NALUnitType.IDR_SLICE || marker.type == NALUnitType.NON_IDR_SLICE) {
                    val reader = BitReader.createBitReader(nalUnit)
                    validSh = validSh(readPart1(reader))
                    break
                } else if (marker.type == NALUnitType.SPS) {
                    validSps = validSps(SeqParameterSet.read(nalUnit))
                } else if (marker.type == NALUnitType.PPS) {
                    validPps = validPps(PictureParameterSet.read(nalUnit))
                }
            }
            return (if (validSh) 60 else 0) + (if (validSps) 20 else 0) + if (validPps) 20 else 0
        }

        private fun validSh(sh: SliceHeader): Boolean {
            return sh.firstMbInSlice == 0 && sh.sliceType != null && sh.picParameterSetId < 2
        }

        private fun validSps(sps: SeqParameterSet): Boolean {
            return sps.bitDepthChromaMinus8 < 4 && sps.bitDepthLumaMinus8 < 4 && sps.chromaFormatIdc != null && sps.seqParameterSetId < 2 && sps.picOrderCntType <= 2
        }

        private fun validPps(pps: PictureParameterSet): Boolean {
            return pps.picInitQpMinus26 <= 26 && pps.seqParameterSetId <= 2 && pps.picParameterSetId <= 2
        }
    }

    init {
        pictureBuffer = ArrayList()
        poc = POCManager()
        threaded = Runtime.getRuntime().availableProcessors() > 1
        if (threaded) {
            tp = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()) { r ->
                val t = Executors.defaultThreadFactory().newThread(r)
                t.isDaemon = true
                t
            }
        }
        reader = FrameReader()
    }
}