package com.mawen.learn.ribbon.loadbalancer;

/**
 * No Op Ping
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class NoOpPing implements IPing{

	@Override
	public boolean isAlive(Server server) {
		return true;
	}
}
