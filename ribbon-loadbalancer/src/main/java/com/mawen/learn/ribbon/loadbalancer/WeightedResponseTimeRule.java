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
 * @since 2024/8/29
 */
@Slf4j
public class WeightedResponseTimeRule extends RoundRobinRule{

	public static final IClientConfigKey<Integer> WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY = new IClientConfigKey<Integer>() {
		@Override
		public String key() {
			return "ServerWeightTaskTimerInterval";
		}

		@Override
		public String toString() {
			return key();
		}

		@Override
		public Class<Integer> type() {
			return Integer.class;
		}
	};

	public static final int DEFAULT_TIMER_INTERVAL = 30 * 1000;

	private int serverWeightTaskTimerInterval = DEFAULT_TIMER_INTERVAL;

	private volatile List<Double> accumulatedWeights = new ArrayList<>();

	private final Random random = new Random();

	protected Timer serverWeightTimer;

	protected AtomicBoolean serverWeightAssignmentInProgress = new AtomicBoolean(false);

	String name = "unknown";

	public WeightedResponseTimeRule() {
		super();
	}

	public WeightedResponseTimeRule(ILoadBalancer lb) {
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

		serverWeightTimer = new Timer("NFLoadBalancer-serverWeightTimer-" + name, true);
		serverWeightTimer.schedule(new DynamicServerWeightTask(), 0, serverWeightTaskTimerInterval);

		ServerWeight sw = new ServerWeight();
		sw.maintainWeights();

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
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

	@Override
	public Server choose(ILoadBalancer lb, Object key) {
		if (lb == null) {
			return null;
		}

		Server server = null;
		while (server == null) {
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
			double maxTotalWeight = currentWeights.size() == 0 ? 0 : currentWeights.get(currentWeights.size() - 1);
			if (maxTotalWeight < 0.001d) {
				server = super.choose(getLoadBalancer(), key);
			}
			else {
				double randomWeight = random.nextDouble() * maxTotalWeight;
				int n = 0;
				for (Double d : currentWeights) {
					if (d >= randomWeight) {
						serverIndex = n;
						break;
					}
					else {
						n++;
					}
				}
				server = allList.get(serverIndex);
			}

			if (server == null) {
				Thread.yield();
				continue;
			}

			if (server.isAlive()) {
				return server;
			}

			server = null;
		}
		return server;
	}

	class DynamicServerWeightTask extends TimerTask {
		@Override
		public void run() {
			ServerWeight serverWeight = new ServerWeight();
			try {
				serverWeight.maintainWeights();
			}
			catch (Throwable e) {
				log.error("Throwable caught while running DynamicServerWeightTask for " + name, e);
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
				return;
			}
			else {
				serverWeightAssignmentInProgress.set(true);
			}

			try {
				log.info("Weight adjusting job started");
				AbstractLoadBalancer nlb = (AbstractLoadBalancer) lb;
				LoadBalancerStats stats = nlb.getLoadBalancerStats();
				if (stats == null) {
					return;
				}

				double totalResponseTime = 0;
				for (Server server : nlb.getServerList(false)) {
					ServerStats ss = stats.getSingleServerStat(server);
					totalResponseTime += ss.getResponseTimeAvg();
				}

				Double weightSoFar = 0.0;
				List<Double> finalWeights = new ArrayList<>();
				for (Server server : nlb.getServerList(false)) {
					ServerStats ss = stats.getSingleServerStat(server);
					double weight = totalResponseTime - ss.getResponseTimeAvg();
					weightSoFar += weight;
					finalWeights.add(weightSoFar);
				}
				setWeights(finalWeights);
			}
			catch (Throwable e) {
				log.error("Exception while dynamically calculating server weights", e);
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
