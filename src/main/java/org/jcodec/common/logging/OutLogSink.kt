package org.jcodec.common.logging

import org.jcodec.common.tools.MainUtils
import org.jcodec.common.tools.MainUtils.ANSIColor
import java.io.PrintStream
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Outputs messages to standard output
 *
 * @author The JCodec project
 */
class OutLogSink(private val out: PrintStream, private val fmt: MessageFormat, private val minLevel: LogLevel) : LogSink {
    class SimpleFormat(private val fmt: String) : MessageFormat {

        companion object {
            private val colorMap: MutableMap<LogLevel, ANSIColor> = HashMap()

            init {
                colorMap[LogLevel.DEBUG] = ANSIColor.BROWN
                colorMap[LogLevel.INFO] = ANSIColor.GREEN
                colorMap[LogLevel.WARN] = ANSIColor.MAGENTA
                colorMap[LogLevel.ERROR] = ANSIColor.RED
            }
        }

        override fun formatMessage(msg: Message): String {
            return fmt.replace("#level", msg.level.toString())
                    .replace("#color_code", (30 + colorMap[msg.level]!!.ordinal).toString())
                    .replace("#class", msg.className).replace("#method", msg.methodName)
                    .replace("#file", msg.fileName).replace("#line", msg.lineNumber.toString())
                    .replace("#message", msg.message)
        }

    }

    override fun postMessage(msg: Message) {
        if (msg.level.ordinal < minLevel.ordinal) return
        val str = fmt.formatMessage(msg)
        out.println(str)
    }

    interface MessageFormat {
        fun formatMessage(msg: Message): String
    }

    companion object {
        private const val empty = "                                                                                                                                                                                                                                                "
        var DEFAULT_FORMAT = SimpleFormat(
                MainUtils.colorString("[#level]", "#color_code") + MainUtils.bold("\t#class.#method (#file:#line):") + "\t#message")

        @JvmStatic
        fun createOutLogSink(): OutLogSink {
            return OutLogSink(System.out, DEFAULT_FORMAT, LogLevel.INFO)
        }
    }

}