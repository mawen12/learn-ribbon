package com.mawen.learn.ribbon.http;

import com.google.common.net.HttpHeaders;
import com.mawen.learn.ribbon.RequestTemplate;
import com.mawen.learn.ribbon.ResponseValidator;
import com.mawen.learn.ribbon.hystrix.FallbackHandler;
import com.mawen.learn.ribbon.template.ParsedTemplate;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class HttpRequestTemplate<T> extends RequestTemplate<T, HttpClientResponse<ByteBuf>> {

	public static final String CACHE_HYSTRIX_COMMAND_SUFFIX = "_cache";
	public static final int DEFAULT_CACHE_TIMEOUT = 20;

	private final HttpClient<ByteBuf, ByteBuf> client;
	private final int maxResponseTime;
	private final HystrixObservableCommand.Setter setter;
	private final HystrixObservableCommand.Setter cacheSetter;
	private final FallbackHandler<T> fallbackHandler;
	private final ParsedTemplate parsedTemplate;
	private final ResponseValidator<HttpClientResponse<ByteBuf>> validator;
	private final HttpMethod method;
	private final String name;
	private final HttpRequest.CacheProviderWithKey<T> cacheProvider;
	private final ParsedTemplate hystrixCacheKeyTemplate;
	private final Class<? extends T> classType;
	private final int concurrentRequestLimit;
	private final HttpHeaders headers;
	private final HttpResourceGroup group;

	@Override
	public RequestBuilder<T> requestBuilder() {
		return null;
	}

	@Override
	public String name() {
		return "";
	}

	@Override
	public RequestTemplate<T, HttpClientResponse<ByteBuf>> copy(String name) {
		return null;
	}
}
