package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

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
			.build((CacheLoader) server -> createServerStats(server));

	String name;

	volatile Map<String, ZoneStats> zoneStatsMap = new ConcurrentHashMap<>();

	volatile Map<String, List<? extends Server>> upServerListZoneMap = new ConcurrentHashMap<>();

	private volatile DynamicIntProperty connectionFailureThreshold;

	private volatile DynamicIntProperty circuitTrippedTimeoutFactor;

	private volatile DynamicIntProperty maxCircuitTrippedTimeout;


}
