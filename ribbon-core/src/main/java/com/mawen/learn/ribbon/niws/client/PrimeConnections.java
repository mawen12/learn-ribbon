package com.mawen.learn.ribbon.niws.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.mawen.learn.ribbon.loadbalancer.Server;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class PrimeConnections {

	private static final Logger log = LoggerFactory.getLogger(PrimeConnections.class);

	private String primeConnectionURIPath = "/";

	private ExecutorService executorService;

	private int maxExecutorThreads = 5;

	private int executorThreadTimeout = 30000;

	private String name = "default";

	private int maxTasksPerExecutorQueue = 100;

	private float primeRatio = 1.0f;

	private int maxRetries = 2;

	private long maxTotalTimeToPrimeConnections = 30 * 1000;

	private long totalTimeTaken = 0;

	private boolean aSync = true;

	private Counter totalCounter;

	private Counter successCounter;

	private Timer initialPrimeTimer;

	private IPrimeConnection connector;

	private PrimeConnections() {}

	public PrimeConnections(String name, NiwsClientConfig niwsClientConfig) {
		int maxRetriesPerServerPrimeConnection = Integer.parseInt(NiwsClientConfig.DEFAULT_)
	}

	public interface PrimeConnectionListener {

		void primeCompleted(Server s, Throwable lastException);
	}

	static class PrimeConnectionCounters {
		final AtomicInteger numServersLeft;
		final AtomicInteger numServers;
		final AtomicInteger numServersSuccessful;

		public PrimeConnectionCounters(int initialSize) {
			this.numServersLeft = new AtomicInteger(initialSize);
			this.numServers = new AtomicInteger(initialSize);
			this.numServersSuccessful = new AtomicInteger(0);
		}
	}
}
