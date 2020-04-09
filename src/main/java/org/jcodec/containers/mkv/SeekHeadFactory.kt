package org.jcodec.containers.mkv

import org.jcodec.containers.mkv.MKVType.Companion.createByType
import org.jcodec.containers.mkv.boxes.EbmlBase
import org.jcodec.containers.mkv.boxes.EbmlBin
import org.jcodec.containers.mkv.boxes.EbmlMaster
import org.jcodec.containers.mkv.boxes.EbmlUint
import org.jcodec.containers.mkv.util.EbmlUtil.ebmlLength
import org.jcodec.containers.mkv.util.EbmlUtil.toHexString
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
class SeekHeadFactory {
    @JvmField
    var a: MutableList<SeekMock>
    var currentDataOffset: Long = 0
    fun add(e: EbmlBase) {
        val z = SeekMock.make(e)
        z.dataOffset = currentDataOffset
        z.seekPointerSize = EbmlUint.calculatePayloadSize(z.dataOffset)
        currentDataOffset += z.size.toLong()
        //        System.out.println("Added id:"+Reader.printAsHex(z.id)+" offset:"+z.dataOffset+" size:"+z.size+" seekpointer size:"+z.seekPointerSize);
        a.add(z)
    }

    fun indexSeekHead(): EbmlMaster {
        val seekHeadSize = computeSeekHeadSize()
        val seekHead = createByType<EbmlMaster>(MKVType.SeekHead)
        for (z in a) {
            val seek = createByType<EbmlMaster>(MKVType.Seek)
            val seekId = createByType<EbmlBin>(MKVType.SeekID)
            seekId.setBuf(ByteBuffer.wrap(z.id))
            seek.add(seekId)
            val seekPosition = createByType<EbmlUint>(MKVType.SeekPosition)
            seekPosition.uint = z.dataOffset + seekHeadSize
            if (seekPosition._data!!.limit() != z.seekPointerSize) System.err.println("estimated size of seekPosition differs from the one actually used. ElementId: " + toHexString(z.id) + " " + seekPosition.getData().limit() + " vs " + z.seekPointerSize)
            seek.add(seekPosition)
            seekHead.add(seek)
        }
        val mux = seekHead.getData()
        if (mux.limit() != seekHeadSize) System.err.println("estimated size of seekHead differs from the one actually used. " + mux.limit() + " vs " + seekHeadSize)
        return seekHead
    }

    fun computeSeekHeadSize(): Int {
        var seekHeadSize = estimateSize()
        var reindex = false
        do {
            reindex = false
            for (z in a) {
                val minSize = EbmlUint.calculatePayloadSize(z.dataOffset + seekHeadSize)
                if (minSize > z.seekPointerSize) {
                    println("Size " + seekHeadSize + " seems too small for element " + toHexString(z.id) + " increasing size by one.")
                    z.seekPointerSize += 1
                    seekHeadSize += 1
                    reindex = true
                    break
                } else if (minSize < z.seekPointerSize) {
                    throw RuntimeException("Downsizing the index is not well thought through.")
                }
            }
        } while (reindex)
        return seekHeadSize
    }

    fun estimateSize(): Int {
        var s: Int = MKVType.SeekHead.id.size + 1
        s += estimeteSeekSize(a[0].id.size, 1)
        for (i in 1 until a.size) {
            s += estimeteSeekSize(a[i].id.size, a[i].seekPointerSize)
        }
        return s
    }

    class SeekMock {
        @JvmField
        var dataOffset: Long = 0

        @JvmField
        var id: ByteArray = ByteArray(0)

        @JvmField
        var size = 0

        @JvmField
        var seekPointerSize = 0

        companion object {
            fun make(e: EbmlBase): SeekMock {
                val z = SeekMock()
                z.id = e.id
                z.size = e.size().toInt()
                return z
            }
        }
    }

    companion object {
        @JvmStatic
        fun estimeteSeekSize(idLength: Int, offsetSizeInBytes: Int): Int {
            val seekIdSize: Int = MKVType.SeekID.id.size + ebmlLength(idLength.toLong()) + idLength
            val seekPositionSize: Int = MKVType.SeekPosition.id.size + ebmlLength(offsetSizeInBytes.toLong()) + offsetSizeInBytes
            return MKVType.Seek.id.size + ebmlLength(seekIdSize + seekPositionSize.toLong()) + seekIdSize + seekPositionSize
        }
    }

    init {
        a = ArrayList()
    }
}