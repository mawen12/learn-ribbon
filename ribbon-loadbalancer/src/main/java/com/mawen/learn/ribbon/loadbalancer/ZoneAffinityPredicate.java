package com.mawen.learn.ribbon.loadbalancer;

import javax.annotation.Nullable;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class ZoneAffinityPredicate extends AbstractServerPredicate {

	private final String zone = ConfigurationManager.getDeploymentContext().getValue(DeploymentContext.ContextKey.zone);

	@Override
	public boolean apply(@Nullable PredicateKey input) {
		Server s = input.getServer();
		String az = s.getZone();
		if (az != null && zone != null && az.toLowerCase().equals(zone.toLowerCase())) {
			return true;
		}
		else {
			return false;
		}
	}
}
