package com.mawen.learn.ribbon.loadbalancer;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

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
public class ZoneAvoidancePredicate extends AbstractServerPredicate {

	private static final DynamicBooleanProperty ENABLED = DynamicPropertyFactory.getInstance().getBooleanProperty("niws.loadbalancer.zoneAvoidanceRule.enabled", true);

	private volatile DynamicDoubleProperty triggeringLoad = new DynamicDoubleProperty("ZoneAwareNIWSDiscoveryLoadBalancer.triggeringLoadPerServerThreshold", 0.2d);

	private volatile DynamicDoubleProperty triggeringBlackoutPercentage = new DynamicDoubleProperty("ZoneAwareNIWSDiscoveryLoadBalancer.avoidZoneWithBlackoutPercetage", 0.99999d);

	public ZoneAvoidancePredicate(IRule rule, IClientConfig clientConfig) {
		super(rule, clientConfig);
		initDynamicProperties(clientConfig);
	}

	public ZoneAvoidancePredicate(LoadBalancerStats lbStats, IClientConfig clientConfig) {
		super(lbStats, clientConfig);
		initDynamicProperties(clientConfig);
	}

	ZoneAvoidancePredicate(IRule rule) {
		super(rule);
	}

	private void initDynamicProperties(IClientConfig clientConfig) {
		if (clientConfig != null) {
			triggeringLoad = DynamicPropertyFactory.getInstance().getDoubleProperty(
					"ZoneAwareNIWSDiscoveryLoadBalancer." + clientConfig.getClientName() + ".triggeringLoadPerServerThreshold", 0.2d);

			triggeringBlackoutPercentage = DynamicPropertyFactory.getInstance().getDoubleProperty(
					"ZoneAwareNIWSDiscoveryLoadBalancer." + clientConfig.getClientName() + ".avoidZoneWithBlackoutPercetage", 0.99999d);
		}
	}

	@Override
	public boolean apply(@Nullable PredicateKey input) {
		if (!ENABLED.get()) {
			return true;
		}

		String serverZone = input.getServer().getZone();
		if (serverZone == null) {
			return true;
		}

		LoadBalancerStats lbStats = getLBStats();
		if (lbStats == null || lbStats.getAvailableZones().size() <= 1) {
			return true;
		}

		Map<String, ZoneSnapshot> zoneSnapshot = ZoneAvoidanceRule.createSnapshot(lbStats);
		if (!zoneSnapshot.keySet().contains(serverZone)) {
			return true;
		}

		log.debug("Zone snapshots: {}", zoneSnapshot);
		Set<String> availableZones = ZoneAvoidanceRule.getAvailableZones(zoneSnapshot, triggeringLoad.get(), triggeringBlackoutPercentage.get());
		log.debug("Available zones: {}", availableZones);

		if (availableZones != null) {
			return availableZones.contains(input.getServer().getZone());
		}
		else {
			return false;
		}
	}
}
