package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mawen.learn.ribbon.client.ClientFactory;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.DynamicIntProperty;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.omg.SendingContext.RunTime;

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

	public DynamicServerListLoadBalancer(IClientConfig clientConfig) {
		initWithNIWSConfig(clientConfig);
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		try {
			super.initWithNIWSConfig(clientConfig);
			String niwsServerListClassName = clientConfig.getProperty(CommonClientConfigKey.NIWSServerListClassName, DefaultClientConfigImpl.DEFAULT_SEVER_LIST_CLASS).toString();

			ServerList<T> niwsServerListImpl = (ServerList<T>) ClientFactory.instantiateInstanceWithClientConfig(niwsServerListClassName, clientConfig);
			this.serverListImpl = niwsServerListImpl;

			if (niwsServerListImpl instanceof AbstractServerList) {
				AbstractServerListFilter<T> niwsFilter = ((AbstractServerList) niwsServerListImpl).getFilterImpl(clientConfig);
				niwsFilter.setLoadBalancerStats(getLoadBalancerStats());
				this.filter = niwsFilter;
			}

			restOfInit(clientConfig);
		}
		catch (Exception e) {
			throw new RuntimeException("Exception while initializing NIWSDiscoveryLoadBalancer: " + clientConfig.getClientName() + ", niwsClientConfig: " + clientConfig, e);
		}
	}

	void restOfInit(IClientConfig clientConfig) {
		refreshIntervalMills = clientConfig.get(CommonClientConfigKey.ServerListRefreshInterval, LISTOFSERVERS_CACHE_REPEAT_INTERVAL);

		boolean primeConnection = this.isEnablePrimingConnections();
		this.setEnablePrimingConnections(false);
		enableAndInitLearnNewServersFeature();

		updateListOfServers();
		if (primeConnection && this.getPrimeConnections() != null) {
			this.getPrimeConnections().primeConnections(getServerList(true));
		}
		this.setEnablePrimingConnections(primeConnection);
		log.info("DynamicServerListLoadBalancer for client {} initialized: {}", clientConfig.getClientName(), this.toString());
	}

	@Override
	public void setServersList(List lsrv) {
		super.setServersList(lsrv);
		List<T> serverList = lsrv;
		Map<String, List<Server>> serversInZones = new HashMap<>();
		for (Server server : serverList) {
			getLoadBalancerStats().getSingleServerStat(server);
			String zone = server.getZone();
			if (zone != null) {
				zone = zone.toLowerCase();
				List<Server> servers = serversInZones.get(zone);
				if (servers == null) {
					servers = new ArrayList<>();
					serversInZones.put(zone, servers);
				}
				servers.add(server);
			}
		}
		setServerListForZones(serversInZones);
	}

	protected void setServerListForZones(Map<String, List<Server>> zoneServersMap) {
		log.debug("Setting server list for zones: {}", zoneServersMap);
		getLoadBalancerStats().updateZoneServerMapping(zoneServersMap);
	}

	public ServerList<T> getServerListImpl() {
		return serverListImpl;
	}

	public void setServerListImpl(ServerList<T> serverListImpl) {
		this.serverListImpl = serverListImpl;
	}

	@Override
	public void setPing(IPing ping) {
		this.ping = ping;
	}

	public ServerListFilter<T> getFilter() {
		return filter;
	}

	public void setFilter(ServerListFilter<T> filter) {
		this.filter = filter;
	}

	public void forceQuickPing() {
		// no-op
	}

	public void enableAndInitLearnNewServersFeature() {
		keepServerListUpdated();
		serverRefreshEnabled = true;
	}

	private String getIdentifier() {
		return this.getClientConfig().getClientName();
	}

	private void keepServerListUpdated() {
		scheduledFuture = _serverListRefreshExecutor.scheduleAtFixedRate(
				new ServerListRefreshExecutorThread(),
				LISTOFSERVERS_CACHE_UPDATE_DELAY,
				refreshIntervalMills,
				TimeUnit.MILLISECONDS
		);
	}

	private static void shutdownExecutorPool() {
		if (_serverListRefreshExecutor != null) {
			_serverListRefreshExecutor.shutdown();

			if (_shutdownThread != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(_shutdownThread);
				}
				catch (IllegalStateException e) {

				}
			}
		}
	}

	public void stopServerListRefreshing() {
		serverRefreshEnabled = false;
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
		}
	}

	class ServerListRefreshExecutorThread extends Thread {

		@Override
		public void run() {
			if (!serverRefreshEnabled) {
				if (scheduledFuture != null) {
					scheduledFuture.cancel(true);
				}
				return;
			}

			try {
				updateListOfServers();
			}
			catch (Throwable e) {
				log.error("Exception while updating List of Servers obtained from Discovery client", e);
			}
		}
	}

	public void updateListOfServers() {
		List<T> servers = new ArrayList<>();
		if (serverListImpl != null) {
			servers = serverListImpl.getUpdatedListOfServers();
			log.debug("List of Servers for {} obtained from Discovery client: {}",
					getIdentifier(), servers);

			if (filter != null) {
				servers = filter.getFilteredListOfServers(servers);
				log.debug("Filtered List of Servers for {} obtained from Discovery client: {}",
						getIdentifier(), servers);
			}
		}
		lastUpdated.set(System.currentTimeMillis());
		updateAllServerList(servers);
	}

	protected void updateAllServerList(List<T> ls) {
		if (serverListUpdateInProgress.compareAndSet(false, true)) {
			for (T s : ls) {
				s.setAliveFlag(true);
			}
			setServersList(ls);
			super.forceQuickPing();
			serverListUpdateInProgress.set(false);
		}
	}

	@Monitor(name = "NumUpdateCycleMissed", type = DataSourceType.GAUGE)
	public int getNumberMissedCycles() {
		if (!serverRefreshEnabled) {
			return 0;
		}
		return (int) ((int) (System.currentTimeMillis() - lastUpdated.get()) / refreshIntervalMills);
	}

	@Monitor(name = "LastUpdated", type = DataSourceType.INFORMATIONAL)
	public String getLastUpdated() {
		return new Date(lastUpdated.get()).toString();
	}

	@Monitor(name = "NumThreads", type = DataSourceType.GAUGE)
	public int getCoreThreads() {
		if (_serverListRefreshExecutor != null) {
			return _serverListRefreshExecutor.getCorePoolSize();
		}
		else {
			return 0;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DynamicServerListLoadBalancer:");
		sb.append(super.toString());
		sb.append("ServerList:" + String.valueOf(serverListImpl));
		return sb.toString();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		stopServerListRefreshing();
	}
}
