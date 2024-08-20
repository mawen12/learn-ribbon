package com.mawen.learn.ribbon.niws;

import com.mawen.learn.ribbon.niws.client.NiwsClientConfig;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public interface VipAddressResolver {

	String resolve(String vipAddress, NiwsClientConfig niwsClientConfig);
}
