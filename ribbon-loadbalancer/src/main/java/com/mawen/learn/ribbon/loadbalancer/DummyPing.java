package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import lombok.NoArgsConstructor;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@NoArgsConstructor
public class DummyPing extends AbstractLoadBalancerPing {

	@Override
	public boolean isAlive(Server server) {
		return true;
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		// do nothing
	}
}
