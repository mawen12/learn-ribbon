package com.mawen.learn.ribbon.transport.netty;

import java.util.concurrent.ScheduledExecutorService;

import com.mawen.learn.ribbon.client.RetryHandler;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.config.IClientConfigKey;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.LoadBalancerBuilder;
import io.reactivex.netty.client.CompositePoolLimitDeterminationStrategy;
import io.reactivex.netty.client.MaxConnectionsBasedStrategy;
import io.reactivex.netty.client.PoolLimitDeterminationStrategy;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/30
 */
public abstract class LoadBalancingRxClientWithPoolOptions<I, O, T extends RxClient<I, O>> extends LoadBalancingRxClient<I, O, T> {

	protected CompositePoolLimitDeterminationStrategy poolStrategy;
	protected MaxConnectionsBasedStrategy globalStrategy;
	protected int idleConnectionEvictionMills;
	protected ScheduledExecutorService poolCleanerScheduler;
	protected boolean poolEnabled = true;

	public LoadBalancingRxClientWithPoolOptions(IClientConfig config, RetryHandler retryHandler, PipelineConfigurator<O, I> pipelineConfigurator,
			ScheduledExecutorService poolCleanerScheduler) {
		this(
				LoadBalancerBuilder.newBuilder().withClientConfig(config).buildDynamicServerListLoadBalancer(),
				config,
				retryHandler,
				pipelineConfigurator,
				poolCleanerScheduler);
	}

	public LoadBalancingRxClientWithPoolOptions(ILoadBalancer lb, IClientConfig config, RetryHandler retryHandler, PipelineConfigurator<O, I> pipelineConfigurator,
			ScheduledExecutorService poolCleanerScheduler) {
		super(lb, config, retryHandler, pipelineConfigurator);

		this.poolEnabled = config.get(CommonClientConfigKey.EnableConnectionPool, DefaultClientConfigImpl.DEFAULT_ENABLE_CONNECTION_POOL);
		if (poolEnabled) {
			this.poolCleanerScheduler = poolCleanerScheduler;

			int maxTotalConnections = config.get(IClientConfigKey.Keys.MaxTotalConnections, DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_CONNECTIONS);
			int maxConnections = config.get(IClientConfigKey.Keys.MaxConnectionsPerHost, DefaultClientConfigImpl.DEFAULT_MAX_CONNECTIONS_PER_HOST);
			MaxConnectionsBasedStrategy perHostStrategy = new DynamicPropertyBasedPoolStrategy(maxConnections,
					config.getClientName() + "." + config.getNameSpace() + "." + CommonClientConfigKey.MaxConnectionsPerHost);
			globalStrategy = new DynamicPropertyBasedPoolStrategy(maxTotalConnections,
					config.getClientName() + "." + config.getNameSpace() + "." + CommonClientConfigKey.MaxTotalConnections);
			poolStrategy = new CompositePoolLimitDeterminationStrategy(perHostStrategy, globalStrategy);
			idleConnectionEvictionMills = config.get(IClientConfigKey.Keys.ConnIdleEvictTimeMilliSeconds, DefaultClientConfigImpl.DEFAULT_CONNECTIONIDLE_TIME_IN_MSECS);
		}
	}

	protected final PoolLimitDeterminationStrategy getPoolStrategy() {
		return globalStrategy;
	}

	protected int getIdleConnectionEvictionMills() {
		return idleConnectionEvictionMills;
	}

	protected boolean isPoolEnabled() {
		return poolEnabled;
	}

	@Override
	public int getMaxConcurrentRequests() {
		if (poolEnabled) {
			return globalStrategy.getMaxConnections();
		}
		return super.getMaxConcurrentRequests();
	}
}
