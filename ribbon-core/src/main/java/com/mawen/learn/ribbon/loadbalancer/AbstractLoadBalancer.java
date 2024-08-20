package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

import com.mawen.learn.ribbon.niws.client.NiwsClientConfigAware;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public abstract class AbstractLoadBalancer implements ILoadBalancer, NiwsClientConfigAware {

	public abstract List<Server> getServerList(ServerGroup serverGroup);

	public abstract LoadBalancerStats getLoadBalancerStats();

	public enum ServerGroup {
		ALL,
		STATUS_UP,
		STATUS_NOT_UP
	}
}
