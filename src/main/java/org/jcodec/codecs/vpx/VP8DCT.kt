package org.jcodec.codecs.vpx

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @see http://static.googleusercontent.com/external_content/untrusted_dlcp/research.google.com/de//pubs/archive/37073.pdf
 *
 * @see http://jpegclub.org/jidctred/
 *
 * @see http://www3.matapp.unimib.it/corsi-2007-2008/matematica/istituzioni-di-analisi-numerica/jpeg/papers/11-multiplications.pdf
 *
 * @see http://static.googleusercontent.com/external_content/untrusted_dlcp/research.google.com/de//pubs/archive/37073.pdf
 * <pre>
</pre> *
 *
 *
 * @author The JCodec project
 */
object VP8DCT {
    private const val cospi8sqrt2minus1 = 20091
    private const val sinpi8sqrt2 = 35468
    @JvmStatic
    fun decodeDCT(input: IntArray): IntArray {
        var i: Int
        var a1: Int
        var b1: Int
        var c1: Int
        var d1: Int
        var offset = 0
        val output = IntArray(16)
        var temp1: Int
        var temp2: Int
        i = 0
        while (i < 4) {
            a1 = input[offset + 0] + input[offset + 8]
            b1 = input[offset + 0] - input[offset + 8]
            temp1 = input[offset + 4] * sinpi8sqrt2 shr 16
            temp2 = (input[offset + 12]
                    + (input[offset + 12] * cospi8sqrt2minus1 shr 16))
            c1 = temp1 - temp2
            temp1 = (input[offset + 4]
                    + (input[offset + 4] * cospi8sqrt2minus1 shr 16))
            temp2 = input[offset + 12] * sinpi8sqrt2 shr 16
            d1 = temp1 + temp2
            output[offset + 0 * 4] = a1 + d1
            output[offset + 3 * 4] = a1 - d1
            output[offset + 1 * 4] = b1 + c1
            output[offset + 2 * 4] = b1 - c1
            offset++
            i++
        }
        offset = 0
        i = 0
        while (i < 4) {
            a1 = output[offset * 4 + 0] + output[offset * 4 + 2]
            b1 = output[offset * 4 + 0] - output[offset * 4 + 2]
            temp1 = output[offset * 4 + 1] * sinpi8sqrt2 shr 16
            temp2 = (output[offset * 4 + 3]
                    + (output[offset * 4 + 3] * cospi8sqrt2minus1 shr 16))
            c1 = temp1 - temp2
            temp1 = (output[offset * 4 + 1]
                    + (output[offset * 4 + 1] * cospi8sqrt2minus1 shr 16))
            temp2 = output[offset * 4 + 3] * sinpi8sqrt2 shr 16
            d1 = temp1 + temp2
            output[offset * 4 + 0] = a1 + d1 + 4 shr 3
            output[offset * 4 + 3] = a1 - d1 + 4 shr 3
            output[offset * 4 + 1] = b1 + c1 + 4 shr 3
            output[offset * 4 + 2] = b1 - c1 + 4 shr 3
            offset++
            i++
        }
        return output
    }

    @JvmStatic
    fun encodeDCT(input: IntArray): IntArray {
        var i: Int
        var a1: Int
        var b1: Int
        var c1: Int
        var d1: Int
        var ip = 0
        val output = IntArray(input.size)
        var op = 0
        i = 0
        while (i < 4) {
            a1 = input[ip + 0] + input[ip + 3] shl 3
            b1 = input[ip + 1] + input[ip + 2] shl 3
            c1 = input[ip + 1] - input[ip + 2] shl 3
            d1 = input[ip + 0] - input[ip + 3] shl 3
            output[op + 0] = a1 + b1
            output[op + 2] = a1 - b1
            output[op + 1] = c1 * 2217 + d1 * 5352 + 14500 shr 12
            output[op + 3] = d1 * 2217 - c1 * 5352 + 7500 shr 12
            ip += 4
            op += 4
            i++
        }
        ip = 0
        op = 0
        i = 0
        while (i < 4) {
            a1 = output[ip + 0] + output[ip + 12]
            b1 = output[ip + 4] + output[ip + 8]
            c1 = output[ip + 4] - output[ip + 8]
            d1 = output[ip + 0] - output[ip + 12]
            output[op + 0] = a1 + b1 + 7 shr 4
            output[op + 8] = a1 - b1 + 7 shr 4
            output[op + 4] = (c1 * 2217 + d1 * 5352 + 12000 shr 16) + if (d1 != 0) 1 else 0
            output[op + 12] = d1 * 2217 - c1 * 5352 + 51000 shr 16
            ip++
            op++
            i++
        }
        return output
    }

