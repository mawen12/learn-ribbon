package com.mawen.learn.ribbon.loadbalancer;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public class NoOpPing implements IPing{

	@Override
	public boolean isAlive(Server server) {
		return true;
	}
}
