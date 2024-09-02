package com.mawen.learn.ribbon.transport.netty.http;

import com.mawen.learn.ribbon.loadbalancer.Server;
import com.mawen.learn.ribbon.transport.netty.LoadBalancingRxClientWithPoolOptions;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/30
 */
public class LoadBalancingHttpClient<I, O> extends LoadBalancingRxClientWithPoolOptions<HttpClientRequest<I>, HttpClientResponse<O>, HttpClient<I, O>> {

	private static final HttpClient.HttpClientConfig DEFAULT_RX_CONFIG = HttpClient.HttpClientConfig.Builder.newDefaultConfig();

	@Override
	protected HttpClient<I, O> createRxClient(Server server) {
		return null;
	}

	@Override
	protected MetricEventsListener<? extends ClientMetricsEvent<?>> createListener(String name) {
		return null;
	}
}
