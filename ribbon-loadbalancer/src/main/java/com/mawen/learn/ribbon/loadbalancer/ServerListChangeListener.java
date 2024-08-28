package com.mawen.learn.ribbon.loadbalancer;

import java.util.List;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
public interface ServerListChangeListener {

	void serverListChanged(List<Server> oldList, List<Server> newList);
}
