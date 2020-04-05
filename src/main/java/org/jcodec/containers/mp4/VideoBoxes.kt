package org.jcodec.containers.mp4

import org.jcodec.codecs.h264.mp4.AvcCBox
import org.jcodec.containers.mp4.boxes.*

class VideoBoxes : Boxes() {
    init {
        mappings[PixelAspectExt.fourcc()] = PixelAspectExt::class.java
        mappings[AvcCBox.fourcc()] = AvcCBox::class.java
        mappings[ColorExtension.fourcc()] = ColorExtension::class.java
        mappings[GamaExtension.fourcc()] = GamaExtension::class.java
        mappings[CleanApertureExtension.fourcc()] = CleanApertureExtension::class.java
        mappings[FielExtension.fourcc()] = FielExtension::class.java
    }
}