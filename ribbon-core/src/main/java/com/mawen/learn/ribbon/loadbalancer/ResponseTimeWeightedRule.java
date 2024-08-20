package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class ResponseTimeWeightedRule implements IRule {

	private static final Logger log = LoggerFactory.getLogger(ResponseTimeWeightedRule.class);

	private static final int serverWeightTaskTimerInterval = 30 * 1000;

	private static final boolean availableOnly = false;

	private ILoadBalancer lb;

	private Map<Server, Double> serverWeights = new ConcurrentHashMap<>();

	private List<Double> finalWeights = new ArrayList<>();

	private double maxTotalWeight = 0.0;

	private final Random random = new Random(System.currentTimeMillis());

	private Timer serverWeightTimer;

	private AtomicBoolean serverWeightAssignmentInProgress = new AtomicBoolean(false);

	private String name = "unknown";


	public ILoadBalancer getLoadBalancer() {
		return lb;
	}

	public void setLoadBalancer(ILoadBalancer lb) {
		this.lb = lb;
		if (lb instanceof BaseLoadBalancer) {
			this.name = ((BaseLoadBalancer)lb).getName();
		}
	}

	public void initialize(ILoadBalancer lb) {
		setLoadBalancer(lb);
		if (serverWeightTimer != null) {
			serverWeightTimer.cancel();
		}

		serverWeightTimer = new Timer("NFLoadBalancer-serverWeightTimer-" + name, true);
		serverWeightTimer.schedule(new DynamicServerWeightTask(), 0, serverWeightTaskTimerInterval);
		// do a initial run
		ServerWeight sw = new ServerWeight();
		sw.maintainWeights();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Stopping NFLoadBalancer-serverWeightTimer-" + name);
			serverWeightTimer.cancel();
		}));
	}

	public void shutdown() {
		if (serverWeightTimer != null) {
			log.info("Stopping NFLoadBalancer-serverWeightTimer-" + name);
			serverWeightTimer.cancel();
		}
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

			int upCount = lb.getServerCount(true);
			int serverCount = lb.getServerCount(availableOnly);

			if (upCount == 0 || serverCount == 0) {
				return null;
			}

			double randomIndex = 0;

			while (randomIndex == 0) {
				randomIndex = random.nextDouble() * maxTotalWeight;
				if (randomIndex != 0) {
					break;
				}
			}

			int serverIndex = 0;

			// pick the server index based on the randomIndex
			int n = 0;
			for (Double d : finalWeights) {
				if (randomIndex <= d) {
					serverIndex = n;
				}
				else {
					n++;
				}
			}

			server = lb.getServerByIndex(serverIndex, availableOnly);

			if (server == null) {
				Thread.yield();
				continue;
			}

			if (server.isAlive()) {
				return server;
			}

			// Next
			server = null;
		}

		return server;
	}

	class DynamicServerWeightTask extends TimerTask {

		@Override
		public void run() {

		}
	}

	class ServerWeight {

		public void maintainWeights() {
			if (lb == null) {
				return;
			}

			if (serverWeightAssignmentInProgress.get()) {
				return;
			}
			else {
				serverWeightAssignmentInProgress.set(true);
			}

			try {
				BaseLoadBalancer nlb = (BaseLoadBalancer) lb;
				for (Server server : nlb.getServerList(availableOnly)) {
					Double weight = 10.00;
					if (nlb.getLoadBalancerStats() != null) {
						if (nlb.getLoadBalancerStats().getServerStats().get(server) != null) {
							ServerStats ss = nlb.getLoadBalancerStats().getServerStats().get(server);
							weight = ss.getResponseTime95thPercentile();
						}
						else {
							nlb.getLoadBalancerStats().addServer(server);
						}
					}
					serverWeights.put(server, weight);
				}
				// calculate final weights
				Double weightSoFar = 0.0;
				finalWeights.clear();
				for (Server server : nlb.getServerList(availableOnly)) {
					weightSoFar += serverWeights.get(server);
					finalWeights.add(weightSoFar);
				}
				maxTotalWeight = weightSoFar;
			}
			catch (Throwable e) {
				log.error("Exception while dynamically calculating server weights", e);
			}
			finally {
				serverWeightAssignmentInProgress.set(false);
			}
		}

	}
}
