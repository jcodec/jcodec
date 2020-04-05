package org.jcodec.movtool

import org.jcodec.common.io.IOUtils
import org.jcodec.common.tools.MainUtils
import org.jcodec.common.tools.MainUtils.FlagType
import org.jcodec.containers.mp4.boxes.MetaValue
import org.jcodec.containers.mp4.boxes.MetaValue.Companion.createFloat
import org.jcodec.containers.mp4.boxes.MetaValue.Companion.createInt
import org.jcodec.containers.mp4.boxes.MetaValue.Companion.createOther
import org.jcodec.containers.mp4.boxes.MetaValue.Companion.createString
import org.jcodec.platform.Platform
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MetadataEditorMain {
    private const val TYPENAME_FLOAT = "float"
    private const val TYPENAME_INT2 = "integer"
    private const val TYPENAME_INT = "int"
    private val FLAG_SET_KEYED = MainUtils.Flag.flag("set-keyed", "sk",
            "key1[,type1]=value1:key2[,type2]=value2[,...] Sets the metadata piece into a file.")
    private val FLAG_SET_ITUNES = MainUtils.Flag.flag("set-itunes", "si",
            "key1[,type1]=value1:key2[,type2]=value2[,...] Sets the metadata piece into a file.")
    private val FLAG_SET_ITUNES_BLOB = MainUtils.Flag.flag("set-itunes-blob", "sib",
            "key[,type]=file Sets the data read from a file into the metadata field 'key'. If file is not present stdin is read.")
    private val FLAG_QUERY = MainUtils.Flag.flag("query", "q", "Query the value of one key from the metadata set.")
    private val FLAG_FAST = MainUtils.Flag("fast", "f",
            "Fast edit, will move the " + "header to the end of the file when ther's no room to fit it.",
            FlagType.VOID)
    private val FLAG_DROP_KEYED = MainUtils.Flag.flag("drop-keyed", "dk", "Drop the field(s) from keyed metadata,"
            + " format: key1,key2,key3,...")
    private val FLAG_DROP_ITUNES = MainUtils.Flag.flag("drop-itunes", "di",
            "Drop the field(s) from iTunes metadata," + " format: key1,key2,key3,...")
    private val flags = arrayOf(FLAG_SET_KEYED, FLAG_SET_ITUNES, FLAG_QUERY, FLAG_FAST, FLAG_SET_ITUNES_BLOB,
            FLAG_DROP_KEYED, FLAG_DROP_ITUNES)
    private val strToType: MutableMap<String, Int> = HashMap()

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val cmd = MainUtils.parseArguments(args, flags)
        if (cmd.argsLength() < 1) {
            MainUtils.printHelpCmdVa("metaedit", flags, "file name")
            System.exit(-1)
            return
        }
        var mediaMeta: MetadataEditor = MetadataEditor.Companion.createFrom(File(cmd.getArg(0)))
        var save = false
        val flagSetKeyed = cmd.getStringFlag(FLAG_SET_KEYED)
        if (flagSetKeyed != null) {
            val map = parseMetaSpec(flagSetKeyed)
            save = save or (map.size > 0)
            mediaMeta.keyedMeta!!.putAll(map)
        }
        val flagDropKeyed = cmd.getStringFlag(FLAG_DROP_KEYED)
        if (flagDropKeyed != null) {
            val keys = flagDropKeyed.split(",".toRegex()).toTypedArray()
            val keyedMeta = mediaMeta.keyedMeta
            for (key in keys) {
                save = save or (keyedMeta!!.remove(key) != null)
            }
        }
        val flagDropItunes = cmd.getStringFlag(FLAG_DROP_ITUNES)
        if (flagDropItunes != null) {
            val keys = flagDropItunes.split(",".toRegex()).toTypedArray()
            val itunesMeta = mediaMeta.itunesMeta
            for (key in keys) {
                val fourcc = stringToFourcc(key)
                save = save or (itunesMeta!!.remove(fourcc) != null)
            }
        }
        val flagSetItunes = cmd.getStringFlag(FLAG_SET_ITUNES)
        if (flagSetItunes != null) {
            val map = toFourccMeta(parseMetaSpec(flagSetItunes))
            save = save or (map.size > 0)
            mediaMeta.itunesMeta!!.putAll(map)
        }
        val flagSetItunesBlob = cmd.getStringFlag(FLAG_SET_ITUNES_BLOB)
        if (flagSetItunesBlob != null) {
            val lr = flagSetItunesBlob.split("=".toRegex()).toTypedArray()
            val kt = lr[0].split(",".toRegex()).toTypedArray()
            val key = kt[0]
            var type: Int? = 1
            if (kt.size > 1) {
                type = strToType[kt[1]]
            }
            if (type != null) {
                val data = readStdin(if (lr.size > 1) lr[1] else null)
                mediaMeta.itunesMeta!![stringToFourcc(key)] = createOther(type, data)
                save = true
            } else {
                System.err.println("Unsupported metadata type: " + kt[1])
            }
        }
        if (save) {
            mediaMeta.save(cmd.getBooleanFlag(FLAG_FAST))
            mediaMeta = MetadataEditor.Companion.createFrom(File(cmd.getArg(0)))
        }
        val keyedMeta = mediaMeta.keyedMeta
        if (keyedMeta != null) {
            val flagQuery = cmd.getStringFlag(FLAG_QUERY)
            if (flagQuery == null) {
                println("Keyed metadata:")
                for ((key, value) in keyedMeta) {
                    println("$key: $value")
                }
            } else {
                printValue(keyedMeta[flagQuery])
            }
        }
        val itunesMeta = mediaMeta.itunesMeta
        if (itunesMeta != null) {
            val flagQuery = cmd.getStringFlag(FLAG_QUERY)
            if (flagQuery == null) {
                println("iTunes metadata:")
                for ((key, value) in itunesMeta) {
                    println(fourccToString(key!!) + ": " + value)
                }
            } else {
                printValue(itunesMeta[stringToFourcc(flagQuery)])
            }
        }
    }

    @Throws(IOException::class)
    private fun readStdin(fileName: String?): ByteArray {
        var fis: InputStream? = null
        return try {
            if (fileName != null) {
                fis = FileInputStream(File(fileName))
                IOUtils.toByteArray(fis)
            } else {
                IOUtils.toByteArray(Platform.stdin())
            }
        } finally {
            IOUtils.closeQuietly(fis)
        }
    }

    @Throws(IOException::class)
    private fun printValue(value: MetaValue?) {
        if (value == null) return
        if (value.isBlob) System.out.write(value.data) else println(value)
    }

    private fun toFourccMeta(keyed: Map<String, MetaValue>): Map<Int, MetaValue> {
        val ret = HashMap<Int, MetaValue>()
        for ((key, value) in keyed) {
            ret[stringToFourcc(key)] = value
        }
        return ret
    }

    private fun parseMetaSpec(flagSetKeyed: String): Map<String, MetaValue> {
        val map: MutableMap<String, MetaValue> = HashMap()
        for (value in flagSetKeyed.split(":".toRegex()).toTypedArray()) {
            val lr = value.split("=".toRegex()).toTypedArray()
            val kt = lr[0].split(",".toRegex()).toTypedArray()
            map[kt[0]] = typedValue(if (lr.size > 1) lr[1] else null, if (kt.size > 1) kt[1] else null)
        }
        return map
    }

    private fun fourccToString(key: Int): String {
        val bytes = ByteArray(4)
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putInt(key)
        return Platform.stringFromCharset(bytes, Platform.ISO8859_1)
    }

    private fun stringToFourcc(fourcc: String?): Int {
        if (fourcc!!.length != 4) return 0
        val bytes = Platform.getBytesForCharset(fourcc, Platform.ISO8859_1)
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).int
    }

    private fun typedValue(value: String?, type: String?): MetaValue {
        if (TYPENAME_INT.equals(type, ignoreCase = true) || TYPENAME_INT2.equals(type, ignoreCase = true)) return createInt(value!!.toInt())
        return if (TYPENAME_FLOAT.equals(type, ignoreCase = true)) createFloat(value!!.toFloat()) else createString(value)
    }

    init {
        strToType["utf8"] = 1
        strToType["utf16"] = 2
        strToType[TYPENAME_FLOAT] = 23
        strToType[TYPENAME_INT] = 21
        strToType[TYPENAME_INT2] = 21
        strToType["jpeg"] = 13
        strToType["jpg"] = 13
        strToType["png"] = 14
        strToType["bmp"] = 27
    }
}