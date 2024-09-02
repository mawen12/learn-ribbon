package com.mawen.learn.ribbon;

import java.util.Map;

import rx.Observable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public interface CacheProvider<T> {

	Observable<T> get(String keyTemplate, Map<String, Object> requestProperties);
}
