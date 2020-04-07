package org.jcodec.codecs.h264.encode

import org.jcodec.codecs.h264.io.CAVLC

class EncodingContext(private val mbWidth: Int, private val mbHeight: Int) {
    @JvmField
    var cavlc: Array<CAVLC?>? = null
    @JvmField
    var leftRow: Array<ByteArray>
    @JvmField
    var topLine: Array<ByteArray>
    @JvmField
    var topLeft: ByteArray
    var mvTopX: IntArray
    var mvTopY: IntArray
    var mvTopR: IntArray
    var mvLeftX: IntArray
    var mvLeftY: IntArray
    var mvLeftR: IntArray
    var mvTopLeftX = 0
    var mvTopLeftY = 0
    var mvTopLeftR = 0
    @JvmField
    var prevQp = 0
    fun update(mb: EncodedMB) {
        topLeft[0] = topLine[0][(mb.mbX shl 4) + 15]
        topLeft[1] = topLine[1][(mb.mbX shl 3) + 7]
        topLeft[2] = topLine[2][(mb.mbX shl 3) + 7]
        System.arraycopy(mb.pixels.getPlaneData(0), 240, topLine[0], mb.mbX shl 4, 16)
        System.arraycopy(mb.pixels.getPlaneData(1), 56, topLine[1], mb.mbX shl 3, 8)
        System.arraycopy(mb.pixels.getPlaneData(2), 56, topLine[2], mb.mbX shl 3, 8)
        copyCol(mb.pixels.getPlaneData(0), 15, 16, leftRow[0])
        copyCol(mb.pixels.getPlaneData(1), 7, 8, leftRow[1])
        copyCol(mb.pixels.getPlaneData(2), 7, 8, leftRow[2])
        mvTopLeftX = mvTopX[mb.mbX shl 2]
        mvTopLeftY = mvTopY[mb.mbX shl 2]
        mvTopLeftR = mvTopR[mb.mbX shl 2]
        for (i in 0..3) {
            mvTopX[(mb.mbX shl 2) + i] = mb.mx[12 + i]
            mvTopY[(mb.mbX shl 2) + i] = mb.my[12 + i]
            mvTopR[(mb.mbX shl 2) + i] = mb.mr[12 + i]
            mvLeftX[i] = mb.mx[i shl 2]
            mvLeftY[i] = mb.my[i shl 2]
            mvLeftR[i] = mb.mr[i shl 2]
        }
    }

    private fun copyCol(planeData: ByteArray, off: Int, stride: Int, out: ByteArray) {
        var off = off
        for (i in out.indices) {
            out[i] = planeData[off]
            off += stride
        }
    }

    fun fork(): EncodingContext {
        val ret = EncodingContext(mbWidth, mbHeight)
        ret.cavlc = arrayOfNulls(3)
        for (i in 0..2) {
            System.arraycopy(leftRow[i], 0, ret.leftRow[i], 0, leftRow[i].size)
            System.arraycopy(topLine[i], 0, ret.topLine[i], 0, topLine[i].size)
            ret.topLeft[i] = topLeft[i]
            ret.cavlc!![i] = cavlc!![i]!!.fork()
        }
        System.arraycopy(mvTopX, 0, ret.mvTopX, 0, ret.mvTopX.size)
        System.arraycopy(mvTopY, 0, ret.mvTopY, 0, ret.mvTopY.size)
        System.arraycopy(mvTopR, 0, ret.mvTopR, 0, ret.mvTopR.size)
        System.arraycopy(mvLeftX, 0, ret.mvLeftX, 0, ret.mvLeftX.size)
        System.arraycopy(mvLeftY, 0, ret.mvLeftY, 0, ret.mvLeftY.size)
        System.arraycopy(mvLeftR, 0, ret.mvLeftR, 0, ret.mvLeftR.size)
        ret.mvTopLeftX = mvTopLeftX
        ret.mvTopLeftY = mvTopLeftY
        ret.mvTopLeftR = mvTopLeftR
        ret.prevQp = prevQp
        return ret
    }

    init {
        leftRow = arrayOf(ByteArray(16), ByteArray(8), ByteArray(8))
        topLine = arrayOf(ByteArray(mbWidth shl 4), ByteArray(mbWidth shl 3), ByteArray(mbWidth shl 3))
        topLeft = ByteArray(3)
        mvTopX = IntArray(mbWidth shl 2)
        mvTopY = IntArray(mbWidth shl 2)
        mvTopR = IntArray(mbWidth shl 2)
        mvLeftX = IntArray(4)
        mvLeftY = IntArray(4)
        mvLeftR = IntArray(4)
    }
}