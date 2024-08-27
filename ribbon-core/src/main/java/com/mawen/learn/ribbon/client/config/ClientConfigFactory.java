package com.mawen.learn.ribbon.client.config;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public interface ClientConfigFactory {

	IClientConfig newConfig();

	class DefaultClientConfigFactory implements ClientConfigFactory {
		@Override
		public IClientConfig newConfig() {
			return new DefaultClientConfigImpl();
		}
	}

	ClientConfigFactory DEFAULT = new DefaultClientConfigFactory();
}
