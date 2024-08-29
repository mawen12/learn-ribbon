package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.config.IClientConfigKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class ResponseTimeWeightedRule extends RoundRobinRule {

	public static final IClientConfigKey<Integer> WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY = WeightedResponseTimeRule.WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY;

	public static final int DEFAULT_TIMER_INTERVAL = 30 * 1000;

	private int serverWeightTaskTimerInterval = DEFAULT_TIMER_INTERVAL;

	private volatile List<Double> accumulatedWeights = new ArrayList<>();

	private final Random random = new Random();

	protected Timer serverWeightTimer;

	protected AtomicBoolean serverWeightAssignmentInProgress = new AtomicBoolean(false);

	String name = "unknown";

	public ResponseTimeWeightedRule() {
		super();
	}

	public ResponseTimeWeightedRule(ILoadBalancer lb) {
		super(lb);
	}

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		super.setLoadBalancer(lb);
		if (lb instanceof BaseLoadBalancer) {
			name = ((BaseLoadBalancer) lb).getName();
		}
		initialize(lb);
	}

	void initialize(ILoadBalancer lb) {
		if (serverWeightTimer != null) {
			serverWeightTimer.cancel();
		}

		serverWeightTimer = new Timer("NFLoadBalancer-serverWeightTimer-"
				+ name, true);
		serverWeightTimer.schedule(new DynamicServerWeightTask(), 0,
				serverWeightTaskTimerInterval);

		ServerWeight sw = new ServerWeight();
		sw.maintainWeights();

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				log.info("Stopping NFLoadBalancer-serverWeightTimer-" + name);
				serverWeightTimer.cancel();
			}
		}));
	}

	public void shutdown() {
		if (serverWeightTimer != null) {
			log.info("Stopping NFLoadBalancer-serverWeightTimer-" + name);
			serverWeightTimer.cancel();
		}
	}

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
	@Override
	public Server choose(ILoadBalancer lb, Object key) {
		if (lb == null) {
			return null;
		}
		Server server = null;

		while (server == null) {
			// get hold of the current reference in case it is changed from the other thread
			List<Double> currentWeights = accumulatedWeights;
			if (Thread.interrupted()) {
				return null;
			}
			List<Server> allList = lb.getServerList(false);

			int serverCount = allList.size();

			if (serverCount == 0) {
				return null;
			}

			int serverIndex = 0;

			// last one in the list is the sum of all weights
			double maxTotalWeight = currentWeights.size() == 0 ? 0 : currentWeights.get(currentWeights.size() - 1);
			// No server has been hit yet and total weight is not initialized
			// fallback to use round robin
			if (maxTotalWeight < 0.001d) {
				server =  super.choose(getLoadBalancer(), key);
			} else {
				// generate a random weight between 0 (inclusive) to maxTotalWeight (exclusive)
				double randomWeight = random.nextDouble() * maxTotalWeight;
				// pick the server index based on the randomIndex
				int n = 0;
				for (Double d : currentWeights) {
					if (d >= randomWeight) {
						serverIndex = n;
						break;
					} else {
						n++;
					}
				}

				server = allList.get(serverIndex);
			}

			if (server == null) {
				/* Transient. */
				Thread.yield();
				continue;
			}

			if (server.isAlive()) {
				return (server);
			}

			// Next.
			server = null;
		}
		return server;
	}

	class DynamicServerWeightTask extends TimerTask {
		public void run() {
			ServerWeight serverWeight = new ServerWeight();
			try {
				serverWeight.maintainWeights();
			}
			catch (Throwable t) {
				log.error(
						"Throwable caught while running DynamicServerWeightTask for "
								+ name, t);
			}
		}
	}

	class ServerWeight {

		public void maintainWeights() {
			ILoadBalancer lb = getLoadBalancer();
			if (lb == null) {
				return;
			}
			if (serverWeightAssignmentInProgress.get()) {
				return; // Ping in progress - nothing to do
			}
			else {
				serverWeightAssignmentInProgress.set(true);
			}
			try {
				log.info("Weight adjusting job started");
				AbstractLoadBalancer nlb = (AbstractLoadBalancer) lb;
				LoadBalancerStats stats = nlb.getLoadBalancerStats();
				if (stats == null) {
					// no statistics, nothing to do
					return;
				}
				double totalResponseTime = 0;
				// find maximal 95% response time
				for (Server server : nlb.getServerList(false)) {
					// this will automatically load the stats if not in cache
					ServerStats ss = stats.getSingleServerStat(server);
					totalResponseTime += ss.getResponseTimeAvg();
				}
				// weight for each server is (sum of responseTime of all servers - responseTime)
				// so that the longer the response time, the less the weight and the less likely to be chosen
				Double weightSoFar = 0.0;

				// create new list and hot swap the reference
				List<Double> finalWeights = new ArrayList<Double>();
				for (Server server : nlb.getServerList(false)) {
					ServerStats ss = stats.getSingleServerStat(server);
					double weight = totalResponseTime - ss.getResponseTimeAvg();
					weightSoFar += weight;
					finalWeights.add(weightSoFar);
				}
				setWeights(finalWeights);
			}
			catch (Throwable t) {
				log.error("Exception while dynamically calculating server weights", t);
			}
			finally {
				serverWeightAssignmentInProgress.set(false);
			}

		}
	}

	void setWeights(List<Double> weights) {
		this.accumulatedWeights = weights;
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		super.initWithNIWSConfig(clientConfig);
		serverWeightTaskTimerInterval = clientConfig.get(WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY, DEFAULT_TIMER_INTERVAL);
	}
}
