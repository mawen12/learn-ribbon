package com.mawen.learn.ribbon.loadbalancer;

import java.util.Collections;
import java.util.List;

import com.mawen.learn.ribbon.client.IClientConfigAware;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.servo.monitor.Counter;
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

	private ZoneAffinityPredicate zoneAffinityPredicate = new ZoneAffinityPredicate<>();

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {

	}

	@Override
	public List<T> getFilteredListOfServers(List<T> servers) {
		return Collections.emptyList();
	}
}
