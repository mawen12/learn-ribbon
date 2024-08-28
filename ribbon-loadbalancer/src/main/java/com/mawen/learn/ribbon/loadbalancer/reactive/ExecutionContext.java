package com.mawen.learn.ribbon.loadbalancer.reactive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mawen.learn.ribbon.client.RetryHandler;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.config.IClientConfigKey;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
public class ExecutionContext<T> {

	private final Map<String, Object> context;
	private final ConcurrentHashMap<Object, ChildContext<T>> subContexts;
	private final T request;
	private final IClientConfig requestConfig;
	private final IClientConfig clientConfig;
	private final RetryHandler retryHandler;

	private static class ChildContext<T> extends ExecutionContext<T> {

		private final ExecutionContext<T> parent;

		ChildContext(ExecutionContext<T> parent) {
			super(parent.request, parent.requestConfig, parent.clientConfig, parent.retryHandler, null);
			this.parent = parent;
		}

		@Override
		public ExecutionContext<T> getGlobalContext() {
			return parent;
		}
	}

	public ExecutionContext(T request, IClientConfig requestConfig, IClientConfig clientConfig, RetryHandler retryHandler) {
		this.request = request;
		this.requestConfig = requestConfig;
		this.clientConfig = clientConfig;
		this.retryHandler = retryHandler;
		this.context = new ConcurrentHashMap<>();
		this.subContexts = new ConcurrentHashMap<>();
	}

	public ExecutionContext(T request, IClientConfig requestConfig, IClientConfig clientConfig, RetryHandler retryHandler, ConcurrentHashMap<Object, ChildContext<T>> subContexts) {
		this.subContexts = subContexts;
		this.request = request;
		this.requestConfig = requestConfig;
		this.clientConfig = clientConfig;
		this.retryHandler = retryHandler;
		this.context = new ConcurrentHashMap<>();
	}

	ExecutionContext<T> getChildContext(Object object) {
		if (subContexts == null) {
			return null;
		}

		ChildContext<T> subContext = subContexts.get(object);
		if (subContext == null) {
			subContext = new ChildContext<T>(this);
			ChildContext<T> old = subContexts.putIfAbsent(object, subContext);
			if (old != null) {
				subContext = old;
			}
		}
		return subContext;
	}

	public T getRequest() {
		return request;
	}

	public Object get(String name) {
		return context.get(name);
	}

	public <S> S getClientProperty(IClientConfigKey<S> key) {
		S value;
		if (requestConfig != null) {
			value = requestConfig.get(key);
			if (value != null) {
				return value;
			}
		}
		value = clientConfig.get(key);
		return value;
	}

	public void put(String name, Object value) {
		context.put(name, value);
	}

	public IClientConfig getRequestConfig() {
		return requestConfig;
	}

	public ExecutionContext<T> getGlobalContext() {
		return this;
	}

	public RetryHandler getRetryHandler() {
		return retryHandler;
	}
 }
