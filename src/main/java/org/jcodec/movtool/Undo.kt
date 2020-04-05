package org.jcodec.movtool

import java.io.File
import java.io.IOException

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 *
 * Uses QuickTime feature to undo the recent changes
 *
 * @author The JCodec project
 */
object Undo {
    @Throws(IOException::class)
    fun main1(args: Array<String>) {
        if (args.size < 1) {
            System.err.println("Syntax: qt-undo [-l] <movie>")
            System.err.println("\t-l\t\tList all the previous versions of this movie.")
            System.exit(-1)
        }
        if ("-l" == args[0]) {
            val list = MoovVersions.listMoovVersionAtoms(File(args[1]))
            println((list!!.size - 1).toString() + " versions.")
        } else {
            MoovVersions.undo(File(args[0]))
        }
    }
}