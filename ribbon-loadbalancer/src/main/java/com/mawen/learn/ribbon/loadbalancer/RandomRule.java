package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;
import java.util.Random;

import com.mawen.learn.ribbon.client.config.IClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public class RandomRule extends AbstractLoadBalancerRule{

	Random rand;

	public RandomRule() {
		this.rand = new Random();
	}

	@Override
	public Server choose(Object key) {
		return choose(getLoadBalancer(), key);
	}

	public Server choose(ILoadBalancer lb, Object key) {
		if (lb == null) {
			return null;
		}

		Server server = null;

		while (server == null) {
			if (Thread.interrupted()) {
				return null;
			}

			List<Server> upList = lb.getServerList(true);
			List<Server> allList = lb.getServerList(false);

			int serverCount = allList.size();
			if (serverCount == 0) {
				return null;
			}

			int index = rand.nextInt(serverCount);
			server = upList.get(index);

			if (server == null) {
				Thread.yield();
				continue;
			}

			if (server.isAlive()) {
				return server;
			}

			server = null;
			Thread.yield();
		}

		return server;
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		// do nothing
	}
}
