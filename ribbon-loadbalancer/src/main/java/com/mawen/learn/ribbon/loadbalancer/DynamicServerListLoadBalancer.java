package com.mawen.learn.ribbon.loadbalancer;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.DynamicIntProperty;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
@Slf4j
public class DynamicServerListLoadBalancer<T extends Server> extends BaseLoadBalancer {

	private static final String CORE_THREAD = "DynamicServerListLoadBalancer.ThreadPoolSize";

	private static final DynamicIntProperty poolSizeProp = new DynamicIntProperty(CORE_THREAD, 2);

	private static long LISTOFSERVERS_CACHE_UPDATE_DELAY = 1000;

	private static int LISTOFSERVERS_CACHE_REPEAT_INTERVAL = 30 * 1000;

	private static Thread _shutdownThread;

	private static ScheduledThreadPoolExecutor _serverListRefreshExecutor;

	private long refreshIntervalMills = LISTOFSERVERS_CACHE_REPEAT_INTERVAL;

	protected AtomicBoolean serverListUpdateInProgress = new AtomicBoolean(false);

	private AtomicLong lastUpdated = new AtomicLong(System.currentTimeMillis());

	boolean isSecure = false;
	boolean useTunnel = false;

	volatile ServerList<T> serverListImpl;

	volatile ServerListFilter<T> filter;

	protected volatile boolean serverRefreshEnabled = false;

	private volatile ScheduledFuture<?> scheduledFuture;

	static {
		int coreSize = poolSizeProp.get();
		ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(true).build();
		_serverListRefreshExecutor = new ScheduledThreadPoolExecutor(coreSize, factory);
		poolSizeProp.addCallback(() -> _serverListRefreshExecutor.setCorePoolSize(poolSizeProp.get()));
		_shutdownThread = new Thread(() -> {
			log.info("Shutting down the Executor Pool for DynamicServerListLoadBalancer");
			shutdownExecutorPool();
		});
		Runtime.getRuntime().addShutdownHook(_shutdownThread);
	}

	public DynamicServerListLoadBalancer() {
		super();
	}

	public DynamicServerListLoadBalancer(IClientConfig clientConfig, IRule rule, IPing ping, ServerList<T> serverList, ServerListFilter<T> filter) {
		super(clientConfig, rule, ping);
		this.serverListImpl = serverList;
		this.filter = filter;
		if (filter instanceof AbstractServerListFilter) {
			((AbstractServerListFilter<T>) filter).setLoadBalancerStats(getLoadBalancerStats());
		}
		restOfInit(clientConfig);
	}


}
