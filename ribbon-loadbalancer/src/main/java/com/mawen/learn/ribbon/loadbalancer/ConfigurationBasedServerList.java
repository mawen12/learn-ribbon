package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.IClientConfig;

/**
 * <clientName>.<namespace>:listOfServers=<comma delimited hostname:port strings>
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class ConfigurationBasedServerList extends AbstractServerList<Server> {

	private IClientConfig clientConfig;

	@Override
	public List<Server> getInitialListOfServers() {
		return getUpdatedListOfServers();
	}

	@Override
	public List<Server> getUpdatedListOfServers() {
		String listOfServers = clientConfig.get(CommonClientConfigKey.ListOfServers);
		return derive(listOfServers);
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		this.clientConfig = clientConfig;
	}

	private List<Server> derive(String value) {
		List<Server> list = new ArrayList<>();
		if (!Strings.isNullOrEmpty(value)) {
			for (String s : value.split(",")) {
				list.add(new Server(s.trim()));
			}
		}
		return list;
	}
}
