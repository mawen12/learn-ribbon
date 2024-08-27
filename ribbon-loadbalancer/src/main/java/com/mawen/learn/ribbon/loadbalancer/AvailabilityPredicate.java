package com.mawen.learn.ribbon.loadbalancer;

import javax.annotation.Nullable;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.ChainedDynamicProperty;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public class AvailabilityPredicate extends AbstractServerPredicate{

	private static final DynamicBooleanProperty CIRCUIT_BREAKER_FILTERING =
			DynamicPropertyFactory.getInstance().getBooleanProperty("niws.loadbalancer.availabilityFilteringRule.filterCircuitTripped", true);
	private static final DynamicIntProperty ACTIVE_CONNECTIONS_LIMIT =
			DynamicPropertyFactory.getInstance().getIntProperty("niws.loadbalancer.availabilityFilteringRule.activeConnectionsLimit", Integer.MAX_VALUE);

	private ChainedDynamicProperty.IntProperty activeConnectionsLimit = new ChainedDynamicProperty.IntProperty(ACTIVE_CONNECTIONS_LIMIT);

	public AvailabilityPredicate(IRule rule, IClientConfig clientConfig) {
		super(rule, clientConfig);
		initDynamicProperty(clientConfig);
	}

	public AvailabilityPredicate(LoadBalancerStats lbStats, IClientConfig clientConfig) {
		super(lbStats, clientConfig);
		initDynamicProperty(clientConfig);
	}

	AvailabilityPredicate(IRule rule) {
		super(rule);
	}

	private void initDynamicProperty(IClientConfig clientConfig) {
		String id = "default";
		if (clientConfig != null) {
			id = clientConfig.getClientName();
			activeConnectionsLimit = new ChainedDynamicProperty.IntProperty(id + "." + clientConfig.getNameSpace() + ".ActiveConnectionsLimit", ACTIVE_CONNECTIONS_LIMIT);
		}
	}

	@Override
	public boolean apply(@Nullable PredicateKey input) {
		LoadBalancerStats stats = getLBStats();
		if (stats == null) {
			return true;
		}
		return !shouldSkipServer(stats.getSingleServerStat(input.getServer()));
	}

	private boolean shouldSkipServer(ServerStats stats) {
		return (CIRCUIT_BREAKER_FILTERING.get() && stats.isCircuitBreakerTripped())
				|| stats.getActiveRequestsCount() >= activeConnectionsLimit.get();
	}
}
