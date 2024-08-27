package com.mawen.learn.ribbon.client;

import com.mawen.learn.ribbon.loadbalancer.Server;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public interface IPrimeConnection extends IClientConfigAware {

	boolean connect(Server server, String uriPath) throws Exception;


}
