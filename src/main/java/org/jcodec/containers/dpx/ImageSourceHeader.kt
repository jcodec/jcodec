package org.jcodec.containers.dpx

import java.util.*

class ImageSourceHeader {
    var xOffset = 0
    var yOffset = 0
    var xCenter = 0f
    var yCenter = 0f
    var xOriginal = 0
    var yOriginal = 0
    var sourceImageFilename: String? = null
    var sourceImageDate: Date? = null
    var deviceName: String? = null
    var deviceSerial: String? = null
    var borderValidity: ShortArray = ShortArray(0)
    var aspectRatio: IntArray = IntArray(0)
}