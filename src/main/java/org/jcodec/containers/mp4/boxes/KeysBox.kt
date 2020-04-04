package org.jcodec.containers.mp4.boxes

import org.jcodec.containers.mp4.Boxes
import org.jcodec.containers.mp4.DefaultBoxes
import org.jcodec.containers.mp4.IBoxFactory
import org.jcodec.containers.mp4.boxes.MdtaBox
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class KeysBox(atom: Header) : NodeBox(atom) {
    private class LocalBoxes internal constructor() : DefaultBoxes() {
        //Initializing blocks are not supported by Javascript.
        init {
            mappings[MdtaBox.fourcc()] = MdtaBox::class.java
        }
    }

    override fun parse(input: ByteBuffer) {
        val vf = input.int
        val cnt = input.int
        super.parse(input)
    }

    override fun doWrite(out: ByteBuffer) {
        out.putInt(0)
        out.putInt(_boxes.size)
        super.doWrite(out)
    }

    override fun estimateSize(): Int {
        return 8 + super.estimateSize()
    }

    companion object {
        private const val FOURCC = "keys"
        @JvmStatic
        fun createKeysBox(): KeysBox {
            return KeysBox(Header.createHeader(FOURCC, 0))
        }

        @JvmStatic
        fun fourcc(): String {
            return FOURCC
        }
    }

    override fun setFactory(factory: IBoxFactory) {
        super.setFactory(factory)
    }

    init {
        _factory = SimpleBoxFactory(LocalBoxes())
    }
}