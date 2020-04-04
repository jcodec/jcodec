package org.jcodec.containers.mp4.boxes

import org.jcodec.containers.mp4.Boxes
import org.jcodec.containers.mp4.IBoxFactory
import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.platform.Platform

class SimpleBoxFactory(private val boxes: Boxes) : IBoxFactory {
    override fun newBox(header: Header): Box {
        val claz = boxes.toClass(header.fourcc!!) ?: return LeafBox(header)
        return Platform.newInstance(claz, arrayOf<Any>(header))!!
    }

}