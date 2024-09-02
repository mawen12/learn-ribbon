package com.mawen.learn.ribbon.transport.netty.udp;

import com.mawen.learn.ribbon.client.RetryHandler;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.Server;
import com.mawen.learn.ribbon.transport.netty.LoadBalancingRxClient;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.client.ClientMetricsEvent;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.metrics.MetricEventsListener;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.protocol.udp.client.UdpClientBuilder;
import io.reactivex.netty.servo.udp.UdpClientListener;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/30
 */
public class LoadBalancingUdpClient<I, O> extends LoadBalancingRxClient<I, O, RxClient<I, O>> implements RxClient<I, O> {

	public LoadBalancingUdpClient(IClientConfig config, RetryHandler retryHandler, PipelineConfigurator<O, I> pipelineConfigurator) {
		super(config, retryHandler, pipelineConfigurator);
	}

	public LoadBalancingUdpClient(ILoadBalancer lb, IClientConfig config, RetryHandler retryHandler, PipelineConfigurator<O, I> pipelineConfigurator) {
		super(lb, config, retryHandler, pipelineConfigurator);
	}

	@Override
	protected RxClient<I, O> createRxClient(Server server) {
		UdpClientBuilder<I, O> builder = RxNetty.newUdpClientBuilder(server.getHost(), server.getPort());
		if (pipelineConfigurator != null) {
			builder.pipelineConfigurator(pipelineConfigurator);
		}
		return builder.build();
	}

	@Override
	protected MetricEventsListener<? extends ClientMetricsEvent<?>> createListener(String name) {
		return UdpClientListener.newUdpListener(name);
	}


}
