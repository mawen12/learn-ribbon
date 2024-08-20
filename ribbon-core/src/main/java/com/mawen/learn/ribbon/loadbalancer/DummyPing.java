package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.niws.client.NiwsClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class DummyPing extends AbstractNIWSLoadBalancerPing {

	public DummyPing(){}

	@Override
	public boolean isAlive(Server server) {
		return true;
	}

	@Override
	public void initWithNiwsConfig(NiwsClientConfig niwsClientConfig) {

	}
}
