package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.client.config.IClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public class ClientConfigEnabledRoundRobinRule extends AbstractLoadBalancerRule {

	RoundRobinRule roundRobinRule = new RoundRobinRule();

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		roundRobinRule = new RoundRobinRule();
	}

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		super.setLoadBalancer(lb);
		roundRobinRule.setLoadBalancer(lb);
	}

	@Override
	public Server choose(Object key) {
		if (roundRobinRule != null) {
			return roundRobinRule.choose(key);
		}
		else {
			throw new IllegalArgumentException("This class has not been initialized with the RoundRobinRule class");
		}
	}
}
