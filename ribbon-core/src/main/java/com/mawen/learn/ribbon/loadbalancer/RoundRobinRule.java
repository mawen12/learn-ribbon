package com.mawen.learn.ribbon.loadbalancer;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class RoundRobinRule implements IRule{

	private static final Logger log = LoggerFactory.getLogger(RoundRobinRule.class);

	private static final boolean availableOnly = false;

	private AtomicInteger nextIndexAI;

	public RoundRobinRule() {
		this.nextIndexAI = new AtomicInteger(0);
	}

	@Override
	public Server choose(BaseLoadBalancer lb, Object key) {

		if (lb == null) {
			log.warn("no load balancer");
			return null;
		}

		Server server = null;
		int index = 0;
		int count = 0;

		while (server == null && count++ < 10) {
			int upCount = lb.getServerCount(true);
			int serverCount = lb.getServerCount(availableOnly);

			if (upCount == 0 || serverCount == 0) {
				log.warn("No up servers available from load balancer: {}", lb);
				return null;
			}

			index = nextIndexAI.incrementAndGet() % serverCount;
			server = lb.getServerByIndex(index, availableOnly);

			if (server == null) {
				// Transient
				Thread.yield();
				continue;
			}

			if (server.isAlive() && (!lb.isEnablePrimingConnections() || server.isReadToServe())) {
				return server;
			}

			// Next
			server = null;
		}

		if (count > 10) {
			log.warn("No available alive servers after 10 tries from load balancer: {}", lb);
		}

		return server;
	}
}
