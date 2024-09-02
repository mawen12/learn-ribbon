package com.mawen.learn.ribbon.transport.netty;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.mawen.learn.ribbon.client.RetryHandler;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.config.IClientConfigKey;
import com.mawen.learn.ribbon.client.ssl.AbstractSslContextFactory;
import com.mawen.learn.ribbon.client.ssl.ClientSslSocketFactoryException;
import com.mawen.learn.ribbon.client.ssl.URLSslContextFactory;
import com.mawen.learn.ribbon.client.util.Resources;
import com.mawen.learn.ribbon.loadbalancer.BaseLoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.LoadBalancerBuilder;
import com.mawen.learn.ribbon.loadbalancer.LoadBalancerContext;
import com.mawen.learn.ribbon.loadbalancer.Server;
import com.mawen.learn.ribbon.loadbalancer.ServerListChangeListener;
import com.mawen.learn.ribbon.loadbalancer.reactive.LoadBalancerCommand;
import com.mawen.learn.ribbon.loadbalancer.reactive.ServerOperation;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.metrics.MetricEventsSubject;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscription;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/30
 */
@Slf4j
public abstract class LoadBalancingRxClient<I, O, T extends RxClient<I, O>> implements RxClient<I, O> {

	protected final ConcurrentMap<Server, T> rxClientCache;
	protected final PipelineConfigurator<O, I> pipelineConfigurator;
	protected final IClientConfig clientConfig;
	protected final RetryHandler defaultRetryHandlder;
	protected final AbstractSslContextFactory sslContextFactory;
	protected final MetricEventsListener<? extends ClientMetricsEvent<?>> listener;
	protected final MetricEventsSubject<ClientMetricsEvent<?>> eventSubject;
	protected final LoadBalancerContext lbContext;

	public LoadBalancingRxClient(IClientConfig config, RetryHandler defaultRetryHandlder, PipelineConfigurator<O, I> pipelineConfigurator) {
		this(LoadBalancerBuilder.newBuilder().withClientConfig(config).buildLoadBalancerFromConfigWithReflection(), config, defaultRetryHandlder, pipelineConfigurator);
	}

	public LoadBalancingRxClient(ILoadBalancer lb, IClientConfig config, RetryHandler defaultRetryHandlder, PipelineConfigurator<O, I> pipelineConfigurator) {
		this.rxClientCache = new ConcurrentHashMap<>();
		this.lbContext = new LoadBalancerContext(lb, config, defaultRetryHandlder);
		this.defaultRetryHandlder = defaultRetryHandlder;
		this.pipelineConfigurator = pipelineConfigurator;
		this.clientConfig = config;
		this.listener = createListener(config.getClientName());

		this.eventSubject = new MetricEventsSubject<>();
		boolean isSecure = getProperty(IClientConfigKey.Keys.IsSecure, null, false);
		if (isSecure) {
			final URL trustStoreUrl = getResourceForOptionalProperty(CommonClientConfigKey.TrustStore);
			final URL keyStoreUrl = getResourceForOptionalProperty(CommonClientConfigKey.KeyStore);
			boolean isClientAuthRequired = clientConfig.get(IClientConfigKey.Keys.IsClientAuthRequired, false);
			if ((isClientAuthRequired && (trustStoreUrl != null && keyStoreUrl != null))
					|| (!isClientAuthRequired && (trustStoreUrl != null || keyStoreUrl != null))) {

				try {
					sslContextFactory = new URLSslContextFactory(trustStoreUrl,
							clientConfig.get(CommonClientConfigKey.TrustStorePassword),
							keyStoreUrl,
							clientConfig.get(CommonClientConfigKey.KeyStorePassword));
				}
				catch (ClientSslSocketFactoryException e) {
					throw new IllegalArgumentException("Unable to configure custom secure socket factory", e);
				}
			}
			else {
				this.sslContextFactory = null;
			}
		}
		else {
			this.sslContextFactory = null;
		}

		addLoadBalancerListener();
	}

	public IClientConfig getClientConfig() {
		return clientConfig;
	}

