package org.jcodec.codecs.mpeg4

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Macroblock {
    class Vector(@JvmField var x: Int, @JvmField var y: Int)

    @JvmField
    var mvs: Array<Vector>

    @JvmField
    var predValues: Array<ShortArray>

    @JvmField
    var acpredDirections: IntArray

    @JvmField
    var mode = 0

    @JvmField
    var quant = 0

    @JvmField
    var fieldDCT = false

    @JvmField
    var fieldPred = false

    @JvmField
    var fieldForTop = false

    @JvmField
    var fieldForBottom = false
    private val pmvs: Array<Vector>
    private val qmvs: Array<Vector>

    @JvmField
    var cbp = 0

    @JvmField
    var bmvs: Array<Vector>
    var bqmvs: Array<Vector>
    var amv: Vector

    @JvmField
    var mvsAvg: Vector? = null

    @JvmField
    var x = 0

    @JvmField
    var y = 0

    @JvmField
    var bound = 0

    @JvmField
    var acpredFlag = false

    @JvmField
    var predictors: ShortArray

    @JvmField
    var block: Array<ShortArray>

    @JvmField
    var coded = false

    @JvmField
    var mcsel = false

    @JvmField
    var pred: Array<ByteArray>
    fun reset(x2: Int, y2: Int, bound2: Int) {
        x = x2
        y = y2
        bound = bound2
    }

    companion object {
        const val MBPRED_SIZE = 15

        @JvmStatic
        fun vec(): Vector = Vector(0, 0)
    }

    init {
        mvs = Array(4) { vec() }
        pmvs = Array(4) { vec() }
        qmvs = Array(4) { vec() }
        bmvs = Array(4) { vec() }
        bqmvs = Array(4) { vec() }
        pred = arrayOf(ByteArray(256), ByteArray(64), ByteArray(64), ByteArray(256), ByteArray(64), ByteArray(64))
        predValues = Array(6) { ShortArray(MBPRED_SIZE) }
        acpredDirections = IntArray(6)
        amv = vec()
        predictors = ShortArray(8)
        block = Array(6) { ShortArray(64) }
    }
}