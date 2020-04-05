package org.jcodec.movtool

import org.jcodec.common.io.IOUtils
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import org.jcodec.containers.mp4.MP4Util.getRootAtoms
import org.jcodec.containers.mp4.MP4Util.parseMovie
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.NodeBox
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MovDump {
    @Throws(Exception::class)
    fun main1(args: Array<String>) {
        if (args.size < 1) {
            println("Syntax: movdump [options] <filename>")
            println("Options: \n\t-f <filename> save header to a file\n\t-a <atom name> dump only a specific atom\n")
            return
        }
        var idx = 0
        var headerFile: File? = null
        var atom: String? = null
        while (idx < args.size) {
            if ("-f" == args[idx]) {
                ++idx
                headerFile = File(args[idx++])
            } else if ("-a" == args[idx]) {
                ++idx
                atom = args[idx++]
            } else break
        }
        val source = File(args[idx])
        headerFile?.let { dumpHeader(it, source) }
        if (atom == null) println(print(source)) else {
            val dump = printAtom(source, atom)
            dump?.let { println(it) }
        }
    }

    @Throws(IOException::class, FileNotFoundException::class)
    private fun dumpHeader(headerFile: File, source: File) {
        var raf: SeekableByteChannel? = null
        var daos: SeekableByteChannel? = null
        try {
            raf = NIOUtils.readableChannel(source)
            daos = NIOUtils.writableChannel(headerFile)
            for (atom in getRootAtoms(raf)) {
                val fourcc = atom.header.fourcc
                if ("moov" == fourcc || "ftyp" == fourcc) {
                    atom.copy(raf, daos)
                }
            }
        } finally {
            IOUtils.closeQuietly(raf)
            IOUtils.closeQuietly(daos)
        }
    }

    @Throws(IOException::class)
    fun print(file: File?): String {
        return parseMovie(file).toString()
    }

    private fun findDeep(root: NodeBox?, atom: String): Box? {
        for (b in root!!.getBoxes()) {
            if (atom.equals(b.fourcc, ignoreCase = true)) {
                return b
            } else if (b is NodeBox) {
                val res = findDeep(b, atom)
                if (res != null) return res
            }
        }
        return null
    }

    @Throws(IOException::class)
    fun printAtom(file: File?, atom: String): String? {
        val mov = parseMovie(file)
        val found = findDeep(mov, atom)
        if (found == null) {
            println("Atom $atom not found.")
            return null
        }
        return found.toString()
    }
}