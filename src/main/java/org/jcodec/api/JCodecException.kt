package org.jcodec.api

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
open class JCodecException : Exception {
    constructor(arg0: String?) : super(arg0) {}
    constructor(e: Throwable?) : super(e) {}
}