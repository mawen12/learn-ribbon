package com.mawen.learn.ribbon;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class UnsuccessfulResponseException extends Exception {

	public UnsuccessfulResponseException(String message) {
		super(message);
	}

	public UnsuccessfulResponseException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsuccessfulResponseException(Throwable cause) {
		super(cause);
	}
}
