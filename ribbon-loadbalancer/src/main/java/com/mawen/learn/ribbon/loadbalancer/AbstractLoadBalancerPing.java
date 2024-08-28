package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.client.IClientConfigAware;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractLoadBalancerPing implements IPing, IClientConfigAware {

	AbstractLoadBalancer lb;

	@Override
	public boolean isAlive(Server server) {
		return true;
	}

	public void setLoadBalancer(AbstractLoadBalancer lb) {
		this.lb = lb;
	}

	public AbstractLoadBalancer getLoadBalancer() {
		return lb;
	}
}
