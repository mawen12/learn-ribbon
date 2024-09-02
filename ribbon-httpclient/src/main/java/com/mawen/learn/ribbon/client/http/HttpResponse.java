package com.mawen.learn.ribbon.client.http;

import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.mawen.learn.ribbon.client.IResponse;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public interface HttpResponse  extends IResponse, Closeable {

	int getStatus();

	String getStatusLine();

	Map<String, Collection<String>> getHeaders();

	HttpHeaders getHttpHeaders();

	void close();

	InputStream getInputStream();

	boolean hasEntity();

	<T> T getEntity(Class<T> type) throws Exception;

	<T> T getEntity(Type type) throws Exception;

	<T> T getEntity(TypeToken<T> type) throws Exception;

}
