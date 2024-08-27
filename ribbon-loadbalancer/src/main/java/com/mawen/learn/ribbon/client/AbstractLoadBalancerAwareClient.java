package com.mawen.learn.ribbon.client;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractLoadBalancerAwareClient<S extends ClientRequest, T extends IResponse>
		extends LoadBalancerContext implements IClientConfigAware {



}
