package org.jcodec.common.logging;

import java.lang.IllegalStateException;
import java.lang.StackTraceElement;
import java.lang.Thread;
import java.util.LinkedList;
import java.util.List;

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

    public static void debug(String message) {
        message(LogLevel.DEBUG, message);
    }

    public static void info(String message) {
        message(LogLevel.INFO, message);
    }

    public static void warn(String message) {
        message(LogLevel.WARN, message);
    }

    public static void error(String message) {
        message(LogLevel.ERROR, message);
    }

    private static void message(LogLevel level, String message) {
        if (sinks == null) {
            synchronized (Logger.class) {
                if (sinks == null) {
                    sinks = stageSinks;
                    stageSinks = null;
                    if (sinks.isEmpty())
                        sinks.add(OutLogSink.createOutLogSink());
                }
            }
        }
        StackTraceElement tr = Thread.currentThread().getStackTrace()[3];
        Message msg = new Message(level, tr.getFileName(), tr.getClassName(), tr.getMethodName(), tr.getLineNumber(),
                message);
        for (LogSink logSink : sinks) {
            logSink.postMessage(msg);
        }
    }

    public static void addSink(LogSink sink) {
        if (stageSinks == null)
            throw new IllegalStateException("Logger already started");
        stageSinks.add(sink);
    }
}