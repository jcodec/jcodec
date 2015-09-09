package org.jcodec.api;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An exception thrown by JCodec API functions in case something goes wrong
 * 
 * @author The JCodec Project
 * 
 */
public class JCodecException extends Exception {

    public JCodecException(String message) {
        super(message);
    }

    public JCodecException(String message, Exception cause) {
        super(message, cause);
    }
}
