package org.jcodec.codecs.vpx

import org.jcodec.common.and
import org.jcodec.common.shr
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class VPXBooleanDecoder {
    var bit_count /* # of bits shifted out of value, at most 7 */ = 0
    var input: ByteBuffer? = null
    var offset /* pointer to next compressed data byte */ = 0
    var range /* always identical to encoder's range */ = 0
    var value /* contains at least 24 significant bits */ = 0
    var callCounter: Long = 0
    private val debugName: String? = null

    constructor(input: ByteBuffer?, offset: Int) {
        this.input = input
        this.offset = offset
        initBoolDecoder()
    }

    /**
     * Empty constructor just for testing
     */
    protected constructor() {}

    fun initBoolDecoder() {
        value = 0 /* value = first 16 input bits */

        // data.position(offset);
        value = input!!.get() and 0xFF shl 8 // readUnsignedByte() << 8;
        // value = (data[offset]) << 8;
        offset++
        range = 255 /* initial range is full */
        bit_count = 0 /* have not yet shifted out any bits */
    }

    fun readBitEq(): Int {
        return readBit(128)
    }

    open fun readBit(probability: Int): Int {
        var bit = 0
        var range = range
        var value = value
        val split = 1 + ((range - 1) * probability shr 8)
        val bigsplit = split shl 8
        callCounter++
        //            System.out.println();
//            System.out.println("this.range: " + this.range + " binary: " + Integer.toBinaryString(this.range));
//            System.out.println("split: " + split + " binary: " + Integer.toBinaryString(split));
//            System.out.println("SPLIT: " + bigsplit + " binary: " + Integer.toBinaryString(bigsplit));
//            System.out.println("value: " + value + " binary: " + Integer.toBinaryString(value));
        range = split
        if (value >= bigsplit) {
            range = this.range - range
            value = value - bigsplit
            bit = 1
        }
        var count = bit_count
        val shift = leadingZeroCountInByte(range.toByte())
        range = range shl shift
        value = value shl shift
        count -= shift
        if (count <= 0) {
            value = value or (input!!.get() and 0xFF shl -count)
            //                System.out.println("read value: " + value + " binary: " + Integer.toBinaryString(value));
            offset++
            count += 8
        }
        bit_count = count
        this.value = value
        this.range = range
        return bit
    }

    /*
         * Convenience function reads a "literal", that is, a "num_bits" wide unsigned value whose bits come high- to low-order, with each bit encoded at probability 128 (i.e., 1/2).
         */
    fun decodeInt(sizeInBits: Int): Int {
        var sizeInBits = sizeInBits
        var v = 0
        while (sizeInBits-- > 0) v = v shl 1 or readBit(128)
        return v
    }
    /* root: "0", "1" subtrees */ /* "00" = 0th value, "01" = 1st value */ /* "10" = 2nd value, "11" = 3rd value */
    /**
     *
     * General formula in VP8 trees.
     *
     *  *  if tree element is a positive number it is treated as index of the child elements <pre>tree[i] > 0</pre>
     *
     *  *  left child is assumed to have index <pre>i</pre> and value <pre>tree[i]</pre>
     *  *  right child is assumed to have index <pre>i+1</pre> and value <pre>tree[i+1]</pre>
     *
     *
     *  *  a negative tree value means a leaf node was reached and it's negated value should be returned <pre>-tree[i]</pre>
     *
     *
     * Here's a real example of a tree coded according to this formula in VP8 spec.
     * <pre>
     * const tree_index mb_segment_tree [2 * (4-1)] =
     * // +-------+---+
     * // |       |   |
     * { 2,  4, -0, -1, -2, -3 };
     * //     |           |   |
     * //     +-----------+---+
    </pre> *
     *
     * If presented in hierarchical form this tree would look like:
     * <pre>
     * +---------------+
     * |      root     |
     * |     /    \    |
     * |    2      4   |
     * |   / \    / \  |
     * | -0  -1 -2  -3 |
     * +---------------+
     * <pre>
     *
     * On the other hand probabilities are coded only for non-leaf nodes.
     * Thus tree array has twice as many nodes as probabilities array
     * Consider (3>>1) == 1 == (2>>1), and (0>>1) == 0 == (1>>1)
     * Thus single probability element refers to single parent element in tree.
     * if (using that probability) a '0' is coded, algorithm goes to the left
     * branch, correspondingly if '1' is coded, algorithm goes to
     * the right branch (see tree structure above).
     *
     * The process is repeated until a negative tree element is found.
     *
    </pre></pre> */
    fun readTree(tree: IntArray, probability: IntArray): Int {
        var i = 0

        /*
             * 1. pick corresponding probability probability[i >> 1]
             * 2. pick left or right branch from coded info decodeBool(probability)
             * 3. tree[i+decodedBool] get corresponding (left of right) value
             * 4. repeat until tree[i+decodedBool] is positive
             */while (tree[i + readBit(probability[i shr 1])].also { i = it } > 0) {
        }
        return -i /* negate the return value */
    }

    //There is a method in the class (or one of its parents) having the same name with the method named [readTree] but is less generic
    fun readTree3(tree: IntArray, prob0: Int, prob1: Int): Int {
        var i = 0
        if (tree[i + readBit(prob0)].also { i = it } > 0) {
            while (tree[i + readBit(prob1)].also { i = it } > 0);
        }
        return -i /* negate the return value */
    }

    fun readTreeSkip(t: IntArray,  /* tree specification */
                     p: IntArray,  /* corresponding interior node probabilities */
                     skip_branches: Int): Int {
        var i = skip_branches * 2 /* begin at root */

        /* Descend tree until leaf is reached */while (t[i + readBit(p[i shr 1])].also { i = it } > 0) {
        }
        return -i /* return value is negation of nonpositive index */
    }

    fun seek() {
        input!!.position(offset)
    }

    override fun toString(): String {
        return "bc: $value"
    }

    companion object {
        @JvmStatic
        fun getBitInBytes(bs: ByteArray, i: Int): Int {
            val byteIndex = i shr 3
            val bitIndex = i and 0x07
            return bs[byteIndex] shr 0x07 - bitIndex and 0x01
        }

        fun getBitsInBytes(bytes: ByteArray, idx: Int, len: Int): Int {
            var `val` = 0
            for (i in 0 until len) {
                `val` = `val` shl 1 or getBitInBytes(bytes, idx + i)
            }
            return `val`
        }

        @JvmStatic
        fun leadingZeroCountInByte(b: Byte): Int {
            val i: Int = b and 0xFF
            return if (i >= 128 || i == 0) 0 else Integer.numberOfLeadingZeros(b.toInt()) - 24
            /*
             * if-less alternative:
             * http://aggregate.ee.engr.uky.edu/MAGIC/#Leading Zero Count
             */
        }
    }
}