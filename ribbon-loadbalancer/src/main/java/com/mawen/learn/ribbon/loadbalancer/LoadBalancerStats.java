package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public class LoadBalancerStats {

	private static final String PREFIX = "LBStats_";

	private static final DynamicIntProperty SERVERSTATS_EXPIRE_MINUTES = DynamicPropertyFactory.getInstance()
			.getIntProperty("niws.loadbalancer.serverStats.expire.minutes", 30);

	private final LoadingCache<Server, ServerStats> serverStatsCache = CacheBuilder.newBuilder()
			.expireAfterAccess(SERVERSTATS_EXPIRE_MINUTES.get(), TimeUnit.MINUTES)
			.removalListener((RemovalNotification<Server, ServerStats> notification) -> {
				notification.getValue().close();
			})
			.build(new CacheLoader<Server, ServerStats>() {
				@Override
				public ServerStats load(Server server) throws Exception {
					return createServerStats(server);
				}
			});

	String name;

	volatile Map<String, ZoneStats> zoneStatsMap = new ConcurrentHashMap<>();

	volatile Map<String, List<? extends Server>> upServerListZoneMap = new ConcurrentHashMap<>();

	private volatile DynamicIntProperty connectionFailureThreshold;

	private volatile DynamicIntProperty circuitTrippedTimeoutFactor;

	private volatile DynamicIntProperty maxCircuitTrippedTimeout;

	private LoadBalancerStats() {
		zoneStatsMap = new ConcurrentHashMap<>();
		upServerListZoneMap = new ConcurrentHashMap<>();
	}

	public LoadBalancerStats(String name) {
		this();
		this.name = name;
		Monitors.registerObject(name, this);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	DynamicIntProperty getConnectionFailureCountThreshold() {
		if (connectionFailureThreshold == null) {
			connectionFailureThreshold = DynamicPropertyFactory.getInstance().getIntProperty(
					"niws.loadbalancer." + name + ".connectionFailureCountThreshold", 3);
		}
		return connectionFailureThreshold;
	}

	DynamicIntProperty getCircuitTrippedTimeoutFactor() {
		if (circuitTrippedTimeoutFactor == null) {
			circuitTrippedTimeoutFactor = DynamicPropertyFactory.getInstance().getIntProperty(
					"niws.loadbalancer." + name + ".circuitTrippedTimeoutFactorSeconds", 10);
		}
		return circuitTrippedTimeoutFactor;
	}

	DynamicIntProperty getCircuitTripMaxTimeoutSeconds() {
		if (maxCircuitTrippedTimeout == null) {
			maxCircuitTrippedTimeout = DynamicPropertyFactory.getInstance().getIntProperty(
					"niws.loadbalancer." + name + ".circuitTripMaxTimeoutSeconds", 30);
		}
		return maxCircuitTrippedTimeout;
	}

	public void updateServerList(List<Server> servers) {
		for (Server server : servers) {
			addServer(server);
		}
	}

	public void addServer(Server server) {
		try {
			serverStatsCache.get(server);
		}
		catch (ExecutionException e) {
			ServerStats stats = createServerStats(server);
			serverStatsCache.asMap().putIfAbsent(server, stats);
		}
	}

	public void noteResponseTime(Server server, double msecs) {
		ServerStats ss = getServerStats(server);
		ss.noteResponseTime(msecs);
	}

	private ServerStats getServerStats(Server server) {
		try {
			return serverStatsCache.get(server);
		}
		catch (ExecutionException e) {
			ServerStats ss = createServerStats(server);
			serverStatsCache.asMap().putIfAbsent(server, ss);
			return serverStatsCache.asMap().get(server);
		}
	}

	public void incrementActiveRequestsCount(Server server) {
		ServerStats stats = getServerStats(server);
		stats.incrementActiveRequestsCount();
	}

	public void decrementActiveRequestsCount(Server server) {
		ServerStats stats = getServerStats(server);
		stats.decrementActiveRequestsCount();
	}

	private ZoneStats getZoneStats(String zone) {
		zone = zone.toLowerCase();
		ZoneStats zs = zoneStatsMap.get(zone);
		if (zs == null) {
			zoneStatsMap.put(zone, new ZoneStats(this.getName(), zone, this));
			zs = zoneStatsMap.get(zone);
		}
		return zs;
	}

	public boolean isCircuitBreakerTripped(Server server) {
		ServerStats ss = getServerStats(server);
		return ss.isCircuitBreakerTripped();
	}

	public void incrementSuccessiveConnectionFailureCount(Server server) {
		ServerStats ss = getServerStats(server);
		ss.incrementSuccessiveConnectionFailureCount();
	}

	public void clearSuccessiveConnectionFailureCount(Server server) {
		ServerStats ss = getServerStats(server);
		ss.clearSuccessiveConnectionFailureCount();
	}

	public void incrementNumRequests(Server server) {
		ServerStats ss = getServerStats(server);
		ss.incrementNumRequests();
	}

	public void incrementZoneCounter(Server server) {
		String zone = server.getZone();
		if (zone != null) {
			getZoneStats(zone).incrementCounter();
		}
	}

	public void updateZoneServerMapping(Map<String, List<Server>> map) {
		upServerListZoneMap = new ConcurrentHashMap<>(map);
		for (String zone : map.keySet()) {
			getZoneStats(zone);
		}
	}

	public int getInstanceCount(String zone) {
		if (zone == null) {
			return 0;
		}
		zone = zone.toLowerCase();
		List<? extends Server> currentList = upServerListZoneMap.get(zone);
		if (currentList == null) {
			return 0;
		}
		return currentList.size();
	}

	public int getActiveRequestsCount(String zone) {
		return getZoneStats(zone).getActiveRequestsCount();
	}

	public double getActiveRequestsPerServer(String zone) {
		return getZoneSnapshot(zone).getLoadPerServer();
	}

	public ZoneSnapshot getZoneSnapshot(String zone) {
		if (zone == null) {
			return new ZoneSnapshot();
		}
		zone = zone.toLowerCase();
		List<? extends Server> currentList = upServerListZoneMap.get(zone);
		return getZoneSnapshot(currentList);
	}

	public ZoneSnapshot getZoneSnapshot(List<? extends Server> servers) {
		if (servers == null || servers.isEmpty()) {
			return new ZoneSnapshot();
		}
		int instanceCount = servers.size();
		int activeConnectionCount = 0;
		int activeConnectionsCountOnAvailableServer = 0;
		int circuitBreakerTrippedCount = 0;
		double loadPerServer = 0;
		long currentTime = System.currentTimeMillis();

		for (Server server : servers) {
			ServerStats stat = getSingleServerStat(server);
			if (stat.isCircuitBreakerTripped(currentTime)) {
				circuitBreakerTrippedCount++;
			}
			else {
				activeConnectionsCountOnAvailableServer += stat.getActiveRequestsCount(currentTime);
			}
			activeConnectionCount += stat.getActiveRequestsCount(currentTime);
		}

		if (circuitBreakerTrippedCount == instanceCount) {
			if (instanceCount > 0) {
				loadPerServer = -1;
			}
		}
		else {
			loadPerServer = ((double) activeConnectionsCountOnAvailableServer) / (instanceCount - circuitBreakerTrippedCount);
		}
		return new ZoneSnapshot(instanceCount, circuitBreakerTrippedCount, activeConnectionCount, loadPerServer);
	}

	public int getCircuitBreakerTrippedCount(String zone) {
		return getZoneSnapshot(zone).getCircuitTrippedCount();
	}

	@Monitor(name = PREFIX + "CircuitBreakerTrippedCount", type = DataSourceType.GAUGE)
	public int getCircuitBreakerTrippedCount() {
		int count = 0;
		for (String zone : upServerListZoneMap.keySet()) {
			count += getCircuitBreakerTrippedCount(zone);
		}
		return count;
	}

	public long getMeasuredZoneHits(String zone) {
		if (zone == null) {
			return 0;
		}
		zone = zone.toLowerCase();
		long count = 0;
		List<? extends Server> currentList = upServerListZoneMap.get(zone);
		if (currentList == null) {
			return 0;
		}
		for (Server server : currentList) {
			ServerStats stats = getSingleServerStat(server);
			count += stats.getMeasureRequestsCount();
		}
		return count;
	}

	public int getCongestionRatePercentage(String zone) {
		if (zone == null) {
			return 0;
		}
		zone = zone.toLowerCase();
		List<? extends Server> currentList = upServerListZoneMap.get(zone);
		if (currentList == null || currentList.isEmpty()) {
			return 0;
		}

		int serverCount = currentList.size();
		int activeConnectionsCount = 0;
		int circuitBreakerTrippedCount = 0;
		for (Server server : currentList) {
			ServerStats ss = getSingleServerStat(server);
			activeConnectionsCount += ss.getActiveRequestsCount();
			if (ss.isCircuitBreakerTripped()) {
				circuitBreakerTrippedCount++;
			}
		}
		return (int) ((activeConnectionsCount + circuitBreakerTrippedCount) * 100L / serverCount);
	}

	@Monitor(name = PREFIX + "AvailableZones", type = DataSourceType.INFORMATIONAL)
	public Set<String> getAvailableZones() {
		return upServerListZoneMap.keySet();
	}

	public ServerStats getSingleServerStat(Server server) {
		return getServerStats(server);
	}

	public Map<Server, ServerStats> getServerStats() {
		return serverStatsCache.asMap();
	}

	public Map<String, ZoneStats> getZoneStats() {
		return zoneStatsMap;
	}

	@Override
	public String toString() {
		return "Zone stats: " + zoneStatsMap.toString()
				+ "," + "Server stats: " + getSortedServerStats(getServerStats().values()).toString();
	}

	private static Comparator<ServerStats> serverStatsComparator = new Comparator<ServerStats>() {
		@Override
		public int compare(ServerStats o1, ServerStats o2) {
			String zone1 = "";
			String zone2 = "";
			if (o1.server != null && o1.server.getZone() != null) {
				zone1 = o1.server.getZone();
			}
			if (o2.server != null && o2.server.getZone() != null) {
				zone2 = o2.server.getZone();
			}
			return zone1.compareTo(zone2);
		}
	};

	private static Collection<ServerStats> getSortedServerStats(Collection<ServerStats> serverStats) {
		List<ServerStats> list = new ArrayList<>(serverStats);
		Collections.sort(list, serverStatsComparator);
		return list;
	}

	private ServerStats createServerStats(Server server) {
		ServerStats ss = new ServerStats(this);
		ss.setBufferSize(1000);
		ss.setPublishInterval(1000);
		ss.initialize(server);
		return ss;
	}
}
