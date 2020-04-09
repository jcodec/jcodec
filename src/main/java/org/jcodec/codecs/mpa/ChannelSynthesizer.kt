package org.jcodec.codecs.mpa

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
internal class ChannelSynthesizer(channelnumber: Int, factor: Float) {
    private val v: Array<FloatArray>
    private var pos: Int
    private val scalefactor: Float
    private var current = 0
    fun synthesize(coeffs: FloatArray, out: ShortArray, off: Int) {
        MpaPqmf.computeButterfly(pos, coeffs)
        val next = current.inv() and 1
        distributeSamples(pos, v[current], v[next], coeffs)
        MpaPqmf.computeFilter(pos, v[current], out, off, scalefactor)
        pos = pos + 1 and 0xf
        current = next
    }

    companion object {
        private fun distributeSamples(pos: Int, dest: FloatArray, next: FloatArray, s: FloatArray) {
            for (i in 0..15) dest[(i shl 4) + pos] = s[i]
            for (i in 1..16) next[(i shl 4) + pos] = s[15 + i]
            dest[256 + pos] = 0.0f
            next[0 + pos] = -s[0]
            for (i in 0..14) dest[272 + (i shl 4) + pos] = -s[15 - i]
            for (i in 0..14) next[272 + (i shl 4) + pos] = s[30 - i]
        }
    }

    init {
        v = Array(2) { FloatArray(512) }
        scalefactor = factor
        pos = 15
    }
}