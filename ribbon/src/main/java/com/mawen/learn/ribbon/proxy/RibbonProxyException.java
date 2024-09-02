package com.mawen.learn.ribbon.proxy;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class RibbonProxyException extends RuntimeException {

	private static final long serialVersionUID = -1L;

	public RibbonProxyException(String message) {
		super(message);
	}

	public RibbonProxyException(String message, Throwable cause) {
		super(message, cause);
	}
}
