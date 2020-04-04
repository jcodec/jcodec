package org.jcodec.containers.mp4.boxes

import org.jcodec.common.Preconditions
import org.jcodec.common.StringUtils
import org.jcodec.common.UsedViaReflection
import org.jcodec.common.io.NIOUtils
import org.jcodec.containers.mp4.IBoxFactory
import org.jcodec.platform.Platform
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * An MP4 file struncture (box).
 *
 * @author The JCodec project
 */
abstract class Box @UsedViaReflection constructor(var header: Header) {

    abstract fun parse(buf: ByteBuffer)
    fun write(buf: ByteBuffer) {
        val dup = buf.duplicate()
        NIOUtils.skip(buf, 8)
        doWrite(buf)
        header.setBodySize(buf.position() - dup.position() - 8)
        Preconditions.checkState(header.headerSize() == 8.toLong())
        header.write(dup)
    }

    protected abstract fun doWrite(out: ByteBuffer)
    abstract fun estimateSize(): Int
    val fourcc: String
        get() = header.fourcc!!

    override fun toString(): String {
        val sb = StringBuilder()
        dump(sb)
        return sb.toString()
    }

    open fun dump(sb: StringBuilder) {
        sb.append("{\"tag\":\"" + header.fourcc + "\"}")
    }

    class LeafBox(atom: Header) : Box(atom) {
        var _data: ByteBuffer? = null
        override fun parse(input: ByteBuffer) {
            _data = NIOUtils.read(input, header.bodySize.toInt())
        }

        fun getData(): ByteBuffer {
            return _data!!.duplicate()
        }

        override fun doWrite(out: ByteBuffer) {
            NIOUtils.write(out, _data)
        }

        override fun estimateSize(): Int {
            return _data!!.remaining() + Header.estimateHeaderSize(_data!!.remaining())
        }
    }

    companion object {
        const val MAX_BOX_SIZE = 128 * 1024 * 1024
        fun terminatorAtom(): Box {
            return createLeafBox(Header(Platform.stringFromBytes(ByteArray(4))), ByteBuffer.allocate(0))
        }

        @JvmStatic
        fun path(path: String?): Array<String?> {
            return StringUtils.splitC(path, '.')
        }

        @JvmStatic
        fun createLeafBox(atom: Header, data: ByteBuffer?): LeafBox {
            val leaf = LeafBox(atom)
            leaf._data = data
            return leaf
        }

        @JvmStatic
        fun parseBox(input: ByteBuffer?, childAtom: Header, factory: IBoxFactory): Box {
            val box = factory.newBox(childAtom)
            return if (childAtom.bodySize < MAX_BOX_SIZE) {
                box.parse(input!!)
                box
            } else {
                LeafBox(Header.createHeader("free", 8))
            }
        }

        @JvmStatic
        fun <T : Box?> asBox(class1: Class<T>?, box: Box): T? {
            val res = Platform.newInstance(class1, arrayOf<Any>(box.header))
            return box.reinterpret(res!!) as? T?
        }
    }

    fun reinterpret(other: Box): Box? {
        return try {
            val buffer = ByteBuffer.allocate(this.header.bodySize.toInt())
            this.doWrite(buffer)
            buffer.flip()
            other.parse(buffer)
            other
        } catch (e: Exception) {
            null
        }
    }

}