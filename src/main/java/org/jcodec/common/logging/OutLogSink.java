package org.jcodec.common.logging;

import static org.jcodec.common.tools.MainUtils.colorString;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.logging.Logger.Level;
import org.jcodec.common.logging.Logger.Message;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.ANSIColor;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Outputs messages to standard output
 * 
 * @author The JCodec project
 */
public class OutLogSink implements Logger.LogSink {

    public static OutLogSink createOutLogSink() {
        return new OutLogSink(System.out, DEFAULT_FORMAT);
    }

    private PrintStream out;
    private MessageFormat fmt;

    public OutLogSink(PrintStream out, MessageFormat fmt) {
        this.out = out;
        this.fmt = fmt;
    }

    @Override
    public void postMessage(Message msg) {
        out.println(fmt.formatMessage(msg));
    }

    public static interface MessageFormat {
        String formatMessage(Message msg);
    }

    public static SimpleFormat DEFAULT_FORMAT = new SimpleFormat(colorString("[#level]", "#color_code")
            + MainUtils.bold("\t#class.#method (#file:#line):") + "\t#message");

    public static class SimpleFormat implements MessageFormat {
        private String fmt;
        private static Map<Level, ANSIColor> colorMap = new HashMap<Logger.Level, MainUtils.ANSIColor>();
        static {
            colorMap.put(Level.DEBUG, ANSIColor.BROWN);
            colorMap.put(Level.INFO, ANSIColor.GREEN);
            colorMap.put(Level.WARN, ANSIColor.MAGENTA);
            colorMap.put(Level.ERROR, ANSIColor.RED);
        };

        public SimpleFormat(String fmt) {
            this.fmt = fmt;
        }

        @Override
        public String formatMessage(Message msg) {
            return fmt.replace("#level", String.valueOf(msg.getLevel()))
                    .replace("#color_code", String.valueOf(30 + colorMap.get(msg.getLevel()).ordinal()))
                    .replace("#class", msg.getClassName()).replace("#method", msg.getMethodName())
                    .replace("#file", msg.getFileName()).replace("#line", String.valueOf(msg.getLineNumber()))
                    .replace("#message", msg.getMessage());
        }
    };
}