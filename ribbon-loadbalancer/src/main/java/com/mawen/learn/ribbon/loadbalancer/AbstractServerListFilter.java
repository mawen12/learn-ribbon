package com.mawen.learn.ribbon.loadbalancer;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractServerListFilter<T extends Server> implements ServerListFilter<T> {

	private volatile LoadBalancerStats stats;

	public LoadBalancerStats getLoadBalancerStats() {
		return stats;
	}

	public void setLoadBalancerStats(LoadBalancerStats stats) {
		this.stats = stats;
	}
}
