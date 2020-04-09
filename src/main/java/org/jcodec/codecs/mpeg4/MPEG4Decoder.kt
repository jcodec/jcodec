package org.jcodec.codecs.mpeg4

import org.jcodec.codecs.mpeg4.MPEG4BiRenderer.renderBi
import org.jcodec.codecs.mpeg4.Macroblock.Companion.vec
import org.jcodec.common.UsedViaReflection
import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.VideoDecoder
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.BitReader.Companion.createBitReader
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Rect
import org.jcodec.common.model.Size
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MPEG4Decoder : VideoDecoder() {
    private val refs: Array<Picture?> = arrayOfNulls(2)
    private var prevMBs: Array<Macroblock?> = emptyArray()
    private var mbs: Array<Macroblock?> = emptyArray()
    private var ctx: MPEG4DecodingContext? = null
    override fun decodeFrame(data: ByteBuffer, buffer: Array<ByteArray>): Picture? {
        if (ctx == null) ctx = MPEG4DecodingContext()
        val _ctx = ctx!!
        if (!_ctx.readHeaders(data)) return null
        _ctx.intraDCThreshold = 0
        _ctx.fcodeBackward = _ctx.intraDCThreshold
        _ctx.fcodeForward = _ctx.fcodeBackward
        val br = createBitReader(data)
        if (!_ctx.readVOPHeader(br)) return null
        mbs = arrayOfNulls(_ctx.mbWidth * _ctx.mbHeight)
        for (i in mbs.indices) {
            mbs[i] = Macroblock()
        }
        var decoded: Picture? = null
        if (_ctx.codingType != MPEG4Bitstream.B_VOP) {
            when (_ctx.codingType) {
                MPEG4Bitstream.I_VOP -> decoded = decodeIFrame(br, _ctx, buffer)
                MPEG4Bitstream.P_VOP -> decoded = decodePFrame(br, _ctx, buffer, _ctx.fcodeForward)
                MPEG4Bitstream.S_VOP -> throw RuntimeException("GMC not supported.")
                MPEG4Bitstream.N_VOP -> return null
            }
            refs[1] = refs[0]
            refs[0] = decoded!!
            prevMBs = mbs
        } else {
            decoded = decodeBFrame(br, _ctx, buffer, _ctx.quant, _ctx.fcodeForward, _ctx.fcodeBackward)
        }
        // We don't want to overread
        br.terminate()
        return decoded
    }

    private fun decodeIFrame(br: BitReader, ctx: MPEG4DecodingContext, buffer: Array<ByteArray>): Picture {
        val p = Picture(ctx.mbWidth shl 4, ctx.mbHeight shl 4, buffer, null, ColorSpace.YUV420, 0,
                Rect(0, 0, ctx.width, ctx.height))
        var bound = 0
        var y = 0
        while (y < ctx.mbHeight) {
            var x = 0
            while (x < ctx.mbWidth) {
                val mb = mbs[y * ctx.mbWidth + x]
                mb!!.reset(x, y, bound)
                MPEG4Bitstream.readIntraMode(br, ctx, mb)
                val index = x + y * ctx.mbWidth
                var aboveMb: Macroblock? = null
                var aboveLeftMb: Macroblock? = null
                var leftMb: Macroblock? = null
                val apos = index - ctx.mbWidth
                val lpos = index - 1
                val alpos = index - 1 - ctx.mbWidth
                if (apos >= bound) aboveMb = mbs[apos]
                if (alpos >= bound) aboveLeftMb = mbs[alpos]
                if (x > 0 && lpos >= bound) leftMb = mbs[lpos]
                MPEG4Bitstream.readCoeffIntra(br, ctx, mb, aboveMb, leftMb, aboveLeftMb)
                x = mb.x
                y = mb.y
                bound = mb.bound
                MPEG4Renderer.renderIntra(mb, ctx)
                putPix(p, mb, x, y)
                x++
            }
            y++
        }
        return p
    }

    fun decodePFrame(br: BitReader, ctx: MPEG4DecodingContext, buffers: Array<ByteArray>?, fcode: Int): Picture {
        var bound = 0
        val mbWidth = ctx.mbWidth
        val mbHeight = ctx.mbHeight
        val p = Picture(ctx.mbWidth shl 4, ctx.mbHeight shl 4, buffers, null, ColorSpace.YUV420, 0,
                Rect(0, 0, ctx.width, ctx.height))
        var y = 0
        while (y < mbHeight) {
            var x = 0
            while (x < mbWidth) {

                // skip stuffing
                while (br.checkNBit(10) == 1) br.skip(10)
                if (MPEG4Bitstream.checkResyncMarker(br, fcode - 1)) {
                    bound = MPEG4Bitstream.readVideoPacketHeader(br, ctx, fcode - 1, true, false, true)
                    x = bound % mbWidth
                    y = bound / mbWidth
                }
                val index = x + y * ctx.mbWidth
                var aboveMb: Macroblock? = null
                var aboveLeftMb: Macroblock? = null
                var leftMb: Macroblock? = null
                var aboveRightMb: Macroblock? = null
                val apos = index - ctx.mbWidth
                val lpos = index - 1
                val alpos = index - 1 - ctx.mbWidth
                val arpos = index + 1 - ctx.mbWidth
                if (apos >= bound) aboveMb = mbs[apos]
                if (alpos >= bound) aboveLeftMb = mbs[alpos]
                if (x > 0 && lpos >= bound) leftMb = mbs[lpos]
                if (arpos >= bound && x < ctx.mbWidth - 1) aboveRightMb = mbs[arpos]
                val mb = mbs[y * ctx.mbWidth + x]
                mb!!.reset(x, y, bound)
                MPEG4Bitstream.readInterModeCoeffs(br, ctx, fcode, mb, aboveMb, leftMb, aboveLeftMb, aboveRightMb)
                MPEG4Renderer.renderInter(ctx, refs, mb, fcode, 0, false)
                putPix(p, mb, x, y)
                x++
            }
            y++
        }
        return p
    }

    private fun decodeBFrame(br: BitReader, ctx: MPEG4DecodingContext, buffers: Array<ByteArray>, quant: Int, fcodeForward: Int,
                             fcodeBackward: Int): Picture {
        val p = Picture(ctx.mbWidth shl 4, ctx.mbHeight shl 4, buffers, null, ColorSpace.YUV420, 0,
                Rect(0, 0, ctx.width, ctx.height))
        val pFMV = vec()
        val pBMV = vec()
        //To prevent unexpected behaviour in Javascript, final variables must be declared at method level and not inside loops
        val fcodeMax = if (fcodeForward > fcodeBackward) fcodeForward else fcodeBackward
        var y = 0
        while (y < ctx.mbHeight) {
            pFMV.y = 0
            pFMV.x = pFMV.y
            pBMV.y = pFMV.x
            pBMV.x = pBMV.y
            var x = 0
            while (x < ctx.mbWidth) {
                val mb = mbs[y * ctx.mbWidth + x]
                val lastMB = prevMBs[y * ctx.mbWidth + x]
                if (MPEG4Bitstream.checkResyncMarker(br, fcodeMax - 1)) {
                    val bound = MPEG4Bitstream.readVideoPacketHeader(br, ctx, fcodeMax - 1, fcodeForward != 0, fcodeBackward != 0,
                            ctx.intraDCThreshold != 0)
                    x = bound % ctx.mbWidth
                    y = bound / ctx.mbWidth
                    pFMV.y = 0
                    pFMV.x = pFMV.y
                    pBMV.y = pFMV.x
                    pBMV.x = pBMV.y
                }
                mb!!.x = x
                mb.y = y
                mb.quant = quant
                if (lastMB!!.mode == MPEG4Consts.MODE_NOT_CODED) {
                    mb.cbp = 0
                    mb.mode = MPEG4Consts.MODE_FORWARD
                    MPEG4Bitstream.readInterCoeffs(br, ctx, mb)
                    MPEG4Renderer.renderInter(ctx, refs, lastMB, fcodeForward, 1, true)
                    putPix(p, lastMB, x, y)
                    x++
                    continue
                }
                MPEG4Bitstream.readBi(br, ctx, fcodeForward, fcodeBackward, mb, lastMB, pFMV, pBMV)
                renderBi(ctx, refs as Array<Picture>, fcodeForward, fcodeBackward, mb)
                putPix(p, mb, x, y)
                x++
            }
            y++
        }
        return p
    }

    override fun getCodecMeta(data: ByteBuffer): VideoCodecMeta? {
        val ctx = MPEG4DecodingContext.readFromHeaders(data.duplicate()) ?: return null
        return VideoCodecMeta.createSimpleVideoCodecMeta(Size(ctx.width, ctx.height), ColorSpace.YUV420J)
    }

    companion object {
        fun putPix(p: Picture, mb: Macroblock?, x: Int, y: Int) {
            val plane0 = p.getPlaneData(0)
            var dsto0 = (y shl 4) * p.width + (x shl 4)
            var row = 0
            var srco = 0
            while (row < 16) {
                var col = 0
                while (col < 16) {
                    plane0[dsto0 + col] = mb!!.pred[0][srco]
                    col++
                    srco++
                }
                row++
                dsto0 += p.width
            }
            for (pl in 1..2) {
                val plane = p.getPlaneData(pl)
                var dsto = (y shl 3) * p.getPlaneWidth(pl) + (x shl 3)
                var row = 0
                var srco = 0
                while (row < 8) {
                    var col = 0
                    while (col < 8) {
                        plane[dsto + col] = mb!!.pred[pl][srco]
                        col++
                        srco++
                    }
                    row++
                    dsto += p.getPlaneWidth(pl)
                }
            }
        }

        @UsedViaReflection
        fun probe(data: ByteBuffer): Int {
            val ctx = MPEG4DecodingContext.readFromHeaders(data.duplicate()) ?: return 0
            return Math.min(if (ctx.width > 320) if (ctx.width < 1280) 100 else 50 else 50,
                    if (ctx.height > 240) if (ctx.height < 720) 100 else 50 else 50)
        }
    }

}