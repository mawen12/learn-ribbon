package com.mawen.learn.ribbon.loadbalancer;

import java.util.Collections;
import java.util.List;

import com.mawen.learn.ribbon.niws.client.NiwsClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class NoOpLoadBalancer extends AbstractLoadBalancer {

	private static final Logger log = LoggerFactory.getLogger(NoOpLoadBalancer.class);

	@Override
	public List<Server> getServerList(ServerGroup serverGroup) {
		return Collections.emptyList();
	}

	@Override
	public LoadBalancerStats getLoadBalancerStats() {
		return null;
	}

	@Override
	public void addServers(List<Server> newServers) {
		log.info("addServers to NoOpLoadBalancer ignored");
	}

	@Override
	public Server chooseServer(Object key) {
		return null;
	}

	@Override
	public void markServerDown(Server server) {
		log.info("markServerDown to NoOpLoadBalancer ignored");
	}

	@Override
	public void initWithNiwsConfig(NiwsClientConfig niwsClientConfig) {

	}
}
