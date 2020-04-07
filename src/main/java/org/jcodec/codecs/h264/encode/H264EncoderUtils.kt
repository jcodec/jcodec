package org.jcodec.codecs.h264.encode

/**
 * Contains utility functions commonly used in H264 encoder
 *
 * @author Stanislav Vitvitskyy
 */
object H264EncoderUtils {
    fun median(a: Int, ar: Boolean, b: Int, br: Boolean, c: Int, cr: Boolean, d: Int, dr: Boolean, aAvb: Boolean,
               bAvb: Boolean, cAvb: Boolean, dAvb: Boolean): Int {
        var a = a
        var ar = ar
        var b = b
        var br = br
        var c = c
        var cr = cr
        var bAvb = bAvb
        var cAvb = cAvb
        ar = ar and aAvb
        br = br and bAvb
        cr = cr and cAvb
        if (!cAvb) {
            c = d
            cr = dr
            cAvb = dAvb
        }
        if (aAvb && !bAvb && !cAvb) {
            c = a
            b = c
            cAvb = aAvb
            bAvb = cAvb
        }
        a = if (aAvb) a else 0
        b = if (bAvb) b else 0
        c = if (cAvb) c else 0
        if (ar && !br && !cr) return a else if (br && !ar && !cr) return b else if (cr && !ar && !br) return c
        return a + b + c - Math.min(Math.min(a, b), c) - Math.max(Math.max(a, b), c)
    }

    fun mse(orig: IntArray, enc: IntArray, w: Int, h: Int): Int {
        var sum = 0
        var i = 0
        var off = 0
        while (i < h) {
            var j = 0
            while (j < w) {
                val diff = orig[off] - enc[off]
                sum += diff * diff
                j++
                off++
            }
            i++
        }
        return sum / (w * h)
    }
}