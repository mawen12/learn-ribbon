package com.mawen.learn.ribbon;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public abstract class RequestTemplate<T, R> {

	public abstract RequestBuilder<T> requestBuilder();

	public abstract String name();

	public abstract RequestTemplate<T, R> copy(String name);

	public static abstract class RequestBuilder<T> {

		public abstract RequestBuilder<T> withRequestProperty(String key, Object value);

		public abstract RibbonRequest<T> build();
	}
}
