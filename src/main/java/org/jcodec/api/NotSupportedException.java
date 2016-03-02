package org.jcodec.api;

public class NotSupportedException extends RuntimeException {

//	public NotSupportedException() {
//		super();
//	}
	
	public NotSupportedException(String... arguments) {
		super(""+arguments);
	}

}
