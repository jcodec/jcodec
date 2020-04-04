package org.jcodec.containers.mp4.boxes

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.common.io.StringReader
import org.jcodec.common.logging.Logger
import org.jcodec.platform.Platform
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * An MP4 file structure (atom)
 *
 * @author The JCodec project
 */
class Header(val fourcc: String?) {
    var size: Long = 0
        private set
    private var lng = false

    @Throws(IOException::class)
    fun skip(di: InputStream?) {
        StringReader.sureSkip(di, size - headerSize())
    }

    fun headerSize(): Long {
        return if (lng || size > MAX_UNSIGNED_INT) 16 else 8
    }

    @Throws(IOException::class)
    fun readContents(di: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        for (i in 0 until size - headerSize()) {
            baos.write(di.read())
        }
        return baos.toByteArray()
    }

    val bodySize: Long
        get() = size - headerSize()

    fun setBodySize(length: Int) {
        size = length + headerSize()
    }

    fun write(out: ByteBuffer) {
        if (size > MAX_UNSIGNED_INT) out.putInt(1) else out.putInt(size.toInt())
        val bt = JCodecUtil2.asciiString(fourcc)
        if (bt != null && bt.size == 4) out.put(bt) else out.put(FOURCC_FREE)
        if (size > MAX_UNSIGNED_INT) {
            out.putLong(size)
        }
    }

    @Throws(IOException::class)
    fun writeChannel(output: SeekableByteChannel) {
        val bb = ByteBuffer.allocate(16)
        write(bb)
        bb.flip()
        output.write(bb)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (fourcc?.hashCode() ?: 0)
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null) return false
        if (javaClass != obj.javaClass) return false
        val other = obj as Header
        if (fourcc == null) {
            if (other.fourcc != null) return false
        } else if (fourcc != other.fourcc) return false
        return true
    }

    companion object {
        @JvmField
        val FOURCC_FREE = byteArrayOf('f'.toByte(), 'r'.toByte(), 'e'.toByte(), 'e'.toByte())
        private const val MAX_UNSIGNED_INT = 0x100000000L
        @JvmStatic
        fun createHeader(fourcc: String?, size: Long): Header {
            val header = Header(fourcc)
            header.size = size
            return header
        }

        fun newHeader(fourcc: String?, size: Long, lng: Boolean): Header {
            val header = Header(fourcc)
            header.size = size
            header.lng = lng
            return header
        }

        @JvmStatic
        fun read(input: ByteBuffer): Header? {
            var size: Long = 0
            while (input.remaining() >= 4 && Platform.unsignedInt(input.int).also { size = it } == 0L);
            if (input.remaining() < 4 || size < 8 && size != 1L) {
                Logger.error("Broken atom of size $size")
                return null
            }
            val fourcc = NIOUtils.readString(input, 4)
            var lng = false
            if (size == 1L) {
                if (input.remaining() >= 8) {
                    lng = true
                    size = input.long
                } else {
                    Logger.error("Broken atom of size $size")
                    return null
                }
            }
            return newHeader(fourcc, size, lng)
        }

        fun estimateHeaderSize(size: Int): Int {
            return if (size + 8 > MAX_UNSIGNED_INT) 16 else 8
        }
    }

}