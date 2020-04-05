package org.jcodec.movtool

import org.jcodec.containers.mp4.MP4Util.createRefFullMovieFromFile
import org.jcodec.containers.mp4.MP4Util.writeFullMovieToFile
import org.jcodec.movtool.QTEdit.EditFactory
import java.io.File
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class QTRefEdit(protected val factories: Array<EditFactory>) {
    @Throws(Exception::class)
    fun execute(args: Array<String>) {
        val aa = LinkedList(Arrays.asList(*args))
        val edits: MutableList<MP4Edit?> = LinkedList()
        while (aa.size > 0) {
            var i: Int
            i = 0
            while (i < factories.size) {
                if (aa[0] == factories[i].name) {
                    aa.removeAt(0)
                    try {
                        edits.add(factories[i].parseArgs(aa))
                    } catch (e: Exception) {
                        System.err.println("ERROR: " + e.message)
                        return
                    }
                    break
                }
                i++
            }
            if (i == factories.size) break
        }
        if (aa.size == 0) {
            System.err.println("ERROR: A movie file should be specified")
            help()
        }
        if (edits.size == 0) {
            System.err.println("ERROR: At least one command should be specified")
            help()
        }
        val input = File(aa.removeAt(0))
        if (aa.size == 0) {
            System.err.println("ERROR: A movie output file should be specified")
            help()
        }
        val output = File(aa.removeAt(0))
        if (!input.exists()) {
            System.err.println("ERROR: Input file '" + input.absolutePath + "' doesn't exist")
            help()
        }
        if (output.exists()) {
            System.err.println("WARNING: Output file '" + output.absolutePath + "' exist, overwritting")
        }
        val ref = createRefFullMovieFromFile(input)
        CompoundMP4Edit(edits).apply(ref!!.moov)
        writeFullMovieToFile(output, ref)
        println("INFO: Created reference file: " + output.absolutePath)
    }

    protected fun help() {
        println("Quicktime movie editor")
        println("Syntax: qtedit <command1> <options> ... <commandN> <options> <movie> <output>")
        println("Where options:")
        for (commandFactory in factories) {
            println("\t" + commandFactory.help)
        }
        System.exit(-1)
    }

}