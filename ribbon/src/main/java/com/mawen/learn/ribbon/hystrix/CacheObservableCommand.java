package com.mawen.learn.ribbon.hystrix;

import java.util.Map;

import com.mawen.learn.ribbon.CacheProvider;
import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class CacheObservableCommand<T> extends HystrixObservableCommand<T> {

	private final CacheProvider<T> cacheProvider;
	private final String key;
	private final String hystrixCacheKey;
	private final Map<String, Object> requestProperties;

	public CacheObservableCommand(CacheProvider<T> cacheProvider, String key, String hystrixCacheKey, Map<String, Object> requestProperties, Setter setter) {
		super(setter);
		this.cacheProvider = cacheProvider;
		this.key = key;
		this.hystrixCacheKey = hystrixCacheKey;
		this.requestProperties = requestProperties;
	}

	@Override
	protected String getCacheKey() {
		if (hystrixCacheKey == null) {
			return super.getCacheKey();
		}
		else {
			return hystrixCacheKey;
		}
	}

	@Override
	protected Observable<T> construct() {
		return cacheProvider.get(key, requestProperties);
	}
}
