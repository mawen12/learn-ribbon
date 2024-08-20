package com.mawen.learn.ribbon.niws.client;

import com.mawen.learn.ribbon.loadbalancer.AbstractLoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.BaseLoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.ResponseTimeWeightedRule;
import com.mawen.learn.ribbon.loadbalancer.Server;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class NIWSWeightedResponseTimeRule extends AbstractNIWSLoadBalancerRule {

	private ResponseTimeWeightedRule rule = new ResponseTimeWeightedRule();

	@Override
	public void initWithNiwsConfig(NiwsClientConfig niwsClientConfig) {
		rule = new ResponseTimeWeightedRule();
	}

	@Override
	public void setLoadBalancer(AbstractLoadBalancer lb) {
		super.setLoadBalancer(lb);
		rule.setLoadBalancer(lb);
		rule.initialize(lb);
	}

	@Override
	public Server choose(BaseLoadBalancer lb, Object key) {
		if (rule != null) {
			return rule.choose(lb, key);
		}
		else {
			throw new IllegalArgumentException("This class has not been initialized with the RoundRobinRule class");
		}
	}
}
