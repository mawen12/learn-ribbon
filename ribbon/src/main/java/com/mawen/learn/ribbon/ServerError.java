package com.mawen.learn.ribbon;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class ServerError extends Exception {

	public ServerError(String message) {
		super(message);
	}

	public ServerError(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerError(Throwable cause) {
		super(cause);
	}
}
