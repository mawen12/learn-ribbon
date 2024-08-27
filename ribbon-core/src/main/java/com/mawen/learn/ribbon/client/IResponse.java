package com.mawen.learn.ribbon.client;

import java.net.URI;
import java.util.Map;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public interface IResponse {

	Object getPayload() throws ClientException;

	boolean hasPayload();

	boolean isSuccess();

	URI getRequestedURI();

	Map<String, ?> getHeaders();
}
