package org.jcodec.common.io

import java.io.IOException
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Implements a seekable byte channel that wraps a byte buffer
 *
 * @author The JCodec project
 */
class ByteBufferSeekableByteChannel(private val backing: ByteBuffer, private var contentLength: Int) : SeekableByteChannel {
    private var open = true
    override fun isOpen(): Boolean {
        return open
    }

    @Throws(IOException::class)
    override fun close() {
        open = false
    }

    @Throws(IOException::class)
    override fun read(dst: ByteBuffer): Int {
        if (!backing.hasRemaining() || contentLength <= 0) {
            return -1
        }
        var toRead = Math.min(backing.remaining(), dst.remaining())
        toRead = Math.min(toRead, contentLength)
        dst.put(NIOUtils.read(backing, toRead))
        contentLength = Math.max(contentLength, backing.position())
        return toRead
    }

    @Throws(IOException::class)
    override fun write(src: ByteBuffer): Int {
        val toWrite = Math.min(backing.remaining(), src.remaining())
        backing.put(NIOUtils.read(src, toWrite))
        contentLength = Math.max(contentLength, backing.position())
        return toWrite
    }

    @Throws(IOException::class)
    override fun position(): Long {
        return backing.position().toLong()
    }

    @Throws(IOException::class)
    override fun setPosition(newPosition: Long): SeekableByteChannel {
        backing.position(newPosition.toInt())
        contentLength = Math.max(contentLength, backing.position())
        return this
    }

    @Throws(IOException::class)
    override fun size(): Long {
        return contentLength.toLong()
    }

    @Throws(IOException::class)
    override fun truncate(size: Long): SeekableByteChannel {
        contentLength = size.toInt()
        return this
    }

    val contents: ByteBuffer
        get() {
            val contents = backing.duplicate()
            contents.position(0)
            contents.limit(contentLength)
            return contents
        }

    companion object {
        @JvmStatic
        fun writeToByteBuffer(buf: ByteBuffer): ByteBufferSeekableByteChannel {
            return ByteBufferSeekableByteChannel(buf, 0)
        }

        @JvmStatic
        fun readFromByteBuffer(buf: ByteBuffer): ByteBufferSeekableByteChannel {
            return ByteBufferSeekableByteChannel(buf, buf.remaining())
        }
    }

}