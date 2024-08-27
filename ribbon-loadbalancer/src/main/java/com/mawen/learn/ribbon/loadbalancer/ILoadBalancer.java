package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public interface ILoadBalancer {

	void addServers(List<Server> servers);

	Server chooseServer(Object key);

	void markServerDown(Server server);

	List<Server> getServerList(boolean availableOnly);
}
