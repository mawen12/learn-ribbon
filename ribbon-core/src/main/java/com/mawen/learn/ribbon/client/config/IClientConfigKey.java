package com.mawen.learn.ribbon.client.config;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public interface IClientConfigKey<T> {

	final class Keys extends CommonClientConfigKey {
		private Keys(String configKey) {
			super(configKey);
		}
	}

	String key();

	Class<T> type();
}
