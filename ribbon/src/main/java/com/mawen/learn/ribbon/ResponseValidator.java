package com.mawen.learn.ribbon;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public interface ResponseValidator<T> {

	void validate(T response) throws UnsuccessfulResponseException, ServerError;
}
