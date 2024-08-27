package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class RoundRobinRule extends AbstractLoadBalancerRule{

	static final boolean availableOnly = false;

	AtomicInteger nextIndexAI;

	public RoundRobinRule() {
		this.nextIndexAI = new AtomicInteger(0);
	}

	public RoundRobinRule(ILoadBalancer lb) {
		this();
		setLoadBalancer(lb);
	}



	@Override
	public Server choose(Object key) {
		return choose(getLoadBalancer(), key);
	}

	public Server choose(ILoadBalancer lb, Object key) {
		if (lb == null) {
			log.warn("no laod balancer");
			return null;
		}

		Server server = null;
		int index = 0;
		int count = 0;

		while (server == null && count++ < 10) {
			List<Server> upList = lb.getServerList(true);
			List<Server> allList = lb.getServerList(false);
			int upCount = upList.size();
			int allCount = allList.size();

			if (upCount == 0 && allCount == 0) {
				log.warn("No up servers available from load balancer: {}", lb);
				return null;
			}

			index = nextIndexAI.incrementAndGet() % allCount;
			server = allList.get(index);

			if (server == null) {
				Thread.yield();
				continue;
			}

			if (server.isAlive() && server.isReadyToServe()) {
				return server;
			}

			server = null;
		}

		if (count >= 10) {
			log.warn("No available alive servers after 10 tries from load balancer: {}", lb);
		}

		return server;
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		// do nothing
	}
}
