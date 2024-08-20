package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.niws.client.NiwsClientConfigAware;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public abstract class AbstractNIWSLoadBalancerPing implements IPing, NiwsClientConfigAware {

	protected AbstractLoadBalancer lb;

	@Override
	public boolean isAlive(Server server) {
		return true;
	}

	public AbstractLoadBalancer getLb() {
		return lb;
	}

	public void setLb(AbstractLoadBalancer lb) {
		this.lb = lb;
	}
}
