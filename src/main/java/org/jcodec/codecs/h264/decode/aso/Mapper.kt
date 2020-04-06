package org.jcodec.codecs.h264.decode.aso

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
interface Mapper {
    fun leftAvailable(index: Int): Boolean
    fun topAvailable(index: Int): Boolean
    fun getAddress(index: Int): Int
    fun getMbX(mbIndex: Int): Int
    fun getMbY(mbIndex: Int): Int
    fun topRightAvailable(mbIndex: Int): Boolean
    fun topLeftAvailable(mbIdx: Int): Boolean
}