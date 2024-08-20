package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/19
 */
public interface ILoadBalancer {

	void addServers(List<Server> newServers);

	Server chooseServer(Object key);

	void markServerDown(Server server);
}
