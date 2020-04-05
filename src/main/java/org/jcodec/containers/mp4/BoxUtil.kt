package org.jcodec.containers.mp4

import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.containers.mp4.boxes.Header
import org.jcodec.containers.mp4.boxes.Header.Companion.createHeader
import org.jcodec.containers.mp4.boxes.Header.Companion.read
import org.jcodec.containers.mp4.boxes.NodeBox
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.platform.Platform
import java.nio.ByteBuffer

object BoxUtil {
    @JvmStatic
    fun parseBox(input: ByteBuffer?, childAtom: Header, factory: IBoxFactory): Box {
        val box = factory.newBox(childAtom)
        return if (childAtom.bodySize < Box.MAX_BOX_SIZE) {
            box.parse(input!!)
            box
        } else {
            LeafBox(createHeader("free", 8))
        }
    }

    fun parseChildBox(input: ByteBuffer, factory: IBoxFactory): Box? {
        val fork = input.duplicate()
        while (input.remaining() >= 4 && fork.int == 0) input.int
        if (input.remaining() < 4) return null
        val childAtom = read(input)
        return if (childAtom != null && input.remaining() >= childAtom.bodySize) parseBox(NIOUtils.read(input, childAtom.bodySize.toInt()), childAtom, factory) else null
    }

    @JvmStatic
    fun <T : Box?> `as`(class1: Class<T>?, box: LeafBox): T {
        return try {
            val res = Platform.newInstance(class1, arrayOf<Any>(box.header))
            res!!.parse(box.getData().duplicate())
            res
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    fun containsBox(box: NodeBox?, path: String?): Boolean {
        val b = findFirstPath(box, arrayOf(path))
        return b != null
    }

    @JvmStatic
    fun containsBox2(box: NodeBox?, path1: String?, path2: String?): Boolean {
        val b = findFirstPath(box, arrayOf(path1, path2))
        return b != null
    }

    fun writeBox(b: Box): ByteBuffer {
        val estimateSize = b.estimateSize()
        val buf = ByteBuffer.allocate(estimateSize)
        b.write(buf)
        buf.flip()
        return buf
    }
}