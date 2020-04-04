package org.jcodec.containers.mp4.boxes

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.io.NIOUtils
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * File type box
 *
 *
 * @author The JCodec project
 */
class FileTypeBox(header: Header) : Box(header) {
    var majorBrand: String? = null
        private set
    private var minorVersion = 0
    private var compBrands: MutableCollection<String>
    override fun parse(input: ByteBuffer) {
        majorBrand = NIOUtils.readString(input, 4)
        minorVersion = input.int
        var brand: String = ""
        while (input.hasRemaining() && NIOUtils.readString(input, 4).also { brand = it } != null) {
            compBrands.add(brand)
        }
    }

    fun getCompBrands(): Collection<String> {
        return compBrands
    }

    public override fun doWrite(out: ByteBuffer) {
        out.put(JCodecUtil2.asciiString(majorBrand))
        out.putInt(minorVersion)
        for (string in compBrands) {
            out.put(JCodecUtil2.asciiString(string))
        }
    }

    override fun estimateSize(): Int {
        var size = 5 + 8
        for (string in compBrands) {
            size += JCodecUtil2.asciiString(string).size
        }
        return size
    }

    companion object {
        @JvmStatic
        fun fourcc(): String {
            return "ftyp"
        }

        @JvmStatic
        fun createFileTypeBox(majorBrand: String?, minorVersion: Int, compBrands: MutableCollection<String>): FileTypeBox {
            val ftyp = FileTypeBox(Header(fourcc()))
            ftyp.majorBrand = majorBrand
            ftyp.minorVersion = minorVersion
            ftyp.compBrands = compBrands
            return ftyp
        }
    }

    init {
        compBrands = LinkedList()
    }
}