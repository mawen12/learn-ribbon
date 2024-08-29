package com.mawen.learn.ribbon.loadbalancer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mawen.learn.ribbon.client.ClientFactory;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicPropertyFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
@Slf4j
public class ZoneAwareLoadBalancer<T extends Server> extends DynamicServerListLoadBalancer<T> {

	private static final DynamicBooleanProperty ENABLED = DynamicPropertyFactory.getInstance().getBooleanProperty("ZoneAwareNIWSDiscoveryLoadBalancer.enabled", true);

	private ConcurrentHashMap<String, BaseLoadBalancer> balancers = new ConcurrentHashMap<>();

	private volatile DynamicDoubleProperty triggeringLoad;

	private volatile DynamicDoubleProperty triggeringBlackoutPercentage;

	public ZoneAwareLoadBalancer() {
		super();
	}

	public ZoneAwareLoadBalancer(IClientConfig clientConfig, IRule rule, IPing ping, ServerList<T> serverList, ServerListFilter<T> filter) {
		super(clientConfig, rule, ping, serverList, filter);
	}

	public ZoneAwareLoadBalancer(IClientConfig clientConfig) {
		super(clientConfig);
	}

	void setupServerList(List<Server> upServerList) {
		this.upServerList = upServerList;
	}


	@Override
	protected void setServerListForZones(Map<String, List<Server>> zoneServersMap) {
		super.setServerListForZones(zoneServersMap);
		if (balancers == null) {
			balancers = new ConcurrentHashMap<>();
		}

		for (Map.Entry<String, List<Server>> entry : zoneServersMap.entrySet()) {
			String zone = entry.getKey().toLowerCase();
			getLoadBalancer(zone).setServersList(entry.getValue());
		}

		for (Map.Entry<String, BaseLoadBalancer> existingLBEntry : balancers.entrySet()) {
			if (!zoneServersMap.keySet().contains(existingLBEntry.getKey())) {
				existingLBEntry.getValue().setServersList(Collections.emptyList());
			}
		}
	}

	@Override
	public Server chooseServer(Object key) {
		if (!ENABLED.get() || getLoadBalancerStats().getAvailableZones().size() <= 1) {
			log.debug("Zone aware logic disabled or there is only one zone");
			return super.chooseServer(key);
		}

		Server server = null;
		try {
			LoadBalancerStats lbStats = getLoadBalancerStats();
			Map<String, ZoneSnapshot> zoneSnapshot = ZoneAvoidanceRule.createSnapshot(lbStats);
			log.debug("Zone snapshots: {}", zoneSnapshot);

			if (triggeringLoad == null) {
				triggeringLoad = DynamicPropertyFactory.getInstance().getDoubleProperty("ZoneAwareNIWSDiscoveryLoadBalancer." + this.getName() + ".triggeringLoadPerServerThreshold", 0.2d);
			}

			if (triggeringBlackoutPercentage == null) {
				triggeringBlackoutPercentage = DynamicPropertyFactory.getInstance().getDoubleProperty("ZoneAwareNIWSDiscoveryLoadBalancer." + this.getName() + ".avoidZoneWithBlackoutPercetage", 0.99999d);
			}
			Set<String> availableZones = ZoneAvoidanceRule.getAvailableZones(zoneSnapshot, triggeringLoad.get(), triggeringBlackoutPercentage.get());
			log.debug("Available zones: {}", availableZones);
			if (availableZones != null && availableZones.size() < zoneSnapshot.keySet().size()) {
				String zone = ZoneAvoidanceRule.randomChooseZone(zoneSnapshot, availableZones);
				log.debug("Zone chosen: {}", zone);
				if (zone != null) {
					BaseLoadBalancer zoneLoadBalancer = getLoadBalancer(zone);
					server = zoneLoadBalancer.chooseServer(key);
				}
			}
		}
		catch (Throwable e) {
			log.error("Unexpected exception when choosing server using zone aware logic", e);
		}

		if (server != null) {
			return server;
		}
		else {
			log.debug("Zone avoidance logic is not invoked.");
			return super.chooseServer(key);
		}
	}


	BaseLoadBalancer getLoadBalancer(String zone) {
		zone = zone.toLowerCase();
		BaseLoadBalancer loadBalancer = balancers.get(zone);
		if (loadBalancer == null) {
			IRule rule = cloneRule(this.getRule());
			loadBalancer = new BaseLoadBalancer(this.getName() + "_" + zone, rule, this.getLoadBalancerStats());
			BaseLoadBalancer prev = balancers.putIfAbsent(zone, loadBalancer);
			if (prev != null) {
				loadBalancer = prev;
			}
		}
		return loadBalancer;
	}

	private IRule cloneRule(IRule toClone) {
		IRule rule;
		if (toClone == null) {
			rule = new AvailabilityFilteringRule();
		}
		else {
			String ruleClass = toClone.getClass().getName();
			try {
				rule = (IRule) ClientFactory.instantiateInstanceWithClientConfig(ruleClass, this.getClientConfig());
			}
			catch (Exception e) {
				throw new RuntimeException("Unexpected exception creating rule for ZoneAwareLoadBalancer", e);
			}
		}
		return rule;
	}

	@Override
	public void setRule(IRule rule) {
		super.setRule(rule);
		if (balancers != null) {
			for (String zone : balancers.keySet()) {
				balancers.get(zone).setRule(cloneRule(rule));
			}
		}
	}
}
