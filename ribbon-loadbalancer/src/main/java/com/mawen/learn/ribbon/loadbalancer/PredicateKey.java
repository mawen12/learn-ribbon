package com.mawen.learn.ribbon.loadbalancer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Getter
@AllArgsConstructor
public class PredicateKey {

	private Object loadBalancerKey;

	private Server server;

	public PredicateKey(Server server) {
		this(null, server);
	}
}
