package com.mawen.learn.ribbon.transport.netty;

import com.netflix.config.DynamicProperty;
import io.reactivex.netty.client.MaxConnectionsBasedStrategy;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/30
 */
public class DynamicPropertyBasedPoolStrategy extends MaxConnectionsBasedStrategy {

	private final DynamicProperty poolSizeProperty;

	public DynamicPropertyBasedPoolStrategy(final int maxConnections, String propertyName) {
		super(maxConnections);

		poolSizeProperty = DynamicProperty.getInstance(propertyName);

		setMaxConnections(poolSizeProperty.getInteger(maxConnections));

		poolSizeProperty.addCallback(() -> setMaxConnections(poolSizeProperty.getInteger(maxConnections)));
	}

	protected void setMaxConnections(int max) {
		int diff = max - getMaxConnections();
		incrementMaxConnections(diff);
	}
}
