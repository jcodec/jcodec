package org.jcodec.containers.mp4.boxes

import org.jcodec.common.JCodecUtil2
import org.jcodec.common.io.NIOUtils
import org.jcodec.platform.Platform
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class AliasBox(atom: Header) : FullBox(atom) {
    private var type: String? = null
    private var recordSize: Short = 0
    private var _version: Short = 0
    private var kind: Short = 0
    private var volumeName: String? = null
    private var volumeCreateDate = 0
    private var volumeSignature: Short = 0
    private var volumeType: Short = 0
    private var parentDirId = 0
    var fileName: String? = null
        private set
    private var fileNumber = 0
    private var createdLocalDate = 0
    private var fileTypeName: String? = null
    private var creatorName: String? = null
    private var nlvlFrom: Short = 0
    private var nlvlTo: Short = 0
    private var volumeAttributes = 0
    private var fsId: Short = 0
    private var extra: MutableList<ExtraField>? = null

    class ExtraField(var type: Short, var len: Int, var data: ByteArray) {
        override fun toString(): String {
            return Platform.stringFromCharset4(data, 0, len, if (type.toInt() == 14 || type.toInt() == 15) Platform.UTF_16 else Platform.UTF_8)
        }

    }

    override fun parse(`is`: ByteBuffer) {
        super.parse(`is`)
        if (flags and 0x1 != 0) // self ref
            return
        type = NIOUtils.readString(`is`, 4)
        recordSize = `is`.short
        _version = `is`.short
        kind = `is`.short
        volumeName = NIOUtils.readPascalStringL(`is`, 27)
        volumeCreateDate = `is`.int
        volumeSignature = `is`.short
        volumeType = `is`.short
        parentDirId = `is`.int
        fileName = NIOUtils.readPascalStringL(`is`, 63)
        fileNumber = `is`.int
        createdLocalDate = `is`.int
        fileTypeName = NIOUtils.readString(`is`, 4)
        creatorName = NIOUtils.readString(`is`, 4)
        nlvlFrom = `is`.short
        nlvlTo = `is`.short
        volumeAttributes = `is`.int
        fsId = `is`.short
        NIOUtils.skip(`is`, 10)
        val _extra = ArrayList<ExtraField>()
        while (true) {
            val type = `is`.short
            if (type.toInt() == -1) break
            val len = `is`.short.toInt()
            val bs = NIOUtils.toArray(NIOUtils.read(`is`, len + 1 and -0x2)) ?: break
            _extra.add(ExtraField(type, len, bs))
        }
        extra = _extra
    }

    override fun doWrite(out: ByteBuffer) {
        super.doWrite(out)
        if (flags and 0x1 != 0) // self ref
            return
        out.put(JCodecUtil2.asciiString(type), 0, 4)
        out.putShort(recordSize)
        out.putShort(_version)
        out.putShort(kind)
        NIOUtils.writePascalStringL(out, volumeName, 27)
        out.putInt(volumeCreateDate)
        out.putShort(volumeSignature)
        out.putShort(volumeType)
        out.putInt(parentDirId)
        NIOUtils.writePascalStringL(out, fileName, 63)
        out.putInt(fileNumber)
        out.putInt(createdLocalDate)
        out.put(JCodecUtil2.asciiString(fileTypeName), 0, 4)
        out.put(JCodecUtil2.asciiString(creatorName), 0, 4)
        out.putShort(nlvlFrom)
        out.putShort(nlvlTo)
        out.putInt(volumeAttributes)
        out.putShort(fsId)
        out.put(ByteArray(10))
        for (extraField in extra!!) {
            out.putShort(extraField.type)
            out.putShort(extraField.len.toShort())
            out.put(extraField.data)
        }
        out.putShort((-1).toShort())
        out.putShort(0.toShort())
    }

    override fun estimateSize(): Int {
        var sz = 166
        if (flags and 0x1 == 0) {
            for (extraField in extra!!) {
                sz += 4 + extraField.data.size
            }
        }
        return 12 + sz
    }

    fun getRecordSize(): Int {
        return recordSize.toInt()
    }

    val extras: List<ExtraField>?
        get() = extra

    fun getExtra(type: Int): ExtraField? {
        for (extraField in extra!!) {
            if (extraField.type.toInt() == type) return extraField
        }
        return null
    }

    val isSelfRef: Boolean
        get() = flags and 0x1 != 0

    val unixPath: String?
        get() {
            val extraField = getExtra(UNIXAbsolutePath)
            return if (extraField == null) null else "/$extraField"
        }

    companion object {
        const val DirectoryName = 0
        const val DirectoryIDs = 1 // parent & higher directory ids
        // '/' is not counted, one
// unsigned32 for each dir
        const val AbsolutePath = 2
        const val AppleShareZoneName = 3
        const val AppleShareServerName = 4
        const val AppleShareUserName = 5
        const val DriverName = 6
        const val RevisedAppleShare = 9
        const val AppleRemoteAccessDialup = 10
        const val UNIXAbsolutePath = 18
        const val UTF16AbsolutePath = 14
        const val UFT16VolumeName = 15 // 26
        const val VolumeMountPoint = 19 // 1
        @JvmStatic
        fun fourcc(): String {
            return "alis"
        }

        @JvmStatic
        fun createSelfRef(): AliasBox {
            val alis = AliasBox(Header(fourcc()))
            alis.flags = 1
            return alis
        }
    }
}