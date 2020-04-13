package org.jcodec.containers.dpx

import java.util.*

class FileHeader {
    var magic // SDPX big endian (0x53445058) or XPDS little endinan
            = 0
    var imageOffset //Offset to image data in bytes
            = 0
    @JvmField
    var version: String? = null
    var ditto = 0
    var filename: String? = null
    var created: Date? = null
    var filesize = 0
    var creator: String? = null
    var projectName: String? = null
    var copyright: String? = null
    var encKey = 0

    //        public byte[] reserved;
    var genericHeaderLength = 0
    var industryHeaderLength = 0
    var userHeaderLength = 0
}