package org.jcodec.movtool

import org.jcodec.containers.mp4.boxes.MovieBox
import java.io.File
import java.nio.channels.FileChannel
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class QTEdit(editFactories: Array<EditFactory>) {
    protected val factories: Array<EditFactory>
    private val listeners: MutableList<Flatten.ProgressListener>

    interface EditFactory {
        val name: String
        fun parseArgs(args: List<String>?): MP4Edit?
        val help: String
    }

    abstract class BaseCommand : MP4Edit {
        fun applyRefs(movie: MovieBox, refs: Array<Array<FileChannel?>?>?) {
            apply(movie)
        }

        abstract override fun apply(movie: MovieBox)
    }

    fun addProgressListener(listener: Flatten.ProgressListener) {
        listeners.add(listener)
    }

    @Throws(Exception::class)
    fun execute(args: Array<String>) {
        val aa = LinkedList(Arrays.asList(*args))
        val commands: MutableList<MP4Edit?> = LinkedList()
        while (aa.size > 0) {
            var i: Int
            i = 0
            while (i < factories.size) {
                if (aa[0] == factories[i].name) {
                    aa.removeAt(0)
                    try {
                        commands.add(factories[i].parseArgs(aa))
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
        if (commands.size == 0) {
            System.err.println("ERROR: At least one command should be specified")
            help()
        }
        val input = File(aa.removeAt(0))
        if (!input.exists()) {
            System.err.println("ERROR: Input file '" + input.absolutePath + "' doesn't exist")
            help()
        }
        ReplaceMP4Editor().replace(input, CompoundMP4Edit(commands))
    }

    protected fun help() {
        println("Quicktime movie editor")
        println("Syntax: qtedit <command1> <options> ... <commandN> <options> <movie>")
        println("Where options:")
        for (commandFactory in factories) {
            println("\t" + commandFactory.help)
        }
        System.exit(-1)
    }

    init {
        listeners = ArrayList()
        factories = editFactories
    }
}