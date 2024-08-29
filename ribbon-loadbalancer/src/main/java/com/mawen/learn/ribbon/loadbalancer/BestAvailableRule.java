package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class BestAvailableRule extends ClientConfigEnabledRoundRobinRule {

	private LoadBalancerStats loadBalancerStats;

	@Override
	public Server choose(Object key) {
		if (loadBalancerStats == null) {
			return super.choose(key);
		}

		List<Server> serverList = getLoadBalancer().getServerList(false);
		int minimalConcurrentConnections = Integer.MAX_VALUE;
		long currentTime = System.currentTimeMillis();
		Server chosen = null;
		for (Server server : serverList) {
			ServerStats serverStat = loadBalancerStats.getSingleServerStat(server);
			if (!serverStat.isCircuitBreakerTripped(currentTime)) {
				int concurrentConnections = serverStat.getActiveRequestsCount(currentTime);
				if (concurrentConnections < minimalConcurrentConnections) {
					minimalConcurrentConnections = concurrentConnections;
					chosen = server;
				}
			}
		}

		if (chosen == null) {
			return super.choose(key);
		}
		else {
			return chosen;
		}
	}

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		super.setLoadBalancer(lb);
		if (lb instanceof AbstractLoadBalancer) {
			loadBalancerStats = ((AbstractLoadBalancer) lb).getLoadBalancerStats();
		}
	}
}
