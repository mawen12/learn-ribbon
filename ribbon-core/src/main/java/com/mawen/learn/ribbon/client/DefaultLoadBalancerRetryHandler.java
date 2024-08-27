package com.mawen.learn.ribbon.client;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

import com.google.common.collect.Lists;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public class DefaultLoadBalancerRetryHandler implements RetryHandler {

	private List<Class<? extends Throwable>> retriable = Lists.newArrayList(ConnectException.class, SocketTimeoutException.class);

	private List<Class<? extends Throwable>> circuitRelated = Lists.newArrayList(SocketException.class, SocketTimeoutException.class);

	protected final int retrySameServer;

	protected final int retryNextServer;

	protected final boolean retryEnabled;

	public DefaultLoadBalancerRetryHandler() {
		this.retrySameServer = 0;
		this.retryNextServer = 0;
		this.retryEnabled = false;
	}

	public DefaultLoadBalancerRetryHandler(int retrySameServer, int retryNextServer, boolean retryEnabled) {
		this.retrySameServer = retrySameServer;
		this.retryNextServer = retryNextServer;
		this.retryEnabled = retryEnabled;
	}

	public DefaultLoadBalancerRetryHandler(IClientConfig clientConfig) {
		this.retrySameServer = clientConfig.get(CommonClientConfigKey.MaxAutoRetries, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES);
		this.retryNextServer = clientConfig.get(CommonClientConfigKey.MaxAutoRetriesNextServer, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER);
		this.retryEnabled = clientConfig.get(CommonClientConfigKey.OkToRetryOnAllOperations, false);
	}

	@Override
	public boolean isRetriableException(Throwable e, boolean sameSever) {
		if (retryEnabled) {
			if (sameSever) {
				return Utils.isPresentAsCause(e, getRetriableExceptions());
			}
			else {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCircuitTrippingException(Throwable e) {
		return Utils.isPresentAsCause(e, getCircuitRelatedExceptions());
	}

	@Override
	public int getMaxRetriesOnSameServer() {
		return retrySameServer;
	}

	@Override
	public int getMaxRetriesOnNextServer() {
		return retryNextServer;
	}

	protected List<Class<? extends Throwable>> getRetriableExceptions() {
		return retriable;
	}

	protected List<Class<? extends Throwable>> getCircuitRelatedExceptions() {
		return circuitRelated;
	}
}
