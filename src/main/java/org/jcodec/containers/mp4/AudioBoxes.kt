package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.containers.mp4.boxes.ChannelBox
import org.jcodec.containers.mp4.boxes.WaveExtension

class AudioBoxes : Boxes() {
    init {
        mappings[WaveExtension.fourcc()] = WaveExtension::class.java
        mappings[ChannelBox.fourcc()] = ChannelBox::class.java
        mappings["esds"] = LeafBox::class.java
    }
}