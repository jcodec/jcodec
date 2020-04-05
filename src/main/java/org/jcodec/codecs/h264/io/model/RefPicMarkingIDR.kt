package org.jcodec.codecs.h264.io.model

import org.jcodec.platform.Platform

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Reference picture marking used for IDR frames
 *
 * @author The JCodec project
 */
class RefPicMarkingIDR(var isDiscardDecodedPics: Boolean, var isUseForlongTerm: Boolean) {

    override fun toString(): String {
        return Platform.toJSON(this)
    }

}