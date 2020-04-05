package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.EndianBox
import org.jcodec.containers.mp4.boxes.FormatBox

class WaveExtBoxes : Boxes() {
    init {
        mappings[FormatBox.fourcc()] = FormatBox::class.java
        mappings[EndianBox.fourcc()] = EndianBox::class.java
        //            mappings.put(EsdsBox.fourcc(), EsdsBox.class);
    }
}