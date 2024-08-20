package com.mawen.learn.ribbon.niws.client;

import com.mawen.learn.ribbon.loadbalancer.AbstractLoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.BaseLoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.IRule;
import com.mawen.learn.ribbon.loadbalancer.Server;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public abstract class AbstractNIWSLoadBalancerRule implements IRule, NiwsClientConfigAware {

	protected AbstractLoadBalancer lb;

	@Override
	public Server choose(BaseLoadBalancer lb, Object key) {
		return null;
	}

	public AbstractLoadBalancer getLoadBalancer() {
		return lb;
	}

	public void setLoadBalancer(AbstractLoadBalancer lb) {
		this.lb = lb;
	}
}
