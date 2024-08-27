package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractLoadBalancer implements ILoadBalancer{

	public enum ServerGroup {
		ALL,
		STATUS_UP,
		STATUS_NOT_UP
	}

	public Server chooseServer() {
		return chooseServer(null);
	}

	public abstract List<Server> getServerList(ServerGroup serverGroup);

	public abstract LoadBalancerStats getLoadBalancerStats();
}
