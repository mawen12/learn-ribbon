package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public interface ServerListFilter<T extends Server> {

	List<T> getFilteredListOfServers(List<T> servers);
}
