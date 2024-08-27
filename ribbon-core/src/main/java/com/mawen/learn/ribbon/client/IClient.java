package com.mawen.learn.ribbon.client;

import com.mawen.learn.ribbon.client.config.IClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public interface IClient<S extends ClientRequest, T extends IResponse> {

	T execute(S request, IClientConfig requestConfig) throws Exception;
}
