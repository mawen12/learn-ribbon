package com.mawen.learn.ribbon.loadbalancer;

import com.google.common.base.Optional;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public abstract class PredicateBasedRule extends ClientConfigEnabledRoundRobinRule {

	public abstract AbstractServerPredicate getPredicate();

	@Override
	public Server choose(Object key) {
		ILoadBalancer lb = getLoadBalancer();
		Optional<Server> server = getPredicate().chooseRoundRobinAfterFiltering(lb.getServerList(false), key);
		if (server.isPresent()) {
			return server.get();
		}
		else {
			return null;
		}
	}
}
