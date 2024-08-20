package com.mawen.learn.ribbon.loadbalancer;

import java.util.Random;

/**
 * Randomly choose from all living servers
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class RandomRule implements IRule {

	private Random rand;

	public RandomRule() {
		this.rand = new Random();
	}

	@Override
	public Server choose(BaseLoadBalancer lb, Object key) {
		if (lb == null) {
			return null;
		}

		Server server = null;

		while (server == null) {
			if (Thread.interrupted()) {
				return null;
			}

			int serverCount = lb.getServerCount(true);
			if (serverCount == 0) {
				// No servers. End regardless of pass, because subsequent passes only get more restrictive.
				return null;
			}

			int index = rand.nextInt(serverCount);
			server = lb.getServerByIndex(index, true);

			if (server == null) {
				// The only time this should happen is if the server list were somehow trimmed.
				// This is a transient condition. Retry after yielding.
				Thread.yield();
				continue;
			}

			if (server.isAlive()) {
				return server;
			}

			// Shouldn't actually happen.. but must be transient or a bug.
			server = null;
			Thread.yield();
		}

		return server;
	}
}
