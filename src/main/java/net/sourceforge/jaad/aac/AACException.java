package net.sourceforge.jaad.aac;

import java.io.IOException;

/**
 * Standard exception, thrown when decoding of an AAC frame fails.
 * The message gives more detailed information about the error.
 * @author in-somnia
 */
public class AACException extends IOException {

	private final boolean eos;

	public AACException(String message) {
		this(message, false);
	}

	public AACException(String message, boolean eos) {
		super(message);
		this.eos = eos;
	}

	public AACException(Throwable cause) {
		super(cause);
		eos = false;
	}

	public boolean isEndOfStream() {
		return eos;
	}
}
