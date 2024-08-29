package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mawen.learn.ribbon.client.IClientConfigAware;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class ZoneAffinityServerListFilter<T extends Server> extends AbstractServerListFilter<T> implements IClientConfigAware {

	private volatile boolean zoneAffinity = DefaultClientConfigImpl.DEFAULT_ENABLE_ZONE_AFFINITY;

	private volatile boolean zoneExclusive = DefaultClientConfigImpl.DEFAULT_ENABLE_ZONE_EXCLUSIVITY;

	private DynamicDoubleProperty activeRequestsPerServerThreshold;

	private DynamicDoubleProperty blackOutServerPercentageThreshold;

	private DynamicIntProperty availableServersThreshold;

	private Counter overrideCounter;

	private ZoneAffinityPredicate zoneAffinityPredicate = new ZoneAffinityPredicate();

	String zone;

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		String sZoneAffinity = "" + clientConfig.getProperty(CommonClientConfigKey.EnableZoneAffinity, false);
		if (sZoneAffinity != null) {
			zoneAffinity = Boolean.parseBoolean(sZoneAffinity);
			log.debug("ZoneAffinity is set to {}", zoneAffinity);
		}

		String zZoneExclusive = "" + clientConfig.getProperty(CommonClientConfigKey.EnableZoneExclusivity, false);
		if (zZoneExclusive != null) {
			zoneExclusive = Boolean.parseBoolean(zZoneExclusive);
			log.debug("ZoneExclusive is set to {}", zoneExclusive);
		}

		if (ConfigurationManager.getDeploymentContext() != null) {
			zone = ConfigurationManager.getDeploymentContext().getValue(DeploymentContext.ContextKey.zone);
		}

		activeRequestsPerServerThreshold = DynamicPropertyFactory.getInstance().getDoubleProperty(
				clientConfig.getClientName() + "." + clientConfig.getNameSpace() + ".zoneAffinity.maxLoadPerServer", 0.6d);
		log.debug("activeRequestsPerServerThreshold is set to {}", activeRequestsPerServerThreshold.get());

		blackOutServerPercentageThreshold = DynamicPropertyFactory.getInstance().getDoubleProperty(
			clientConfig.getClientName() + "." + clientConfig.getNameSpace() +" .zoneAffinity.maxBlackOutServersPercentage", 0.8d);
		log.debug("blackOutServerPercentageThreshold is set to {}", blackOutServerPercentageThreshold.get());

		availableServersThreshold = DynamicPropertyFactory.getInstance().getIntProperty(
				clientConfig.getClientName() + "." + clientConfig.getNameSpace() + ".zoneAffinity.maxAvailableServers", 2);
		log.debug("availableServersThreshold is set to {}", availableServersThreshold.get());

		Monitors.registerObject("NIWSServerListFilter_" + clientConfig.getClientName());
	}

	private boolean shouldEnableZoneAffinity(List<T> filtered) {
		if (!zoneAffinity && !zoneExclusive) {
			return false;
		}

		if (zoneExclusive) {
			return true;
		}

		LoadBalancerStats stats = getLoadBalancerStats();
		if (stats == null) {
			return zoneAffinity;
		}
		else {
			log.debug("Determining if zone affinity should be enabled with given server list: {}", filtered);
			ZoneSnapshot snapshot = stats.getZoneSnapshot(filtered);
			double loadPerServer = snapshot.getLoadPerServer();
			int instanceCount = snapshot.getInstanceCount();
			int circuitTrippedCount = snapshot.getCircuitTrippedCount();
			if (((double) circuitTrippedCount) / instanceCount >= blackOutServerPercentageThreshold.get()
					|| loadPerServer >= activeRequestsPerServerThreshold.get()
					|| (instanceCount - circuitTrippedCount) < availableServersThreshold.get()) {
				log.debug("zoneAffinity is overriden. blackOutServerPercentage: {}, activeRequestsPerServer: {}, availableServers: {}",
						(double) circuitTrippedCount / instanceCount, loadPerServer, instanceCount - circuitTrippedCount);
				return false;
			}
			else {
				return true;
			}
		}
	}

	@Override
	public List<T> getFilteredListOfServers(List<T> servers) {
		if (zone != null && (zoneAffinity || zoneExclusive) && servers != null && !servers.isEmpty()) {
			List<T> filteredServers = Lists.newArrayList(Iterables.filter(servers, this.zoneAffinityPredicate.getServerOnlyPredicate()));
			if (shouldEnableZoneAffinity(filteredServers)) {
				return filteredServers;
			}
			else if (zoneAffinity) {
				overrideCounter.increment();
			}
		}
		return servers;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("ZoneAffinityServerListFilter:");
		sb.append(", zone: ").append(zone).append(", zoneAffinity:").append(zoneAffinity);
		sb.append(", zoneExclusivity:").append(zoneExclusive);
		return sb.toString();
	}
}