    @JvmStatic
    fun decodeWHT(input: IntArray): IntArray {
        var i: Int
        var a1: Int
        var b1: Int
        var c1: Int
        var d1: Int
        var a2: Int
        var b2: Int
        var c2: Int
        var d2: Int
        val output = IntArray(16)
        val diff = Array(4) { IntArray(4) }
        var offset = 0
        i = 0
        while (i < 4) {
            a1 = input[offset + 0] + input[offset + 12]
            b1 = input[offset + 4] + input[offset + 8]
            c1 = input[offset + 4] - input[offset + 8]
            d1 = input[offset + 0] - input[offset + 12]
            output[offset + 0] = a1 + b1
            output[offset + 4] = c1 + d1
            output[offset + 8] = a1 - b1
            output[offset + 12] = d1 - c1
            offset++
            i++
        }
        offset = 0
        i = 0
        while (i < 4) {
            a1 = output[offset + 0] + output[offset + 3]
            b1 = output[offset + 1] + output[offset + 2]
            c1 = output[offset + 1] - output[offset + 2]
            d1 = output[offset + 0] - output[offset + 3]
            a2 = a1 + b1
            b2 = c1 + d1
            c2 = a1 - b1
            d2 = d1 - c1
            output[offset + 0] = a2 + 3 shr 3
            output[offset + 1] = b2 + 3 shr 3
            output[offset + 2] = c2 + 3 shr 3
            output[offset + 3] = d2 + 3 shr 3
            diff[0][i] = a2 + 3 shr 3
            diff[1][i] = b2 + 3 shr 3
            diff[2][i] = c2 + 3 shr 3
            diff[3][i] = d2 + 3 shr 3
            offset += 4
            i++
        }
        return output
    }

    @JvmStatic
    fun encodeWHT(input: IntArray): IntArray {
        var i: Int
        var a1: Int
        var b1: Int
        var c1: Int
        var d1: Int
        var a2: Int
        var b2: Int
        var c2: Int
        var d2: Int
        var inputOffset = 0
        var outputOffset = 0
        val output = IntArray(input.size)
        i = 0
        while (i < 4) {
            /**
             *
             */
            a1 = input[inputOffset + 0] + input[inputOffset + 2] shl 2
            d1 = input[inputOffset + 1] + input[inputOffset + 3] shl 2
            c1 = input[inputOffset + 1] - input[inputOffset + 3] shl 2
            b1 = input[inputOffset + 0] - input[inputOffset + 2] shl 2
            output[outputOffset + 0] = a1 + d1 + if (a1 != 0) 1 else 0
            output[outputOffset + 1] = b1 + c1
            output[outputOffset + 2] = b1 - c1
            output[outputOffset + 3] = a1 - d1
            inputOffset += 4
            outputOffset += 4
            i++
        }
        inputOffset = 0
        outputOffset = 0
        i = 0
        while (i < 4) {
            a1 = output[inputOffset + 0] + output[inputOffset + 8]
            d1 = output[inputOffset + 4] + output[inputOffset + 12]
            c1 = output[inputOffset + 4] - output[inputOffset + 12]
            b1 = output[inputOffset + 0] - output[inputOffset + 8]
            a2 = a1 + d1
            b2 = b1 + c1
            c2 = b1 - c1
            d2 = a1 - d1
            a2 += if (a2 < 0) 1 else 0
            b2 += if (b2 < 0) 1 else 0
            c2 += if (c2 < 0) 1 else 0
            d2 += if (d2 < 0) 1 else 0
            output[outputOffset + 0] = a2 + 3 shr 3
            output[outputOffset + 4] = b2 + 3 shr 3
            output[outputOffset + 8] = c2 + 3 shr 3
            output[outputOffset + 12] = d2 + 3 shr 3
            inputOffset++
            outputOffset++
            i++
        }
        return output
    }
}