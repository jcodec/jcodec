package org.jcodec.codecs.h264

import org.jcodec.codecs.h264.decode.SliceHeaderReader.readPart1
import org.jcodec.codecs.h264.decode.SliceHeaderReader.readPart2
import org.jcodec.codecs.h264.io.model.*
import org.jcodec.codecs.h264.io.model.NALUnit.Companion.read
import org.jcodec.codecs.h264.io.model.RefPicMarking.InstrType
import org.jcodec.common.Demuxer
import org.jcodec.common.DemuxerTrack
import org.jcodec.common.DemuxerTrackMeta
import org.jcodec.common.IntObjectMap
import org.jcodec.common.io.BitReader
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Extracts H.264 frames out H.264 Elementary stream ( according to Annex B )
 *
 * @author The JCodec project
 */
class BufferH264ES(bb: ByteBuffer) : DemuxerTrack, Demuxer {
    private val bb: ByteBuffer
    private val pps: IntObjectMap<PictureParameterSet?>
    private val sps: IntObjectMap<SeqParameterSet?>

    // POC and framenum detection
    private var prevFrameNumOffset = 0
    private var prevFrameNum = 0
    private var prevPicOrderCntMsb = 0
    private var prevPicOrderCntLsb = 0
    private var frameNo: Int
    override fun nextFrame(): Packet {
        val result = bb.duplicate()
        var prevNu: NALUnit? = null
        var prevSh: SliceHeader? = null
        while (true) {
            bb.mark()
            val buf = H264Utils.nextNALUnit(bb) ?: break
            // NIOUtils.skip(buf, 4);
            val nu = read(buf)
            if (nu.type == NALUnitType.IDR_SLICE || nu.type == NALUnitType.NON_IDR_SLICE) {
                val sh = readSliceHeader(buf, nu)
                if (prevNu != null && prevSh != null && !sameFrame(prevNu, nu, prevSh, sh)) {
                    bb.reset()
                    break
                }
                prevSh = sh
                prevNu = nu
            } else if (nu.type == NALUnitType.PPS) {
                val read = PictureParameterSet.read(buf)
                pps.put(read.picParameterSetId, read)
            } else if (nu.type == NALUnitType.SPS) {
                val read = SeqParameterSet.read(buf)
                sps.put(read.seqParameterSetId, read)
            }
        }
        result.limit(bb.position())
        return prevSh?.let { detectPoc(result, prevNu, it) }!!
    }

    private fun readSliceHeader(buf: ByteBuffer, nu: NALUnit): SliceHeader {
        val br = BitReader.createBitReader(buf)
        val sh = readPart1(br)
        val pp = pps[sh.picParameterSetId]
        readPart2(sh, nu, sps[pp!!.seqParameterSetId]!!, pp, br)
        return sh
    }

    private fun sameFrame(nu1: NALUnit, nu2: NALUnit, sh1: SliceHeader, sh2: SliceHeader): Boolean {
        if (sh1.picParameterSetId != sh2.picParameterSetId) return false
        if (sh1.frameNum != sh2.frameNum) return false
        val sps = sh1.sps
        if (sps!!.picOrderCntType == 0 && sh1.picOrderCntLsb != sh2.picOrderCntLsb) return false
        if (sps.picOrderCntType == 1 && (sh1.deltaPicOrderCnt!![0] != sh2.deltaPicOrderCnt!![0] || sh1.deltaPicOrderCnt!![1] != sh2.deltaPicOrderCnt!![1])) return false
        if ((nu1.nal_ref_idc == 0 || nu2.nal_ref_idc == 0) && nu1.nal_ref_idc != nu2.nal_ref_idc) return false
        if (nu1.type == NALUnitType.IDR_SLICE != (nu2.type == NALUnitType.IDR_SLICE)) return false
        return if (sh1.idrPicId != sh2.idrPicId) false else true
    }

    private fun detectPoc(result: ByteBuffer, nu: NALUnit?, sh: SliceHeader): Packet {
        val maxFrameNum = 1 shl sh.sps!!.log2MaxFrameNumMinus4 + 4
        if (detectGap(sh, maxFrameNum)) {
            issueNonExistingPic(sh, maxFrameNum)
        }
        val absFrameNum = updateFrameNumber(sh.frameNum, maxFrameNum, detectMMCO5(sh.refPicMarkingNonIDR))
        var poc = 0
        if (nu!!.type == NALUnitType.NON_IDR_SLICE) {
            poc = calcPoc(absFrameNum, nu, sh)
        }
        return Packet(result, absFrameNum.toLong(), 1, 1, frameNo++.toLong(), if (nu.type == NALUnitType.IDR_SLICE) FrameType.KEY else FrameType.INTER, null, poc)
    }

    private fun updateFrameNumber(frameNo: Int, maxFrameNum: Int, mmco5: Boolean): Int {
        val frameNumOffset: Int
        frameNumOffset = if (prevFrameNum > frameNo) prevFrameNumOffset + maxFrameNum else prevFrameNumOffset
        val absFrameNum = frameNumOffset + frameNo
        prevFrameNum = if (mmco5) 0 else frameNo
        prevFrameNumOffset = frameNumOffset
        return absFrameNum
    }

