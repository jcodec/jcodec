package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.BitRateBox
import org.jcodec.containers.mp4.boxes.TextConfigBox
import org.jcodec.containers.mp4.boxes.URIBox
import org.jcodec.containers.mp4.boxes.URIInitBox

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MetaDataBoxes : Boxes() {
    init {
        mappings[URIBox.fourcc()] = URIBox::class.java
        mappings[URIInitBox.fourcc()] = URIInitBox::class.java
        mappings[BitRateBox.fourcc()] = BitRateBox::class.java
        mappings[TextConfigBox.fourcc()] = TextConfigBox::class.java
    }
}