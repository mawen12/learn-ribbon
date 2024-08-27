package com.mawen.learn.ribbon.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.loadbalancer.Server;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class PrimeConnections {

	String primeConnectionURIPath = "/";

	private ExecutorService executorService;

	private int maxExecutorThreads = 5;

	private long executorThreadTimeout = 30000;

	private String name = "default";

	private float primeRatio = 1.0f;

	int maxRetries = 9;

	long maxTotalTimeToPrimeConnections = 30 * 1000;

	long totalTimeTaken = 0;

	private boolean aSync = true;

	Counter totalCounter;

	Counter successCounter;

	Timer initialPrimeTimer;

	private IPrimeConnection connector;

	private PrimeConnectionEndStats stats;

	private PrimeConnections() {}

	public PrimeConnections(String name, IClientConfig niwsClientConfig) {
		int maxRetriesPerServerPrimeConnection = DefaultClientConfigImpl.DEFAULT_MAX_RETRIES_PER_SERVER_PRIME_CONNECTION;
		long maxTotalTimeToPrimeConnections = DefaultClientConfigImpl.DEFAULT_MAX_TOTAL_TIME_TO_PRIME_CONNECTIONS;
		String primeConnectionsURI = DefaultClientConfigImpl.DEFAULT_PRIME_CONNECTIONS_URI;
		String className = DefaultClientConfigImpl.DEFAULT_PRIME_CONNECTIONS_CLASS;
		try {
			maxRetriesPerServerPrimeConnection = Integer.parseInt(String.valueOf(niwsClientConfig.getProperty(
					CommonClientConfigKey.MaxRetriesPerServerPrimeConnection, maxRetriesPerServerPrimeConnection)));
		}
		catch (Exception e) {
			log.warn("Invalid maxRetriesPerServerPrimeConnection");
		}

		try {
			maxTotalTimeToPrimeConnections = Long.parseLong(String.valueOf(niwsClientConfig.getProperty(
					CommonClientConfigKey.MaxTotalTimeToPrimeConnections, maxTotalTimeToPrimeConnections)));
		}
		catch (Exception e) {
			log.warn("Invalid maxTotalTimeToPrimeConnections");
		}

		primeConnectionsURI = String.valueOf(niwsClientConfig.getProperty(CommonClientConfigKey.PrimeConnectionsURI, primeConnectionsURI));
		float primeRatio = Float.parseFloat(String.valueOf(niwsClientConfig.getProperty(CommonClientConfigKey.MinPrimeConnectionsRatio)));
		className = (String) niwsClientConfig.getProperty(CommonClientConfigKey.PrimeConnectionsClassName, DefaultClientConfigImpl.DEFAULT_PRIME_CONNECTIONS_CLASS);

		try {
			connector = (IPrimeConnection) Class.forName(className).newInstance();
			connector.initWithNIWSConfig(niwsClientConfig);
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to initialize prime connections", e);
		}

		setUp(name, maxRetriesPerServerPrimeConnection, maxTotalTimeToPrimeConnections, primeConnectionsURI, primeRatio);
	}

	public PrimeConnections(String name, int maxRetries, long maxTotalTimeToPrimeConnections, String primeConnectionsURI) {
		setUp(name, maxRetries, maxTotalTimeToPrimeConnections, primeConnectionsURI, DefaultClientConfigImpl.DEFAULT_MIN_PRIME_CONNECTIONS_RATIO);
	}

	public PrimeConnections(String name, int maxRetries, long maxTotalTimeToPrimeConnections, String primeConnectionsURI, float primeRatio) {
		setUp(name, maxRetries, maxTotalTimeToPrimeConnections, primeConnectionsURI, primeRatio);
	}

	private void setUp(String name, int maxRetries, Long maxTotalTimeToPrimeConnections, String primeConnectionsURI, float primeRatio) {
		this.name = name;
		this.maxRetries = maxRetries;
		this.maxTotalTimeToPrimeConnections = maxTotalTimeToPrimeConnections;
		this.primeConnectionURIPath = primeConnectionsURI;
		this.primeRatio = primeRatio;
		executorService = new ThreadPoolExecutor(1,
				maxExecutorThreads,
				executorThreadTimeout,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingDeque<>(),
				new ASyncPrimeConnectionsThreadFactory(name));
		totalCounter = Monitors.newCounter(name + "_PrimeConnection_TotalCounter");
		successCounter = Monitors.newCounter(name + "_PrimeConnection_SuccessCounter");
		initialPrimeTimer = Monitors.newTimer(name + "_initialPrimeConnectionsTimer", TimeUnit.MILLISECONDS);
		Monitors.registerObject(name + "_PrimeConnection", this);
	}

	public void primeConnections(List<Server> servers) {
		if (servers == null || servers.size() == 0) {
			log.debug("No server to prime");
			return;
		}

		for (Server server : servers) {
			server.setReadyToServe(false);
		}

		int totalCount = (int) (servers.size() * primeRatio);
		final CountDownLatch latch = new CountDownLatch(totalCount);
		final AtomicInteger successCount = new AtomicInteger(0);
		final AtomicInteger failureCount = new AtomicInteger(0);
		primeConnectionsAsync(servers, new PrimeConnectionListener(){
			@Override
			public void primeCompleted(Server s, Throwable lastException) {
				if (lastException == null) {
					successCount.incrementAndGet();
					s.setReadyToServe(true);
				}
				else {
					failureCount.incrementAndGet();
				}
				latch.countDown();
			}
		});

		Stopwatch stopwatch = initialPrimeTimer.start();
		try {
			latch.await(maxTotalTimeToPrimeConnections, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			log.error("Priming connection interrupted", e);
		}
		finally {
			stopwatch.stop();
		}

		stats = new PrimeConnectionEndStats(totalCount, successCount.get(), failureCount.get(), stopwatch.getDuration(TimeUnit.MILLISECONDS));

		printStats(stats);
	}

	private PrimeConnectionEndStats getEndStats() {
		return stats;
	}

	private void printStats(PrimeConnectionEndStats stats) {
		if (stats.total != stats.success) {
			log.info("Priming Connections not fully successful");
		} else {
			log.info("Priming connections fully successful");
		}
		log.debug("numServers left to be 'primed'="
				+ (stats.total - stats.success));
		log.debug("numServers successfully 'primed'=" + stats.success);
		log.debug("numServers whose attempts not complete exclusively due to max time allocated="
						+ (stats.total - (stats.success + stats.failure)));
		log.debug("Total Time Taken=" + stats.totalTime
				+ " msecs, out of an allocated max of (msecs)="
				+ maxTotalTimeToPrimeConnections);
		log.debug("stats = " + stats);
	}

	public List<Future<Boolean>> primeConnectionsAsync(final List<Server> servers, final PrimeConnectionListener listener) {
		if (servers == null) {
			return Collections.emptyList();
		}

		List<Server> allServers = new ArrayList<>();
		allServers.addAll(servers);
		if (allServers.size() == 0) {
			log.debug("RestClient: {}. No nodes/servers to prime connections");
			return Collections.emptyList();
		}

		log.info("Priming Connections for RestClient: {}, numServers: {}", name, allServers.size());

		List<Future<Boolean>> ftList = new ArrayList<>();
		for (Server server : allServers) {
			server.setReadyToServe(false);
			if (aSync) {
				Future<Boolean> ftC = null;
				try {
					ftC = makeConnectionsASync(server, listener);
					ftList.add(ftC);
				}
				catch (RejectedExecutionException ree) {
					log.error("executor submit failed", ree);
				}
				catch (Exception e) {
					log.error("general error", e);
				}
			}
			else {
				connectToServer(server, listener);
			}
		}

		return ftList;
	}

	private Future<Boolean> makeConnectionsASync(final Server server, final PrimeConnectionListener listener) {
		Callable<Boolean> ftConn = () -> {
			log.debug("calling primeConnections...");
			return connectToServer(server, listener);
		};

		return executorService.submit(ftConn);
	}

	public void shutdown() {
		executorService.shutdown();
		Monitors.unregisterObject(name + "_PrimeConnection", this);
	}

	private Boolean connectToServer(final Server server, final PrimeConnectionListener listener) {
		int tryNum = 0;
		Exception lastException = null;
		totalCounter.increment();
		boolean success = false;
		do {
			try {
				log.debug("Executing PrimeConnections request to server {} with path {}, tryNum = {}", server, primeConnectionURIPath, tryNum);
				success = connector.connect(server, primeConnectionURIPath);
				successCounter.increment();
			}
			catch (Exception e) {
				log.debug("Error connecting to server: {}", e.getMessage());
				lastException = e;
				sleepBeforeRetry(tryNum);
			}
			log.debug("server: {}, result = {}, tryNum = {}, maxRetries = {}", server, success, tryNum, maxRetries);
			tryNum++;
		} while (!success && tryNum <= maxRetries);

		if (listener != null) {
			try {
				listener.primeCompleted(server, lastException);
			}
			catch (Throwable e) {
				log.error("Error calling PrimeConnection listener", e);
			}
		}
		log.debug("Either done, or quitting server: {}, result = {}, tryNum = {}, maxRetries = {}",
				server, success, tryNum, maxRetries);

		return success;
	}

	private void sleepBeforeRetry(int tryNum) {
		try {
			int sleep = (tryNum + 1) * 100;
			log.debug("Sleeping for {}ms ...", sleep);
			Thread.sleep(sleep);
		}
		catch (InterruptedException e) {}
	}

	static class ASyncPrimeConnectionsThreadFactory implements ThreadFactory {

		private static final AtomicInteger groupNumber =  new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger threadNumber= new AtomicInteger(1);
		private final String namePrefix;

		ASyncPrimeConnectionsThreadFactory(String name) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "ASyncPrimeConnectionsThreadFactory-" + name + "-" + groupNumber.getAndIncrement() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (!t.isDaemon()) {
				t.setDaemon(true);
			}
			if (t.getPriority() != Thread.NORM_PRIORITY) {
				t.setPriority(Thread.NORM_PRIORITY);
			}
			return t;
		}
	}

	public interface PrimeConnectionListener {
		void primeCompleted(Server s, Throwable exception);
	}

	public static class PrimeConnectionEndStats {
		private final int total;
		private final int success;
		private final int failure;
		private final long totalTime;

		public PrimeConnectionEndStats(int total, int success, int failure, long totalTime) {
			this.total = total;
			this.success = success;
			this.failure = failure;
			this.totalTime = totalTime;
		}

		@Override
		public String toString() {
			return "PrimeConnectionEndStats{" +
					"total=" + total +
					", success=" + success +
					", failure=" + failure +
					", totalTime=" + totalTime +
					'}';
		}
	}
}
