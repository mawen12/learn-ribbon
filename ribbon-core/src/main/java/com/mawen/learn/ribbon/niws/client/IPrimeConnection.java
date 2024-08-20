package com.mawen.learn.ribbon.niws.client;

import com.mawen.learn.ribbon.loadbalancer.Server;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public interface IPrimeConnection extends NiwsClientConfigAware {

	boolean connect(Server server, String uriPath) throws Exception;
}
