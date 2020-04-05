package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.FileTypeBox
import org.jcodec.containers.mp4.boxes.FileTypeBox.Companion.createFileTypeBox
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class Brand private constructor(majorBrand: String, version: Int, compatible: Array<String>) {
    val fileTypeBox: FileTypeBox

    companion object {
        @JvmField
        val MOV = Brand("qt  ", 0x00000200, arrayOf("qt  "))
        @JvmField
        val MP4 = Brand("isom", 0x00000200, arrayOf("isom", "iso2", "avc1", "mp41"))
    }

    init {
        fileTypeBox = createFileTypeBox(majorBrand, version, Arrays.asList(*compatible))
    }
}