package com.mawen.learn.ribbon.niws.client;

import com.mawen.learn.ribbon.loadbalancer.BaseLoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.RoundRobinRule;
import com.mawen.learn.ribbon.loadbalancer.Server;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class NIWSRoundRobinRule extends AbstractNIWSLoadBalancerRule {

	private RoundRobinRule rule = new RoundRobinRule();

	@Override
	public void initWithNiwsConfig(NiwsClientConfig niwsClientConfig) {
		this.rule = new RoundRobinRule();
	}

	@Override
	public Server choose(BaseLoadBalancer lb, Object key) {
		if (rule != null) {
			return rule.choose(lb, key);
		}
		else {
			throw new IllegalArgumentException("This call has not been initialized with the RoundRobinRule class");
		}
	}
}
