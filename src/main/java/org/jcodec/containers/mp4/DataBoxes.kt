package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.AliasBox
import org.jcodec.containers.mp4.boxes.UrlBox

class DataBoxes : Boxes() {
    init {
        mappings[UrlBox.fourcc()] = UrlBox::class.java
        mappings[AliasBox.fourcc()] = AliasBox::class.java
        mappings["cios"] = AliasBox::class.java
    }
}