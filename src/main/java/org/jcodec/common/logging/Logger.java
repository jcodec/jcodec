package org.jcodec.common.logging;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p/>
 * JCodec has to be dependancy free, so it can run both on Java SE and Android
 * hence defining here our small logger that can be plugged into the logging
 * framework of choice on the target platform
 *
 * @author The JCodec project
 */

public class Logger {

    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("jcodec");

    public static void debug(String message) {
        logger.fine(message);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warn(String message) {
        logger.warning(message);
    }

    public static void error(String message) {
        logger.severe(message);
    }

}