package com.mawen.learn.ribbon.loadbalancer;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public interface IRule {

	Server choose(Object key);

	void setLoadBalancer(ILoadBalancer lb);

	ILoadBalancer getLoadBalancer();
}
