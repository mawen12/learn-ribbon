package com.mawen.learn.ribbon.loadbalancer;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
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
import com.netflix.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

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

	protected void noteResponse(ServerStats stats, ClientRequest request, Object response, long responseTime) {
		try {
			recordStats(stats, responseTime);
			RetryHandler errorHandler = getRetryHandler();
			if (errorHandler != null && response != null) {
				stats.incrementSuccessiveConnectionFailureCount();
			}
		}
		catch (Throwable ex) {
			log.error("Unexpected exception", ex);
		}
	}

	public void noteOpenConnection(ServerStats stats) {
		if (stats == null) {
			return;
		}

		try {
			stats.incrementActiveRequestsCount();
		}
		catch (Throwable ex) {
			log.info("Unable to note Server Stats", ex);
		}
	}

	public Server getServerFromLoadBalancer(URI original, Object loadBalancerKey) throws ClientException {
		String host = null;
		int port = -1;
		if (original != null) {
			host = original.getHost();
			Pair<String, Integer> schemeAndPort = deriveSchemeAndPortFromPartialUri(original);
			port = schemeAndPort.second();
		}

		ILoadBalancer lb = getLoadBalancer();
		if (host == null) {
			if (lb != null) {
				Server srv = lb.chooseServer(loadBalancerKey);
				if (srv == null) {
					throw new ClientException(ClientException.ErrorType.GENERAL,
							"Load balancer does not have available server for client: " + clientName);
				}
				host = srv.getHost();
				if (host == null) {
					throw new ClientException(ClientException.ErrorType.GENERAL,
							"Invalid Server for: " + srv);
				}
				log.debug("{} using LB returned Server: {} for request {}", clientName, srv, original);
				return srv;
			}
			else {
				if (vipAddresses != null && vipAddresses.contains(".")) {
					throw new ClientException(ClientException.ErrorType.GENERAL,
							"Method is invoked for client " + clientName + " with partial URI of ("
									+ original + ") with no load balancer configured. Also, there are multiple vipAddresses " +
									"and hence no vip address can be chosen to complete this partial uri");
				}
				else if (vipAddresses != null) {
					try {
						Pair<String, Integer> hostAndPort = deriveHostAndPortFromVipAddress(vipAddresses);
						host = hostAndPort.first();
						port = hostAndPort.second();
					}
					catch (URISyntaxException e) {
						throw new ClientException(ClientException.ErrorType.GENERAL,
								"Method is invoked for client " + clientName + " with partial URI of ("
										+ original + ") with no load balancer configured." +
										"Also, the configured/registered vipAddress is unparseable (to determine host and port)");
					}
				}
				else {
					throw new ClientException(ClientException.ErrorType.GENERAL,
							this.clientName + " has no LoadBalancer and passed in a partial URL request (with no host:port)."
									+ " Also has no vipAddress registered");
				}
			}
		}
		else {
			boolean shouldInterpretAsVip = false;

			if (lb != null) {
				shouldInterpretAsVip = isVipRecognized(original.getAuthority());
			}
			if (shouldInterpretAsVip) {
				Server srv = lb.chooseServer(loadBalancerKey);
				if (srv != null) {
					host = srv.getHost();
					if (host == null) {
						throw new ClientException(ClientException.ErrorType.GENERAL, "Invalid Server for: " + srv);
					}
					log.debug("using LB returned Server: {} for request: {}", srv, original);
					return srv;
				}
				else {
					log.debug("{}:{} assumed to be a valid VIP address or exists in the DNS", host, port);
				}
			}
			else {
				log.debug("Using full URL passed in by caller (not using load balancer): {}", original);
			}
		}

		if (host == null) {
			throw new ClientException(ClientException.ErrorType.GENERAL, "Request contains no HOST to talk to");
		}

		return new Server(host, port);
	}

	protected Pair<String, Integer> deriveSchemeAndPortFromPartialUri(URI uri) {
		boolean isSecure = false;
		String scheme = uri.getScheme();
		if (scheme != null) {
			isSecure = scheme.equalsIgnoreCase("https");
		}

		int port = uri.getPort();
		if (port < 0 && !isSecure) {
			port = 80;
		}
		else if (port < 0 && isSecure) {
			port = 443;
		}
		if (scheme == null) {
			if (isSecure) {
				scheme = "https";
			}
			else {
				scheme = "http";
			}
		}

		return new Pair<>(scheme, port);
	}

	protected int getDefaultPortFromScheme(String scheme) {
		if (scheme == null) {
			return -1;
		}

		if (scheme.equalsIgnoreCase("http")) {
			return 80;
		}
		else if (scheme.equalsIgnoreCase("https")) {
			return 443;
		}
		else {
			return -1;
		}
	}

	protected Pair<String, Integer> deriveHostAndPortFromVipAddress(String vipAddress) throws ClientException, URISyntaxException {
		Pair<String, Integer> hostAndPort = new Pair<>(null, -1);
		URI uri = new URI(vipAddress);

		String scheme = uri.getScheme();
		if (scheme == null) {
			uri = new URI("http://" + vipAddress);
		}

		String host = uri.getHost();
		if (host == null) {
			throw new ClientException("Unable to derive host/port from vip address " + vipAddress);
		}

		int port = uri.getPort();
		if (port < 0) {
			port = getDefaultPortFromScheme(scheme);
		}
		if (port < 0) {
			throw new ClientException("Unable to derive host/port from vip address " + vipAddress);
		}

		hostAndPort.setFirst(host);
		hostAndPort.setSecond(port);
		return hostAndPort;
	}

	private boolean isVipRecognized(String vipEmbeddedInUri) {
		if (vipEmbeddedInUri == null) {
			return false;
		}
		if (vipAddresses == null) {
			return false;
		}
		String[] addresses = vipAddresses.split(",");
		for (String address : addresses) {
			if (vipEmbeddedInUri.equalsIgnoreCase(address.trim())) {
				return true;
			}
		}
		return false;
	}

	public URI reconstructURIWithServer(Server server, URI original) {
		String host = server.getHost();
		int port = server.getPort();
		if (host.equals(original.getHost()) && port == original.getPort()) {
			return original;
		}

		String scheme = original.getScheme();
		if (scheme == null) {
			scheme = deriveSchemeAndPortFromPartialUri(original).first();
		}

		try {
			StringBuilder sb = new StringBuilder();
			sb.append(scheme).append("://");
			if (!StringUtils.isBlank(original.getRawUserInfo())) {
				sb.append(original.getRawUserInfo()).append("@");
			}
			sb.append(host);
			if (port >= 0) {
				sb.append(":").append(port);
			}
			sb.append(original.getRawPath());
			if (!StringUtils.isBlank(original.getRawQuery())) {
				sb.append("?").append(original.getRawQuery());
			}
			if (!StringUtils.isBlank(original.getRawFragment())) {
				sb.append("#").append(original.getRawFragment());
			}
			URI newURI = new URI(sb.toString());
			return newURI;
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public final ServerStats getServerStats(Server server) {
		ServerStats stats = null;
		ILoadBalancer lb = this.getLoadBalancer();
		if(lb instanceof AbstractLoadBalancer) {
			LoadBalancerStats lbStats = ((AbstractLoadBalancer) lb).getLoadBalancerStats();
			stats = lbStats.getSingleServerStats(server);
		}
		return stats;
	}

	protected int getNumberRetriesOnSameServer(IClientConfig overrideClientConfig) {
		int numRetries = maxAutoRetriesNextServer;
		if (overrideClientConfig != null) {
			numRetries = overrideClientConfig.getPropertyAsInteger(CommonClientConfigKey.MaxAutoRetriesNextServer, maxAutoRetriesNextServer);
		}
		return numRetries;
	}

	public boolean handleSameServerRetry(Server server, int currentRetryCount, int maxRetries, Throwable e) {
		if (currentRetryCount > maxRetries) {
			return false;
		}

		log.debug("Exception while executing request which is deemed retry-able, retrying..., SAME Server Retry Attempt#: {}",
				currentRetryCount, server);
		return true;
	}

	public final RetryHandler getRetryHandler() {
		return defaultRetryHandler;
	}

	public final void setRetryHandler(RetryHandler retryHandler) {
		this.defaultRetryHandler = retryHandler;
	}

	public final boolean isOkToRetryOnAllOperations() {
		return okToRetryOnAllOperations;
	}

	public final void setOkToRetryOnAllOperations(boolean okToRetryOnAllOperations) {
		this.okToRetryOnAllOperations = okToRetryOnAllOperations;
	}
}
