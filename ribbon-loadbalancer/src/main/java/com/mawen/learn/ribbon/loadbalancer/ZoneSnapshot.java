package com.mawen.learn.ribbon.loadbalancer;

import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Getter
public class ZoneSnapshot {

	final int instanceCount;

	final double loadPerServer;

	final int circuitTrippedCount;

	final int activeRequestsCount;

	public ZoneSnapshot() {
		this(0,  0, 0, 0d);
	}

	public ZoneSnapshot(int instanceCount, int circuitTrippedCount, int activeRequestsCount, double loadPerServer) {
		this.instanceCount = instanceCount;
		this.loadPerServer = loadPerServer;
		this.circuitTrippedCount = circuitTrippedCount;
		this.activeRequestsCount = activeRequestsCount;
	}

	@Override
	public String toString() {
		return "ZoneSnapshot [instanceCount=" + instanceCount
				+ ", loadPerServer=" + loadPerServer + ", circuitTrippedCount="
				+ circuitTrippedCount + ", activeRequestsCount="
				+ activeRequestsCount + "]";
	}
}
