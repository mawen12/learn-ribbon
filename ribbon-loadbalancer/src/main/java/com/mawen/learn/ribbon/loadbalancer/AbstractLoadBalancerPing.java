package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.client.IClientConfigAware;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Setter
@Getter
public abstract class AbstractLoadBalancerPing implements IPing, IClientConfigAware {

	AbstractLoadBalancer lb;

	@Override
	public boolean isAlive(Server server) {
		return true;
	}
}
