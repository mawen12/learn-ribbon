package com.mawen.learn.ribbon;

import com.mawen.learn.ribbon.client.config.ClientConfigFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/30
 */
public class RibbonTransportFactory {

	public static final RibbonTransportFactory DEFAULT = new DefaultRibbonTransportFactory(ClientConfigFactory.DEFAULT);

	protected final ClientConfigFactory clientConfigFactory;

	public RibbonTransportFactory(ClientConfigFactory clientConfigFactory) {
		this.clientConfigFactory = clientConfigFactory;
	}

	public static class DefaultRibbonTransportFactory extends RibbonTransportFactory {
		public DefaultRibbonTransportFactory(ClientConfigFactory clientConfigFactory) {
			super(clientConfigFactory);
		}
	}
}
