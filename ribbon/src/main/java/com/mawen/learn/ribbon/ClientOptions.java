package com.mawen.learn.ribbon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.config.IClientConfigKey;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public final class ClientOptions {

	public static ClientOptions create() {
		return new ClientOptions();
	}

	public static ClientOptions from(IClientConfig config) {
		ClientOptions options = new ClientOptions();
		for (final IClientConfigKey key : IClientConfigKey.Keys.keys()) {
			Object value = config.get(key);
			if (value != null) {
				options.options.put(key, value);
			}
		}
		return options;
	}

	private final ConcurrentMap<IClientConfigKey<?>, Object> options;

	private ClientOptions() {
		options = new ConcurrentHashMap<>();
	}

	public ClientOptions withDiscoveryServiceIdentifier(String identifier) {
		options.put(IClientConfigKey.Keys.DeploymentContextBasedVipAddress, identifier);
		return this;
	}

	public ClientOptions withConfigurationBasedServerList(String serverList) {
		options.put(IClientConfigKey.Keys.ListOfServers, serverList);
		return this;
	}

	public ClientOptions withMaxAutoRetries(int value) {
		options.put(IClientConfigKey.Keys.MaxAutoRetries, value);
		return this;
	}

	public ClientOptions withMaxAutoRetriesNextServer(int value) {
		options.put(IClientConfigKey.Keys.MaxAutoRetriesNextServer, value);
		return this;
	}

	public ClientOptions withRetryOnAllOperations(boolean value) {
		options.put(IClientConfigKey.Keys.OkToRetryOnAllOperations, value);
		return this;
	}

	public ClientOptions withMaxConnectionsPerHost(int value) {
		options.put(IClientConfigKey.Keys.MaxConnectionsPerHost, value);
		return this;
	}

	public ClientOptions withMaxTotalConnections(int value) {
		options.put(IClientConfigKey.Keys.MaxTotalConnections, value);
		return this;
	}

	public ClientOptions withConnectionTimeout(int value) {
		options.put(IClientConfigKey.Keys.ConnectTimeout, value);
		return this;
	}

	public ClientOptions withReadTimeout(int value) {
		options.put(IClientConfigKey.Keys.ReadTimeout, value);
		return this;
	}

	public ClientOptions withFollowRedirects(boolean value) {
		options.put(IClientConfigKey.Keys.FollowRedirects, value);
		return this;
	}

	public ClientOptions withConnectionPoolIdleEvictTimeMilliseconds(int value) {
		options.put(IClientConfigKey.Keys.ConnIdleEvictTimeMilliSeconds, value);
		return this;
	}

	public ClientOptions withLoadBalancerEnabled(boolean value) {
		options.put(IClientConfigKey.Keys.InitializeNFLoadBalancer, value);
		return this;
	}

	Map<IClientConfigKey<?>, Object> getOptions() {
		return options;
	}
}
