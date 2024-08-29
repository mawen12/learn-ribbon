package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.client.ClientException;
import com.mawen.learn.ribbon.client.ClientFactory;
import com.mawen.learn.ribbon.client.IClientConfigAware;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.IClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractServerList<T extends Server> implements ServerList<T>, IClientConfigAware {

	public AbstractServerListFilter<T> getFilterImpl(IClientConfig niwsClientConfig) throws ClientException {
		try {
			String niwsServerListFilterClassName = niwsClientConfig.getProperty(CommonClientConfigKey.NIWSServerListFilterClassName, ZoneAffinityServerListFilter.class.getName()).toString();

			AbstractServerListFilter<T> abstractServerListFilter = (AbstractServerListFilter<T>) ClientFactory.instantiateInstanceWithClientConfig(niwsServerListFilterClassName, niwsClientConfig);
			return abstractServerListFilter;
		}
		catch (Throwable e) {
			throw new ClientException(ClientException.ErrorType.CONFIGURATION,
					"Unable to get an instance of CommonClientConfigKey.NIWSServerListFilterClassName. Configured class:"
							+ niwsClientConfig.getProperty(CommonClientConfigKey.NIWSServerListFilterClassName), e);
		}
	}
}
