package com.mawen.learn.ribbon;

import com.netflix.hystrix.HystrixInvokableInfo;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public abstract class RibbonResponse<T> {

	abstract T content();

	abstract HystrixInvokableInfo<?> getHystrixInfo();
}
