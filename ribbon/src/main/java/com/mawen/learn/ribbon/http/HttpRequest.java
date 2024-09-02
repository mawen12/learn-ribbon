package com.mawen.learn.ribbon.http;

import java.util.Map;

import com.mawen.learn.ribbon.CacheProvider;
import com.mawen.learn.ribbon.RibbonRequest;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.functions.Func1;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
class HttpRequest<T> implements RibbonRequest<T> {

	private final HttpClientRequest<ByteBuf> httpRequest;
	private final String hystrixCacheKey;
	private final String cacheHystrixCacheKey;
	private final CacheProviderWithKey<T> cacheProvider;
	private final Map<String, Object> requestProperties;
	private final HttpClient<ByteBuf, ByteBuf> client;
	final HttpRequestTemplate<T> template;

	HttpRequest(HttpRequestBuilder<T> requestBuilder) {

	}


	static class CacheProviderWithKey<T> {
		CacheProvider<T> cacheProvider;
		String key;

		public CacheProviderWithKey(CacheProvider<T> cacheProvider, String key) {
			this.cacheProvider = cacheProvider;
			this.key = key;
		}

		public final CacheProvider<T> getCacheProvider() {
			return cacheProvider;
		}

		public final String getKey() {
			return key;
		}
	}

	private static final Func1<ByteBuf, ByteBuf> refCountIncrement = new Func1<ByteBuf, ByteBuf>() {
		@Override
		public ByteBuf call(ByteBuf byteBuf) {
			byteBuf.retain();
			return byteBuf;
		}
	};

}
