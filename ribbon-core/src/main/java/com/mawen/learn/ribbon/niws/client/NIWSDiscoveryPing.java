package com.mawen.learn.ribbon.niws.client;

import com.mawen.learn.ribbon.loadbalancer.AbstractNIWSLoadBalancerPing;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class NIWSDiscoveryPing extends AbstractNIWSLoadBalancerPing {

	private BaseLoadBalancer lb;

	@Override
	public void initWithNiwsConfig(NiwsClientConfig niwsClientConfig) {

	}
}
