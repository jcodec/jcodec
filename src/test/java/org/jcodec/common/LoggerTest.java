package org.jcodec.common;

import org.jcodec.common.logging.Logger;

public class LoggerTest {
    public static void main(String[] args) {
        Logger.error("This is a error");
        Logger.warn("This is a warn");
        Logger.info("This is an info");
        Logger.debug("This is a debug");
    }
}
