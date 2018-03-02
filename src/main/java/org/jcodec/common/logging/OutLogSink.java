package org.jcodec.common.logging;

import static org.jcodec.common.tools.MainUtils.colorString;

import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.ANSIColor;

import java.io.PrintStream;
import java.lang.System;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Outputs messages to standard output
 * 
 * @author The JCodec project
 */
public class OutLogSink implements LogSink {

    private static String empty = "                                                                                                                                                                                                                                                ";

    public static class SimpleFormat implements MessageFormat {
        private String fmt;
        private static Map<LogLevel, ANSIColor> colorMap = new HashMap<LogLevel, MainUtils.ANSIColor>();
        static {
            colorMap.put(LogLevel.DEBUG, ANSIColor.BROWN);
            colorMap.put(LogLevel.INFO, ANSIColor.GREEN);
            colorMap.put(LogLevel.WARN, ANSIColor.MAGENTA);
            colorMap.put(LogLevel.ERROR, ANSIColor.RED);
        };

        public SimpleFormat(String fmt) {
            this.fmt = fmt;
        }

        @Override
        public String formatMessage(Message msg) {
            String str = fmt.replace("#level", String.valueOf(msg.getLevel()))
                    .replace("#color_code", String.valueOf(30 + colorMap.get(msg.getLevel()).ordinal()))
                    .replace("#class", msg.getClassName()).replace("#method", msg.getMethodName())
                    .replace("#file", msg.getFileName()).replace("#line", String.valueOf(msg.getLineNumber()))
                    .replace("#message", msg.getMessage());
            return str;
        }
    };

    public static SimpleFormat DEFAULT_FORMAT = new SimpleFormat(
            colorString("[#level]", "#color_code") + MainUtils.bold("\t#class.#method (#file:#line):") + "\t#message");

    public static OutLogSink createOutLogSink() {
        return new OutLogSink(System.out, DEFAULT_FORMAT, LogLevel.INFO);
    }

    private PrintStream out;
    private MessageFormat fmt;
    private LogLevel minLevel;

    public OutLogSink(PrintStream out, MessageFormat fmt, LogLevel minLevel) {
        this.out = out;
        this.fmt = fmt;
        this.minLevel = minLevel;
    }

    @Override
    public void postMessage(Message msg) {
        if (msg.getLevel().ordinal() < minLevel.ordinal())
            return;
        String str = fmt.formatMessage(msg);
        out.println(str);
    }

    public static interface MessageFormat {
        String formatMessage(Message msg);
    }
}