    private fun issueNonExistingPic(sh: SliceHeader, maxFrameNum: Int) {
        val nextFrameNum = (prevFrameNum + 1) % maxFrameNum
        // refPictureManager.addNonExisting(nextFrameNum);
        prevFrameNum = nextFrameNum
    }

    private fun detectGap(sh: SliceHeader, maxFrameNum: Int): Boolean {
        return sh.frameNum != prevFrameNum && sh.frameNum != (prevFrameNum + 1) % maxFrameNum
    }

    private fun calcPoc(absFrameNum: Int, nu: NALUnit?, sh: SliceHeader): Int {
        return if (sh.sps!!.picOrderCntType == 0) {
            calcPOC0(nu, sh)
        } else if (sh.sps!!.picOrderCntType == 1) {
            calcPOC1(absFrameNum, nu, sh)
        } else {
            calcPOC2(absFrameNum, nu, sh)
        }
    }

    private fun calcPOC2(absFrameNum: Int, nu: NALUnit?, sh: SliceHeader): Int {
        return if (nu!!.nal_ref_idc == 0) 2 * absFrameNum - 1 else 2 * absFrameNum
    }

    private fun calcPOC1(absFrameNum: Int, nu: NALUnit?, sh: SliceHeader): Int {
        var absFrameNum = absFrameNum
        if (sh.sps!!.numRefFramesInPicOrderCntCycle == 0) absFrameNum = 0
        if (nu!!.nal_ref_idc == 0 && absFrameNum > 0) absFrameNum = absFrameNum - 1
        var expectedDeltaPerPicOrderCntCycle = 0
        for (i in 0 until sh.sps!!.numRefFramesInPicOrderCntCycle) expectedDeltaPerPicOrderCntCycle += sh.sps!!.offsetForRefFrame!![i]
        var expectedPicOrderCnt: Int
        if (absFrameNum > 0) {
            val picOrderCntCycleCnt = (absFrameNum - 1) / sh.sps!!.numRefFramesInPicOrderCntCycle
            val frameNumInPicOrderCntCycle = (absFrameNum - 1) % sh.sps!!.numRefFramesInPicOrderCntCycle
            expectedPicOrderCnt = picOrderCntCycleCnt * expectedDeltaPerPicOrderCntCycle
            for (i in 0..frameNumInPicOrderCntCycle) expectedPicOrderCnt = expectedPicOrderCnt + sh.sps!!.offsetForRefFrame!![i]
        } else {
            expectedPicOrderCnt = 0
        }
        if (nu.nal_ref_idc == 0) expectedPicOrderCnt = expectedPicOrderCnt + sh.sps!!.offsetForNonRefPic
        return expectedPicOrderCnt + sh.deltaPicOrderCnt!![0]
    }

    private fun calcPOC0(nu: NALUnit?, sh: SliceHeader): Int {
        val pocCntLsb = sh.picOrderCntLsb
        val maxPicOrderCntLsb = 1 shl sh.sps!!.log2MaxPicOrderCntLsbMinus4 + 4

        // TODO prevPicOrderCntMsb should be wrapped!!
        val picOrderCntMsb: Int
        picOrderCntMsb = if (pocCntLsb < prevPicOrderCntLsb && prevPicOrderCntLsb - pocCntLsb >= maxPicOrderCntLsb / 2) prevPicOrderCntMsb + maxPicOrderCntLsb else if (pocCntLsb > prevPicOrderCntLsb && pocCntLsb - prevPicOrderCntLsb > maxPicOrderCntLsb / 2) prevPicOrderCntMsb - maxPicOrderCntLsb else prevPicOrderCntMsb
        if (nu!!.nal_ref_idc != 0) {
            prevPicOrderCntMsb = picOrderCntMsb
            prevPicOrderCntLsb = pocCntLsb
        }
        return picOrderCntMsb + pocCntLsb
    }

    private fun detectMMCO5(refPicMarkingNonIDR: RefPicMarking?): Boolean {
        if (refPicMarkingNonIDR == null) return false
        val instructions = refPicMarkingNonIDR.instructions
        for (i in instructions.indices) {
            val instr = instructions[i]
            if (instr.type === InstrType.CLEAR) {
                return true
            }
        }
        return false
    }

    fun getSps(): Array<SeqParameterSet?> {
        return sps.values(arrayOfNulls<SeqParameterSet>(0))
    }

    fun getPps(): Array<PictureParameterSet?> {
        return pps.values(arrayOfNulls<PictureParameterSet>(0))
    }

    override fun getMeta(): DemuxerTrackMeta? {
        return null
    }

    @Throws(IOException::class)
    override fun close() {
        // TODO Auto-generated method stub
    }

    override fun getTracks(): List<DemuxerTrack> {
        return videoTracks
    }

    override fun getVideoTracks(): List<DemuxerTrack> {
        val tracks: MutableList<DemuxerTrack> = ArrayList()
        tracks.add(this)
        return tracks
    }

    override fun getAudioTracks(): List<DemuxerTrack> {
        return ArrayList()
    }

    init {
        pps = IntObjectMap()
        sps = IntObjectMap()
        this.bb = bb
        frameNo = 0
    }
}