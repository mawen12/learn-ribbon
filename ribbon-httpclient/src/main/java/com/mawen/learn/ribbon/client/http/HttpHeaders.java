package com.mawen.learn.ribbon.client.http;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public interface HttpHeaders {

	String getFirstValue(String headerName);

	List<String> getAllValues(String headerName);

	List<Map.Entry<String ,String>> getAllHeaders();

	boolean containsHeader(String headerName);
}
