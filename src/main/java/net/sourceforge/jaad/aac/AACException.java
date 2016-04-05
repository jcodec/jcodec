package net.sourceforge.jaad.aac;
import js.io.IOException;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * Standard exception, thrown when decoding of an AAC frame fails. The message
 * gives more detailed information about the error.
 * 
 * @author in-somnia
 */
public class AACException extends IOException {

    public static AACException endOfStream() {
        AACException ex = new AACException("end of stream");
        ex.eos = true;
        return ex;
    }

    private boolean eos;

    public AACException(String message) {
        super(message);
    }

    public boolean isEndOfStream() {
        return eos;
    }

    public static AACException wrap(Exception e) {
        if (e != null && e instanceof AACException) {
            return (AACException) e;
        }
        if (e != null && e.getMessage() != null) {
            return new AACException(e.getMessage());
        }
        return new AACException("" + e);
    }
}
