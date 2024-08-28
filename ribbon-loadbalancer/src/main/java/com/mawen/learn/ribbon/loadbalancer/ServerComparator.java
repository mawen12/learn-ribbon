package com.mawen.learn.ribbon.loadbalancer;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
public class ServerComparator implements Comparator<Server>, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public int compare(Server o1, Server o2) {
		return o1.getHostPort().compareTo(o2.getId());
	}
}
