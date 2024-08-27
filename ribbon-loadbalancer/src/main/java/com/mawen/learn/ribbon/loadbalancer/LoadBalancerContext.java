package com.mawen.learn.ribbon.loadbalancer;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import com.mawen.learn.ribbon.client.ClientException;
import com.mawen.learn.ribbon.client.ClientRequest;
import com.mawen.learn.ribbon.client.DefaultLoadBalancerRetryHandler;
import com.mawen.learn.ribbon.client.IClientConfigAware;
import com.mawen.learn.ribbon.client.RetryHandler;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ConnectTimeoutException;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class LoadBalancerContext implements IClientConfigAware {

	protected String clientName = "default";

	protected String vipAddresses;

	protected int maxAutoRetriesNextServer = DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER;

	protected int maxAutoRetries = DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES;

	protected RetryHandler defaultRetryHandler = new DefaultLoadBalancerRetryHandler();

	protected boolean okToRetryOnAllOperations = DefaultClientConfigImpl.DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS.booleanValue();

	private ILoadBalancer lb;

	private volatile Timer tracer;

	public LoadBalancerContext(ILoadBalancer lb) {
		this.lb = lb;
	}

	public LoadBalancerContext(ILoadBalancer lb, IClientConfig clientConfig) {
		this.lb = lb;
		initWithNIWSConfig(clientConfig);
	}

	public LoadBalancerContext(ILoadBalancer lb, IClientConfig clientConfig, RetryHandler retryHandler) {
		this.lb = lb;
		this.defaultRetryHandler = retryHandler;
		initWithNIWSConfig(clientConfig);
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		if (clientConfig == null) {
			return;
		}

		clientName = clientConfig.getClientName();
		if (clientName == null) {
			clientName = "default";
		}

		vipAddresses = clientConfig.resolveDeploymentContextbasedVipAddresses();
		maxAutoRetries = clientConfig.getPropertyAsInteger(CommonClientConfigKey.MaxAutoRetries, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES);
		maxAutoRetriesNextServer = clientConfig.getPropertyAsInteger(CommonClientConfigKey.MaxAutoRetriesNextServer, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER);
		okToRetryOnAllOperations = clientConfig.getPropertyAsBoolean(CommonClientConfigKey.OkToRetryOnAllOperations, okToRetryOnAllOperations);
		defaultRetryHandler = new DefaultLoadBalancerRetryHandler(clientConfig);

		tracer = getExecuteTracer();

		Monitors.registerObject("Client_" + clientName, this);
	}

	public Timer getExecuteTracer() {
		if (tracer == null) {
			synchronized (this) {
				if (tracer == null) {
					tracer = Monitors.newTimer(clientName + "_LoadBalancerExecutionTimer", TimeUnit.MILLISECONDS);
				}
			}
		}

		return tracer;
	}

	public String getClientName() {
		return clientName;
	}

	public ILoadBalancer getLoadBalancer() {
		return lb;
	}

	public void setLoadBalancer(ILoadBalancer lb) {
		this.lb = lb;
	}

	public int getMaxAutoRetriesNextServer() {
		return maxAutoRetriesNextServer;
	}

	public void setMaxAutoRetriesNextServer(int maxAutoRetriesNextServer) {
		this.maxAutoRetriesNextServer = maxAutoRetriesNextServer;
	}

	public int getMaxAutoRetries() {
		return maxAutoRetries;
	}

	public void setMaxAutoRetries(int maxAutoRetries) {
		this.maxAutoRetries = maxAutoRetries;
	}

	protected Throwable getDeepestCause(Throwable e) {
		if (e != null) {
			int infiniteLoopPreventionCounter = 10;
			while (e.getCause() != null && infiniteLoopPreventionCounter > 0) {
				infiniteLoopPreventionCounter--;
				e = e.getCause();
			}
		}
		return e;
	}

	private boolean isPresentAsCause(Throwable throwableToSearchIn, Class<? extends Throwable> throwableToSearchFor) {
		return isPresentAsCauseHelper(throwableToSearchIn, throwableToSearchFor) != null;
	}

	static Throwable isPresentAsCauseHelper(Throwable throwableToSearchIn, Class<? extends Throwable> throwableToSearchFor) {
		int infiniteLoopPreventionCounter = 10;
		while (throwableToSearchIn != null && infiniteLoopPreventionCounter > 0) {
			infiniteLoopPreventionCounter--;
			if (throwableToSearchIn.getClass().isAssignableFrom(throwableToSearchFor)) {
				return throwableToSearchIn;
			}
			else {
				throwableToSearchIn = throwableToSearchIn.getCause();
			}
		}
		return null;
	}

	protected ClientException generateNIWSException(String uri, Throwable e) {
		ClientException niwsClientException;
		if (isPresentAsCause(e, SocketTimeoutException.class)) {
			niwsClientException = generateTimeoutNIWSException(uri, e);
		}
		else if (e.getCause() instanceof ConnectException) {
			niwsClientException = new ClientException(ClientException.ErrorType.CONNECT_EXCEPTION,
					"Unable to execute RestClient request for URI: " + uri, e);
		}
		else if (e.getCause() instanceof NoRouteToHostException) {
			niwsClientException = new ClientException(ClientException.ErrorType.NO_ROUTE_TO_HOST_EXCEPTION,
					"Unable to execute RestClient request for URI: " + uri, e);
		}
		else if (e instanceof ClientException) {
			niwsClientException = (ClientException) e;
		}
		else {
			niwsClientException = new ClientException(ClientException.ErrorType.GENERAL,
					"Unable to execute RestClient request for URI: " + uri, e);
		}
		return niwsClientException;
	}

	private boolean isPresentAsCause(Throwable throwableToSearchIn, Class<? extends Throwable> throwableToSearchFor, String messageSubstringToSearchFor) {
		Throwable throwableFound = isPresentAsCauseHelper(throwableToSearchIn, throwableToSearchFor);
		if (throwableFound != null) {
			return throwableFound.getMessage().contains(messageSubstringToSearchFor);
		}
		return false;
	}

	private ClientException generateTimeoutNIWSException(String uri, Throwable e) {
		ClientException niwsClientException;
		if (isPresentAsCause(e, SocketTimeoutException.class)) {
			niwsClientException = new ClientException(ClientException.ErrorType.READ_TIMEOUT_EXCEPTION,
					"Unable to execute RestClient request for URI: " + uri + ":" + getDeepestCause(e).getMessage(), e);
		}
		else {
			niwsClientException = new ClientException(ClientException.ErrorType.SOCKET_TIMEOUT_EXCEPTION,
					"Unable to execute RestClient request for URI: " + uri + ":" + getDeepestCause(e).getMessage(), e);
		}
		return niwsClientException;
	}

	private void recordStats(ServerStats stats, long responseTime) {
		stats.decrementActiveRequestsCount();
		stats.incrementNumRequests();
		stats.noteResponseTime(responseTime);
	}

	protected void noteRequestCompletion(ServerStats stats, Object response, Throwable e, long responseTime) {
		noteRequestCompletion(stats, response, e, responseTime, null);
	}

	public void noteRequestCompletion(ServerStats stats, Object response, Throwable e, long responseTime, RetryHandler errorHandler) {
		try {
			recordStats(stats, responseTime);
			RetryHandler callErrorHandler = errorHandler == null ? getRetryHandler() : errorHandler;
			if (callErrorHandler != null && response != null) {
				stats.clearSuccessiveConnectionFailureCount();
			}
			else if (callErrorHandler != null && e != null) {
				if (callErrorHandler.isCircuitTrippingException(e)) {
					stats.incrementSuccessiveConnectionFailureCount();
					stats.addToFailureCount();
				}
				else {
					stats.clearSuccessiveConnectionFailureCount();
				}
			}
		}
		catch (Throwable ex) {
			log.error("Unexpected exception", ex);
		}
	}

	protected void noteError(ServerStats stats, ClientRequest request, Throwable e, long responseTime) {
		try {
			recordStats(stats, responseTime);
			RetryHandler errorHandler = getRetryHandler();
			if (errorHandler != null && e != null) {
				stats.incrementSuccessiveConnectionFailureCount();
				stats.addToFailureCount();
			}
			else {
				stats.clearSuccessiveConnectionFailureCount();
			}
		}
		catch (Throwable ex) {
			log.error("Unexpected exception", ex);
		}
	}

	protected void noteResponse(ServerStats stats, )

}
