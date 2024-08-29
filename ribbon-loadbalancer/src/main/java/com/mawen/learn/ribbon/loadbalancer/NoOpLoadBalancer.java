package com.mawen.learn.ribbon.loadbalancer;

import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
@Slf4j
public class NoOpLoadBalancer extends AbstractLoadBalancer {

	@Override
	public void addServers(List<Server> servers) {
		log.info("addServers to NoOpLoadBalancer ignored");
	}

	@Override
	public Server chooseServer(Object key) {
		return null;
	}

	@Override
	public LoadBalancerStats getLoadBalancerStats() {
		return null;
	}

	@Override
	public List<Server> getServerList(ServerGroup serverGroup) {
		return Collections.emptyList();
	}

	@Override
	public void markServerDown(Server server) {
			log.info("markServerDown to NoOpLoadBalancer ignored");
	}

	@Override
	public List<Server> getServerList(boolean availableOnly) {
		return null;
	}
}
