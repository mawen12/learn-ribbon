package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

import com.google.common.collect.Collections2;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class AvailabilityFilteringRule extends PredicateBasedRule{

	private AbstractServerPredicate predicate;

	public AvailabilityFilteringRule() {
		super();

		predicate = CompositePredicate.withPredicate(new AvailabilityPredicate(this, null))
				.addFallbackPredicate(AbstractServerPredicate.alwaysTrue())
				.build();
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		predicate = CompositePredicate.withPredicate(new AvailabilityPredicate(this, clientConfig))
				.addFallbackPredicate(AbstractServerPredicate.alwaysTrue())
				.build();
	}

	@Monitor(name = "AvailableServersCount", type = DataSourceType.GAUGE)
	public int getAvailableServersCount() {
		ILoadBalancer lb = getLoadBalancer();
		List<Server> servers = lb.getServerList(false);
		if (servers == null) {
			return 0;
		}
		return Collections2.filter(servers, predicate.getServerOnlyPredicate()).size();
	}

	@Override
	public Server choose(Object key) {
		int count = 0;
		Server server = roundRobinRule.choose(key);
		while (count++ <= 10) {
			if (predicate.apply(new PredicateKey(server))) {
				return server;
			}
			server = roundRobinRule.choose(key);
		}
		return super.choose(key);
	}

	@Override
	public AbstractServerPredicate getPredicate() {
		return predicate;
	}
}
