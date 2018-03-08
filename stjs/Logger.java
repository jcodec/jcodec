package org.jcodec.common.logging;
import js.lang.IllegalArgumentException;
import js.lang.IllegalStateException;
import js.lang.Comparable;
import js.lang.StringBuilder;
import js.lang.System;
import js.lang.Runtime;
import js.lang.Runnable;
import js.lang.Process;
import js.lang.ThreadLocal;
import js.lang.IndexOutOfBoundsException;
import js.lang.Thread;
import js.lang.NullPointerException;

import static org.jcodec.common.logging.LogLevel.DEBUG;

import js.util.Arrays;

import js.lang.IllegalStateException;
import js.lang.StackTraceElement;
import js.lang.Thread;
import js.util.LinkedList;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * JCodec has to be dependancy free, so it can run both on Java SE and Android
 * hence defining here our small logger that can be plugged into the logging
 * framework of choice on the target platform
 * 
 * @author The JCodec project
 */
public class Logger {

    private static List<LogSink> stageSinks = new LinkedList<LogSink>();
    private static List<LogSink> sinks;

    public static void debug(Object... arguments) {
        message(LogLevel.DEBUG, (String) arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
    }
    
    public static void info(Object... arguments) {
        message(LogLevel.INFO, (String) arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
    }
    
    public static void warn(Object... arguments) {
        message(LogLevel.WARN, (String) arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
    }
    
    public static void error(Object... arguments) {
        message(LogLevel.WARN, (String) arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
    }
    
    private static void message(LogLevel level, String message, Object[] args) {
        if (Logger.globalLogLevel.ordinal() >= level.ordinal()) {
            return;
        }
        if (sinks == null) {
            {
                if (sinks == null) {
                    sinks = stageSinks;
                    stageSinks = null;
                    if (sinks.isEmpty())
                        sinks.add(OutLogSink.createOutLogSink());
                }
            }
        }
        Message msg;
        if (DEBUG.equals(globalLogLevel)) {
            StackTraceElement tr = Thread.currentThread().getStackTrace()[3];
            msg = new Message(level, tr.getFileName(), tr.getClassName(), tr.getMethodName(), tr.getLineNumber(),
                    message, args);
        } else {
            msg = new Message(level, "", "", "", 0, message, args);
        }
        for (LogSink logSink : sinks) {
            logSink.postMessage(msg);
        }
    }

    private static LogLevel globalLogLevel = LogLevel.INFO;
    
    public static void setLevel(LogLevel level) {
        globalLogLevel = level;
    }
    
    public static LogLevel getLevel() {
        return globalLogLevel;
    }


    public static void addSink(LogSink sink) {
        if (stageSinks == null)
            throw new IllegalStateException("Logger already started");
        stageSinks.add(sink);
    }
}
