package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.client.IClientConfigAware;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractLoadBalancerRule implements IRule, IClientConfigAware {

	private ILoadBalancer lb;

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		this.lb = lb;
	}

	@Override
	public ILoadBalancer getLoadBalancer() {
		return lb;
	}
}
