package com.mawen.learn.ribbon.client;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public interface RetryHandler {

	RetryHandler DEFAULT = new DefaultLoadBalancerRetryHandler();

	boolean isRetriableException(Throwable e, boolean sameSever);

	boolean isCircuitTrippingException(Throwable e);

	int getMaxRetriesOnSameServer();

	int getMaxRetriesOnNextServer();
}
