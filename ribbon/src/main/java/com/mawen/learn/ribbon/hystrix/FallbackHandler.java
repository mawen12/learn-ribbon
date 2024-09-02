package com.mawen.learn.ribbon.hystrix;

import java.util.Map;

import com.netflix.hystrix.HystrixInvokableInfo;
import rx.Observable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public interface FallbackHandler<T> {

	Observable<T> getFallback(HystrixInvokableInfo<?> hystrixInfo, Map<String, Object> requestProperties);

}
