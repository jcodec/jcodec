package org.jcodec.codecs.mpa

import org.jcodec.codecs.mpa.Mp3Bitstream.*
import org.jcodec.common.AudioCodecMeta
import org.jcodec.common.AudioDecoder
import org.jcodec.common.AudioFormat
import org.jcodec.common.io.BitReader
import org.jcodec.common.io.BitReader.Companion.createBitReader
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.AudioBuffer
import org.jcodec.common.tools.MathUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Mp3Decoder : AudioDecoder {
    private val filter: Array<ChannelSynthesizer?>
    private var initialized = false
    private var prevBlk: Array<FloatArray> = emptyArray()
    private val frameData: ByteBuffer
    private var channels = 0
    private var sfreq = 0
    private val samples: FloatArray
    private val mdctIn: FloatArray
    private val mdctOut: FloatArray
    private val dequant: Array<FloatArray>
    private val tmpOut: Array<ShortArray>
    private fun init(header: MpaHeader) {
        val scalefactor = 32700.0f
        channels = if (header.mode == MpaConst.SINGLE_CHANNEL) 1 else 2
        filter[0] = ChannelSynthesizer(0, scalefactor)
        if (channels == 2) filter[1] = ChannelSynthesizer(1, scalefactor)
        prevBlk = Array(2) { FloatArray(NUM_BANDS * SAMPLES_PER_BAND) }
        sfreq = header.sampleFreq + if (header.version == MpaConst.MPEG1) 3 else if (header.version == MpaConst.MPEG25_LSF) 6 else 0
        for (ch in 0..1) Arrays.fill(prevBlk[ch], 0.0f)
        initialized = true
    }

    private fun decodeGranule(header: MpaHeader, output: ByteBuffer, si: MP3SideInfo, br: BitReader,
                              scalefac: Array<ScaleFactors?>, grInd: Int) {
        Arrays.fill(dequant[0], 0f)
        Arrays.fill(dequant[1], 0f)
        for (ch in 0 until channels) {
            val part2Start = br.position()
            val granule = si.granule[ch][grInd]
            if (header.version == MpaConst.MPEG1) {
                val old = scalefac[ch]
                val scfi = if (grInd == 0) ALL_TRUE else si.scfsi[ch]
                scalefac[ch] = Mp3Bitstream.readScaleFactors(br, si.granule[ch][grInd], scfi)
                mergeScaleFac(scalefac[ch], old, scfi)
            } else {
                scalefac[ch] = Mp3Bitstream.readLSFScaleFactors(br, header, granule, ch)
            }
            val coeffs = IntArray(NUM_BANDS * SAMPLES_PER_BAND + 4)
            val nonzero = Mp3Bitstream.readCoeffs(br, granule, ch, part2Start, sfreq, coeffs)
            dequantizeCoeffs(coeffs, nonzero, granule, scalefac[ch], dequant[ch])
        }
        val msStereo = header.mode == MpaConst.JOINT_STEREO && header.modeExtension and 0x2 != 0
        if (msStereo && channels == 2) decodeMsStereo(header, si.granule[0][grInd], scalefac, dequant)
        for (ch in 0 until channels) {
            val out = dequant[ch]
            val granule = si.granule[ch][grInd]
            antialias(granule, out)
            mdctDecode(ch, granule, out)
            var sb18 = 18
            while (sb18 < 576) {
                var ss = 1
                while (ss < SAMPLES_PER_BAND) {
                    out[sb18 + ss] = -out[sb18 + ss]
                    ss += 2
                }
                sb18 += 36
            }
            var ss = 0
            var off = 0
            while (ss < SAMPLES_PER_BAND) {
                var sb18 = 0
                var sb = 0
                while (sb18 < 576) {
                    samples[sb] = out[sb18 + ss]
                    sb18 += 18
                    sb++
                }
                filter[ch]!!.synthesize(samples, tmpOut[ch], off)
                ss++
                off += 32
            }
        }
        if (channels == 2) {
            appendSamplesInterleave(output, tmpOut[0], tmpOut[1], 576)
        } else {
            appendSamples(output, tmpOut[0], 576)
        }
    }

    private fun mergeScaleFac(sf: ScaleFactors?, old: ScaleFactors?, scfsi: BooleanArray) {
        if (!scfsi[0]) {
            for (i in 0..5) sf!!.large[i] = old!!.large[i]
        }
        if (!scfsi[1]) {
            for (i in 6..10) sf!!.large[i] = old!!.large[i]
        }
        if (!scfsi[2]) {
            for (i in 11..15) sf!!.large[i] = old!!.large[i]
        }
        if (!scfsi[3]) {
            for (i in 16..20) sf!!.large[i] = old!!.large[i]
        }
    }

    private fun dequantizeCoeffs(input: IntArray, nonzero: Int, granule: Granule, scalefac: ScaleFactors?, out: FloatArray) {
        val globalGain = Math.pow(2.0, 0.25 * (granule.globalGain - 210.0)).toFloat()
        if (granule.windowSwitchingFlag && granule.blockType == 2) {
            if (granule.mixedBlockFlag) {
                dequantMixed(input, nonzero, granule, scalefac, globalGain, out)
            } else {
                dequantShort(input, nonzero, granule, scalefac, globalGain, out)
            }
        } else {
            dequantLong(input, nonzero, granule, scalefac, globalGain, out)
        }
    }

    private fun dequantMixed(input: IntArray, nonzero: Int, granule: Granule, scalefac: ScaleFactors?, globalGain: Float,
                             out: FloatArray) {
        var i = 0
        run {
            var sfb = 0
            while (sfb < 8 && i < nonzero) {
                while (i < MpaConst.sfbLong[sfreq][sfb + 1] && i < nonzero) {
                    val idx = scalefac!!.large[sfb] + if (granule.preflag) MpaConst.pretab[sfb] else 0 shl granule.scalefacScale
                    out[i] = globalGain * pow43(input[i]) * MpaConst.quantizerTab[idx]
                    i++
                }
                sfb++
            }
        }
        var sfb = 3
        while (sfb < 12 && i < nonzero) {
            val sfbSz = MpaConst.sfbShort[sfreq][sfb + 1] - MpaConst.sfbShort[sfreq][sfb]
            val sfbStart = i
            for (wnd in 0..2) {
                var j = 0
                while (j < sfbSz && i < nonzero) {
                    val idx = (scalefac!!.small[wnd][sfb] shl granule.scalefacScale) + (granule.subblockGain[wnd] shl 2)
                    // interleaving samples of the short windows
                    out[sfbStart + j * 3 + wnd] = globalGain * pow43(input[i]) * MpaConst.quantizerTab[idx]
                    j++
                    i++
                }
            }
            sfb++
        }
    }

    private fun dequantShort(input: IntArray, nonzero: Int, granule: Granule, scalefac: ScaleFactors?, globalGain: Float,
                             out: FloatArray) {
        var sfb = 0
        var i = 0
        while (i < nonzero) {
            val sfbSz = MpaConst.sfbShort[sfreq][sfb + 1] - MpaConst.sfbShort[sfreq][sfb]
            val sfbStart = i
            for (wnd in 0..2) {
                var j = 0
                while (j < sfbSz && i < nonzero) {
                    val idx = (scalefac!!.small[wnd][sfb] shl granule.scalefacScale) + (granule.subblockGain[wnd] shl 2)
                    // interleaving samples of the short windows
                    out[sfbStart + j * 3 + wnd] = globalGain * pow43(input[i]) * MpaConst.quantizerTab[idx]
                    j++
                    i++
                }
            }
            sfb++
        }
    }

    private fun dequantLong(input: IntArray, nonzero: Int, granule: Granule, scalefac: ScaleFactors?,
                            globalGain: Float, out: FloatArray) {
        var i = 0
        var sfb = 0
        while (i < nonzero) {
            if (i == MpaConst.sfbLong[sfreq][sfb + 1]) ++sfb
            val idx = scalefac!!.large[sfb] + if (granule.preflag) MpaConst.pretab[sfb] else 0 shl granule.scalefacScale
            out[i] = globalGain * pow43(input[i]) * MpaConst.quantizerTab[idx]
            i++
        }
    }

    private fun pow43(`val`: Int): Float {
        return if (`val` == 0) {
            0.0f
        } else {
            val sign = 1 - (`val` ushr 31 shl 1)
            val abs = MathUtil.abs(`val`)
            if (abs < MpaConst.power43Tab.size) sign * MpaConst.power43Tab[abs] else sign * Math.pow(abs.toDouble(), fourByThree).toFloat()
        }
    }

    private fun decodeMsStereo(header: MpaHeader, granule: Granule, scalefac: Array<ScaleFactors?>, ro: Array<FloatArray>) {
        for (i in 0..575) {
            val a = ro[0][i]
            val b = ro[1][i]
            ro[0][i] = (a + b) * 0.707106781f
            ro[1][i] = (a - b) * 0.707106781f
        }
    }

    private fun antialias(granule: Granule, out: FloatArray) {
        if (granule.windowSwitchingFlag && granule.blockType == 2 && !granule.mixedBlockFlag) return
        val bands = if (granule.windowSwitchingFlag && granule.mixedBlockFlag && granule.blockType == 2) 1 else 31
        var band = 0
        var bandStart = 0
        while (band < bands) {
            for (sample in 0..7) {
                val src_idx1 = bandStart + 17 - sample
                val src_idx2 = bandStart + 18 + sample
                val bu = out[src_idx1]
                val bd = out[src_idx2]
                out[src_idx1] = bu * MpaConst.cs[sample] - bd * MpaConst.ca[sample]
                out[src_idx2] = bd * MpaConst.cs[sample] + bu * MpaConst.ca[sample]
            }
            band++
            bandStart += 18
        }
    }

    private fun mdctDecode(ch: Int, granule: Granule, out: FloatArray) {
        var sb18 = 0
        while (sb18 < 576) {
            val blockType = if (granule.windowSwitchingFlag && granule.mixedBlockFlag && sb18 < 36) 0 else granule.blockType
            for (cc in 0..17) mdctIn[cc] = out[cc + sb18]
            if (blockType == 2) {
                Mp3Mdct.threeShort(mdctIn, mdctOut)
            } else {
                Mp3Mdct.oneLong(mdctIn, mdctOut)
                for (i in 0..35) mdctOut[i] *= MpaConst.win[blockType][i]
            }
            for (i in 0..17) {
                out[i + sb18] = mdctOut[i] + prevBlk[ch][sb18 + i]
                prevBlk[ch][sb18 + i] = mdctOut[18 + i]
            }
            sb18 += 18
        }
    }

    @Throws(IOException::class)
    override fun decodeFrame(frame: ByteBuffer, dst: ByteBuffer): AudioBuffer {
        val header = MpaHeader.read_header(frame)
        if (!initialized) {
            init(header)
        }
        val intensityStereo = header.mode == MpaConst.JOINT_STEREO && header.modeExtension and 0x1 != 0
        if (intensityStereo) throw RuntimeException("Intensity stereo is not supported.")
        dst.order(ByteOrder.LITTLE_ENDIAN)
        val si = Mp3Bitstream.readSideInfo(header, frame, channels)
        val reserve = frameData.position()
        frameData.put(NIOUtils.read(frame, header.frameBytes))
        frameData.flip()
        if (header.protectionBit == 0) {
            frame.short
        }
        NIOUtils.skip(frameData, reserve - si.mainDataBegin)
        val br = createBitReader(frameData)
        val scalefac = arrayOfNulls<ScaleFactors>(2)
        decodeGranule(header, dst, si, br, scalefac, 0)
        if (header.version == MpaConst.MPEG1) decodeGranule(header, dst, si, br, scalefac, 1)
        br.terminate()
        NIOUtils.relocateLeftover(frameData)
        dst.flip()
        return AudioBuffer(dst, null, 1)
    }

    @Throws(IOException::class)
    override fun getCodecMeta(data: ByteBuffer): AudioCodecMeta {
        val header = MpaHeader.read_header(data.duplicate())
        val format = AudioFormat(MpaConst.frequencies[header.version][header.sampleFreq], 16,
                if (header.mode == MpaConst.SINGLE_CHANNEL) 1 else 2, true, false)
        return AudioCodecMeta.fromAudioFormat(format)
    }

    companion object {
        private val ALL_TRUE = booleanArrayOf(true, true, true, true)
        private const val SAMPLES_PER_BAND = 18
        private const val NUM_BANDS = 32
        private const val fourByThree = 4.0 / 3.0
        fun appendSamples(buf: ByteBuffer, f: ShortArray, n: Int) {
            for (i in 0 until n) {
                buf.putShort(f[i])
            }
        }

        fun appendSamplesInterleave(buf: ByteBuffer, f0: ShortArray, f1: ShortArray, n: Int) {
            for (i in 0 until n) {
                buf.putShort(f0[i])
                buf.putShort(f1[i])
            }
        }
    }

    init {
        filter = arrayOf(null, null)
        frameData = ByteBuffer.allocate(4096)
        samples = FloatArray(32)
        mdctIn = FloatArray(18)
        mdctOut = FloatArray(36)
        dequant = Array(2) { FloatArray(576) }
        tmpOut = Array(2) { ShortArray(576) }
    }
}