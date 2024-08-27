package com.mawen.learn.ribbon.client.http;

import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
@Getter
public class UnexpectedHttpResponseException extends Exception{

	private static final long serialVersionUID = 1L;

	private final int statusCode;
	private final String line;

	public UnexpectedHttpResponseException(int statusCode, String line) {
		super(line);
		this.statusCode = statusCode;
		this.line = line;
	}
}
