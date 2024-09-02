package com.mawen.learn.ribbon.transport.netty.tcp;

import java.util.concurrent.ScheduledExecutorService;

import com.mawen.learn.ribbon.client.RetryHandler;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.config.IClientConfigKey;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.Server;
import com.mawen.learn.ribbon.transport.netty.LoadBalancingRxClientWithPoolOptions;
import io.netty.channel.ChannelOption;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.ClientBuilder;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.servo.tcp.TcpClientListener;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/30
 */
public class LoadBalancingTcpClient<I, O> extends LoadBalancingRxClientWithPoolOptions<I, O, RxClient<I, O>> implements RxClient<I, O> {

	public LoadBalancingTcpClient(ILoadBalancer lb, IClientConfig config, RetryHandler retryHandler, PipelineConfigurator<O, I> pipelineConfigurator,
			ScheduledExecutorService poolCleanerScheduler) {
		super(lb, config, retryHandler, pipelineConfigurator, poolCleanerScheduler);
	}

	public LoadBalancingTcpClient(IClientConfig config, RetryHandler retryHandler, PipelineConfigurator<O, I> pipelineConfigurator, ScheduledExecutorService poolCleanerScheduler) {
		super(config, retryHandler, pipelineConfigurator, poolCleanerScheduler);
	}

	@Override
	protected RxClient<I, O> createRxClient(Server server) {
		ClientBuilder<I, O> builder = RxNetty.newTcpClientBuilder(server.getHost(), server.getPort());
		if (pipelineConfigurator != null) {
			builder.pipelineConfigurator(pipelineConfigurator);
		}

		Integer connectTimeout = getProperty(IClientConfigKey.Keys.ConnectTimeout, null, DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
		builder.channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
		if (isPoolEnabled()) {
			builder.withConnectionPoolLimitStrategy(poolStrategy)
					.withIdleConnectionsTimeoutMillis(idleConnectionEvictionMills)
					.withPoolIdleCleanupScheduler(poolCleanerScheduler);
		}
		else {
			builder.withNoConnectionPooling();
		}
		return builder.build();
	}

	@Override
	protected MetricEventsListener<? extends ClientMetricsEvent<?>> createListener(String name) {
		return TcpClientListener.newListener(name);
	}
}
