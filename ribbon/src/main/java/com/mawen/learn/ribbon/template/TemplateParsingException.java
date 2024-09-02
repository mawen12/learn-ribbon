package com.mawen.learn.ribbon.template;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class TemplateParsingException extends Exception {

	private static final long serialVersionUID = -2062196150144556509L;

	public TemplateParsingException(String message) {
		super(message);
	}

	public TemplateParsingException(String message, Throwable cause) {
		super(message, cause);
	}
}
