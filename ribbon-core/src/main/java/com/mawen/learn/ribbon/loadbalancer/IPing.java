package com.mawen.learn.ribbon.loadbalancer;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public interface IPing {

	boolean isAlive(Server server);
}
