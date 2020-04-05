package org.jcodec.codecs.h264.io.model

import org.jcodec.platform.Platform

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A script of instructions applied to reference picture list
 *
 * @author The JCodec project
 */
class RefPicMarking(val instructions: Array<Instruction>) {
    enum class InstrType {
        REMOVE_SHORT, REMOVE_LONG, CONVERT_INTO_LONG, TRUNK_LONG, CLEAR, MARK_LONG
    }

    class Instruction(val type: InstrType, val arg1: Int, val arg2: Int)

    override fun toString(): String {
        return Platform.toJSON(this)
    }

}