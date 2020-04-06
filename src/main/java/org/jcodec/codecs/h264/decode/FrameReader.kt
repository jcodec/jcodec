package org.jcodec.codecs.h264.decode

import org.jcodec.codecs.common.biari.MDecoder
import org.jcodec.codecs.h264.H264Utils
import org.jcodec.codecs.h264.decode.SliceHeaderReader.readPart1
import org.jcodec.codecs.h264.decode.SliceHeaderReader.readPart2
import org.jcodec.codecs.h264.decode.aso.MapManager
import org.jcodec.codecs.h264.io.CABAC
import org.jcodec.codecs.h264.io.CAVLC
import org.jcodec.codecs.h264.io.model.NALUnit
import org.jcodec.codecs.h264.io.model.NALUnitType
import org.jcodec.codecs.h264.io.model.PictureParameterSet
import org.jcodec.codecs.h264.io.model.SeqParameterSet
import org.jcodec.common.IntObjectMap
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.logging.Logger
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * MPEG 4 AVC ( H.264 ) Frame reader
 *
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 *
 * @author The JCodec project
 */
class FrameReader {
    private val sps: IntObjectMap<SeqParameterSet>
    private val pps: IntObjectMap<PictureParameterSet>
    fun readFrame(nalUnits: List<ByteBuffer?>): List<SliceReader>? {
        val result: MutableList<SliceReader> = ArrayList()
        for (nalData in nalUnits) {
            val nalUnit = NALUnit.read(nalData!!)
            H264Utils.unescapeNAL(nalData)
            if (NALUnitType.SPS == nalUnit.type) {
                val _sps = SeqParameterSet.read(nalData)
                sps.put(_sps.seqParameterSetId, _sps)
            } else if (NALUnitType.PPS == nalUnit.type) {
                val _pps = PictureParameterSet.read(nalData)
                pps.put(_pps.picParameterSetId, _pps)
            } else if (NALUnitType.IDR_SLICE == nalUnit.type || NALUnitType.NON_IDR_SLICE == nalUnit.type) {
                if (sps.size() == 0 || pps.size() == 0) {
                    Logger.warn("Skipping frame as no SPS/PPS have been seen so far...")
                    return null
                }
                result.add(createSliceReader(nalData, nalUnit))
            }
        }
        return result
    }

    private fun createSliceReader(segment: ByteBuffer?, nalUnit: NALUnit): SliceReader {
        val _in = BitReader.createBitReader(segment)
        val sh = readPart1(_in)
        sh.pps = pps[sh.picParameterSetId]
        sh.sps = sps[sh.pps!!.seqParameterSetId]
        readPart2(sh, nalUnit, sh.sps!!, sh.pps!!, _in)
        val mapper = MapManager(sh.sps!!, sh.pps!!).getMapper(sh)
        val cavlc = arrayOf(CAVLC(sh.sps, sh.pps, 2, 2), CAVLC(sh.sps, sh.pps, 1, 1),
                CAVLC(sh.sps, sh.pps, 1, 1))
        val mbWidth = sh.sps!!.picWidthInMbsMinus1 + 1
        val cabac = CABAC(mbWidth)
        var mDecoder: MDecoder? = null
        if (sh.pps!!.isEntropyCodingModeFlag) {
            _in.terminate()
            val cm = Array(2) { IntArray(1024) }
            val qp = sh.pps!!.picInitQpMinus26 + 26 + sh.sliceQpDelta
            cabac.initModels(cm, sh.sliceType!!, sh.cabacInitIdc, qp)
            mDecoder = MDecoder(segment, cm)
        }
        return SliceReader(sh.pps, cabac, cavlc, mDecoder, _in, mapper, sh, nalUnit)
    }

    fun addSpsList(spsList: List<ByteBuffer?>) {
        for (byteBuffer in spsList) {
            addSps(byteBuffer)
        }
    }

    fun addSps(byteBuffer: ByteBuffer?) {
        val clone = NIOUtils.clone(byteBuffer)
        H264Utils.unescapeNAL(clone)
        val s = SeqParameterSet.read(clone)
        sps.put(s.seqParameterSetId, s)
    }

    fun addPpsList(ppsList: List<ByteBuffer?>) {
        for (byteBuffer in ppsList) {
            addPps(byteBuffer)
        }
    }

    fun addPps(byteBuffer: ByteBuffer?) {
        val clone = NIOUtils.clone(byteBuffer)
        H264Utils.unescapeNAL(clone)
        val p = PictureParameterSet.read(clone)
        pps.put(p.picParameterSetId, p)
    }

    init {
        sps = IntObjectMap()
        pps = IntObjectMap()
    }
}