	public int getMaxConcurrentRequests() {
		return -1;
	}

	public int getResponseTimeOut() {
		int maxRetryNextServer = 0;
		int maxRetrySameServer = 0;
		if (defaultRetryHandlder != null) {
			maxRetryNextServer = defaultRetryHandlder.getMaxRetriesOnNextServer();
			maxRetrySameServer = defaultRetryHandlder.getMaxRetriesOnSameServer();
		}
		else {
			maxRetryNextServer = clientConfig.get(IClientConfigKey.Keys.MaxAutoRetriesNextServer, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER);
			maxRetrySameServer = clientConfig.get(IClientConfigKey.Keys.MaxAutoRetries, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES);
		}

		int readTimeout = getProperty(IClientConfigKey.Keys.ReadTimeout, null, DefaultClientConfigImpl.DEFAULT_READ_TIMEOUT);
		int connectTimeout = getProperty(IClientConfigKey.Keys.ConnectTimeout, null, DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
		return (maxRetryNextServer + 1) * (maxRetrySameServer + 1) * (readTimeout + connectTimeout);
	}

	protected <S> S getProperty(IClientConfigKey<S> key, IClientConfig requestConfig, S defaultValue) {
		if (requestConfig != null && requestConfig.get(key) != null) {
			return requestConfig.get(key);
		}
		else {
			return clientConfig.get(key, defaultValue);
		}
	}

	protected URL getResourceForOptionalProperty(final IClientConfigKey<String> configKey) {
		final String propValue = clientConfig.get(configKey);
		URL result = null;

		if (propValue != null) {
			result = Resources.getResource(propValue);
			if (result == null) {
				throw new IllegalArgumentException("No resource found for " + configKey + ": " + propValue);
			}
		}
		return result;
	}

	private void addLoadBalancerListener() {
		if (!(lbContext.getLoadBalancer() instanceof BaseLoadBalancer)) {
			return;
		}

		((BaseLoadBalancer) lbContext.getLoadBalancer()).addServerListChangeListener(new ServerListChangeListener() {
			@Override
			public void serverListChanged(List<Server> oldList, List<Server> newList) {
				Set<Server> removedServers = new HashSet<>(oldList);
				removedServers.removeAll(newList);
				for (Server server : rxClientCache.keySet()) {
					if (removedServers.contains(server)) {
						removeClient(server);
					}
				}
			}
		});
	}

	protected abstract T createRxClient(Server server);

	protected T getOrCreateRxClient(Server server) {
		T client = rxClientCache.get(server);
		if (client != null) {
			return client;
		}
		else {
			client = createRxClient(server);
			client.subscribe(listener);
			client.subscribe(eventSubject);
			T old = rxClientCache.putIfAbsent(server, client);
			if (old != null) {
				return old;
			}
			else {
				return client;
			}
		}
	}

	protected T removeClient(Server server) {
		T client = rxClientCache.remove(server);
		if (client != null) {
			client.shutdown();
		}
		return client;
	}

	@Override
	public Observable<ObservableConnection<O, I>> connect() {
		return LoadBalancerCommand.<ObservableConnection<O, I>>builder()
				.withLoadBalancerContext(lbContext)
				.build()
				.submit(new ServerOperation<ObservableConnection<O, I>>() {
					@Override
					public Observable<ObservableConnection<O, I>> call(Server server) {
						return getOrCreateRxClient(server).connect();
					}
				});
	}

	protected abstract MetricEventsListener<? extends ClientMetricsEvent<?>> createListener(String name);

	@Override
	public void shutdown() {
		for (Server server : rxClientCache.keySet()) {
			removeClient(server);
		}
	}

	@Override
	public String name() {
		return clientConfig.getClientName();
	}

	@Override
	public Subscription subscribe(MetricEventsListener<? extends ClientMetricsEvent<?>> metricEventsListener) {
		return eventSubject.subscribe(listener);
	}

	public final LoadBalancerContext getLoadBalancerContext() {
		return lbContext;
	}
}
