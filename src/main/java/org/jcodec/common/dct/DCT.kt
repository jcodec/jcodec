package org.jcodec.common.dct

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
interface DCT {
    fun decode(orig: IntArray): IntArray
}