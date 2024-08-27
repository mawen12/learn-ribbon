package com.mawen.learn.ribbon.client;

import java.net.SocketException;
import java.util.List;

import com.google.common.collect.Lists;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.IClientConfig;

import static com.google.common.base.Preconditions.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public class RequestSpecificRetryHandler implements RetryHandler {

	private final RetryHandler fallback;

	private int retrySameServer = -1;

	private int retryNextServer = -1;

	private final boolean okToRetryOnConnectErrors;

	private final boolean okToRetryOnAllErrors;

	protected List<Class<? extends Throwable>> connectionRelated = Lists.newArrayList(SocketException.class);

	public RequestSpecificRetryHandler(boolean okToRetryOnConnectErrors, boolean okToRetryOnAllErrors) {
		this();
	}

	public RequestSpecificRetryHandler(boolean okToRetryOnConnectErrors, boolean okToRetryOnAllErrors, RetryHandler baseRetryHandler, IClientConfig clientConfig) {
		checkNotNull(baseRetryHandler);
		this.okToRetryOnConnectErrors = okToRetryOnConnectErrors;
		this.okToRetryOnAllErrors = okToRetryOnAllErrors;
		this.fallback = baseRetryHandler;

		if (clientConfig != null) {
			if (clientConfig.containsProperty(CommonClientConfigKey.MaxAutoRetries)) {
				retrySameServer = clientConfig.get(CommonClientConfigKey.MaxAutoRetries);
			}
			if (clientConfig.containsProperty(CommonClientConfigKey.MaxAutoRetriesNextServer)) {
				retryNextServer = clientConfig.get(CommonClientConfigKey.MaxAutoRetriesNextServer);
			}
		}
	}

	public boolean isConnectionException(Throwable e) {
		return Utils.isPresentAsCause(e, connectionRelated);
	}

	@Override
	public boolean isRetriableException(Throwable e, boolean sameSever) {
		if (okToRetryOnAllErrors) {
			return true;
		}
		else if (e instanceof ClientException) {
			ClientException ce = (ClientException) e;
			if (ce.getErrorType() == ClientException.ErrorType.SERVER_EXCEPTION) {
				return !sameSever;
			}
			else {
				return false;
			}
		}
		else {
			return okToRetryOnConnectErrors && isConnectionException(e);
		}
	}

	@Override
	public boolean isCircuitTrippingException(Throwable e) {
		return fallback.isCircuitTrippingException(e);
	}

	@Override
	public int getMaxRetriesOnSameServer() {
		if (retrySameServer >= 0) {
			return retrySameServer;
		}
		return fallback.getMaxRetriesOnSameServer();
	}

	@Override
	public int getMaxRetriesOnNextServer() {
		if (retryNextServer >= 0) {
			return retryNextServer;
		}
		return fallback.getMaxRetriesOnNextServer();
	}

}
