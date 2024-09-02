package com.mawen.learn.ribbon;

import com.mawen.learn.ribbon.client.config.ClientConfigFactory;
import com.mawen.learn.ribbon.client.config.IClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public abstract class ResourceGroup<T extends RequestTemplate<?, ?>> {

	private final String name;
	private final IClientConfig clientConfig;
	private final ClientConfigFactory configFactory;
	private final RibbonTransportFactory transportFactory;

	public static abstract class GroupBuilder<T extends ResourceGroup> {

		public abstract T build();

		public abstract GroupBuilder withClientOptions(ClientOptions options);
	}

	public static abstract class TemplateBuilder<S, R, T extends RequestTemplate<S, R>> {

	}

}
