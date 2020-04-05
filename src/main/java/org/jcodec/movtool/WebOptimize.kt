package org.jcodec.movtool

import org.jcodec.containers.mp4.MP4Util.createRefFullMovieFromFile
import java.io.File
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object WebOptimize {
    @Throws(IOException::class)
    fun main1(args: Array<String?>) {
        if (args.size < 1) {
            println("Syntax: optimize <movie>")
            System.exit(-1)
        }
        val tgt = File(args[0])
        val src = hidFile(tgt)
        tgt.renameTo(src)
        try {
            val movie = createRefFullMovieFromFile(src)
            Flatten().flatten(movie, tgt)
        } catch (t: Throwable) {
            t.printStackTrace()
            tgt.renameTo(File(tgt.parentFile, tgt.name + ".error"))
            src.renameTo(tgt)
        }
    }

    fun hidFile(tgt: File): File {
        var src = File(tgt.parentFile, "." + tgt.name)
        if (src.exists()) {
            var i = 1
            do {
                src = File(tgt.parentFile, "." + tgt.name + "." + i++)
            } while (src.exists())
        }
        return src
    }
}