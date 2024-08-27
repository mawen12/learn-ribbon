package com.mawen.learn.ribbon.loadbalancer;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Getter
public class ZoneStats<T extends Server> {

	private static final String PREFIX = "ZoneStats_";

	private final LoadBalancerStats loadBalancerStats;

	private final String zone;

	private final Counter counter;

	final String monitorId;

	public ZoneStats(String name, String zone, LoadBalancerStats loadBalancerStats) {
		this.zone = zone;
		this.loadBalancerStats = loadBalancerStats;
		this.monitorId = name + ":" + zone;
		this.counter = Monitors.newCounter(PREFIX + name + "_" + zone + "_Counter");
		Monitors.registerObject(monitorId, this);
	}

	@Monitor(name = PREFIX + "ActiveRequestsCount", type = DataSourceType.INFORMATIONAL)
	public int getActiveRequestsCount() {
		return loadBalancerStats.getActiveRequestsCount(zone);
	}

	@Monitor(name = PREFIX + "InstanceCount", type = DataSourceType.GAUGE)
	public int getInstanceCount() {
		return loadBalancerStats.getInstanceCount(zone);
	}

	@Monitor(name = PREFIX + "CircuitBreakerTrippedCount", type = DataSourceType.GAUGE)
	public int getCircuitBreakerTrippedCount() {
		return loadBalancerStats.getCircuitBreakerTrippedCount(zone);
	}

	@Monitor(name = PREFIX + "ActiveRequestsPerServer", type = DataSourceType.GAUGE)
	public double getActiveRequestsPerServer() {
		return loadBalancerStats.getActiveRequestsPerServer(zone);
	}

	@Monitor(name = PREFIX + "RequestsMadeLast5Minutes", type = DataSourceType.GAUGE)
	public long getMeasuredZoneHits() {
		return loadBalancerStats.getMeasuredZoneHits(zone);
	}

	@Monitor(name = PREFIX + "CircuitBreakerTrippedPercentage", type = DataSourceType.INFORMATIONAL)
	public double getCircuitBreakerTrippedPercentage() {
		ZoneSnapshot snapshot = loadBalancerStats.getZoneSnapshot(zone);
		int totalCount = snapshot.getInstanceCount();
		int circuitTrippedCount = snapshot.getCircuitTrippedCount();
		if (totalCount == 0) {
			if (circuitTrippedCount != 0) {
				return -1;
			}
			else {
				return 0;
			}
		}
		else {
			return circuitTrippedCount / (double) totalCount;
		}
	}

	void incrementCounter() {
		counter.increment();
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("[Zone:" + zone + ";");
		sb.append("\tInstance count:" + getInstanceCount() + ";");
		sb.append("\tActive connections count: " + getActiveRequestsCount() + ";");
		sb.append("\tCircuit breaker tripped count: " + getCircuitBreakerTrippedCount() + ";");
		sb.append("\tActive connections per server: " + getActiveRequestsPerServer() + ";");
		sb.append("]\n");
		return sb.toString();
	}
}
