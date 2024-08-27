package com.mawen.learn.ribbon.client.config;

import java.util.Map;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public interface IClientConfig {

	String getClientName();

	String getNameSpace();

	void loadProperties(String clientName);

	void loadDefaultValues();

	Map<String, Object> getProperties();

	void setProperty(IClientConfigKey key, Object value);

	Object getProperty(IClientConfigKey key);

	Object getProperty(IClientConfigKey key, Object defaultVal);

	boolean containsProperty(IClientConfigKey key);

	String resolveDeploymentContextbasedVipAddresses();

	int getPropertyAsInteger(IClientConfigKey key, int defaultValue);

	String getPropertyAsString(IClientConfigKey key, String defaultValue);

	boolean getPropertyAsBoolean(IClientConfigKey key, boolean defaultValue);

	<T> T get(IClientConfigKey<T> key);

	<T> T get(IClientConfigKey<T> key, T defaultValue);

	<T> IClientConfig set(IClientConfigKey<T> key, T value);


	class Builder {

		private IClientConfig config;

		Builder() {
		}

		public static Builder newBuilder() {
			Builder builder = new Builder();
			builder.config = new DefaultClientConfigImpl();
			return builder;
		}


	}

}
