package org.jcodec.common.logging;

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

    public static enum Level {
        DEBUG, INFO, WARN, ERROR
    };

    public static class Message {
        private Level level;
        private String fileName;
        private String className;
        private int lineNumber;
        private String message;
        private String methodName;

        public Message(Level level, String fileName, String className, String methodName, int lineNumber, String message) {
            this.level = level;
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.message = methodName;
            this.lineNumber = lineNumber;
            this.message = message;
        }

        public Level getLevel() {
            return level;
        }

        public String getFileName() {
            return fileName;
        }

        public String getClassName() {
            return className;
        }
        
        public String getMethodName() {
            return methodName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getMessage() {
            return message;
        }
    }

    public static interface LogSink {
        void postMessage(Message msg);
    }

    private static List<LogSink> stageSinks = new LinkedList<LogSink>();
    private static List<LogSink> sinks;

    public static void debug(String message) {
        message(Level.DEBUG, message);
    }

    public static void info(String message) {
        message(Level.INFO, message);
    }

    public static void warn(String message) {
        message(Level.WARN, message);
    }

    public static void error(String message) {
        message(Level.ERROR, message);
    }

    private static void message(Level level, String message) {
        if (sinks == null) {
            synchronized (Logger.class) {
                if (sinks == null) {
                    sinks = stageSinks;
                    stageSinks = null;
                    if (sinks.isEmpty())
                        sinks.add(new OutLogSink());
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