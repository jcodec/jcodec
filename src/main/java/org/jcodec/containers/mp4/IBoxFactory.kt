package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Header

interface IBoxFactory {
    fun newBox(header: Header): Box
}