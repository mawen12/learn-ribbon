package com.mawen.learn.ribbon.niws.client.http;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.mawen.learn.ribbon.client.ClientException;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.http.HttpHeaders;
import com.mawen.learn.ribbon.client.http.HttpResponse;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class HttpClientResponse implements HttpResponse {

	private final Multimap<String, String> headers = ArrayListMultimap.create();
	private final ClientResponse response;
	private final HttpHeaders httpHeaders;
	private final URI requestedURI;
	private final IClientConfig overrideConfig;

	public HttpClientResponse(ClientResponse response, URI requestedURI, IClientConfig overrideConfig) {
		this.response = response;
		this.requestedURI = requestedURI;
		this.overrideConfig = overrideConfig;

		for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				headers.putAll(entry.getKey(), entry.getValue());
			}
		}

		httpHeaders = new HttpHeaders() {
			@Override
			public String getFirstValue(String headerName) {
				return response.getHeaders().getFirst(headerName);
			}

			@Override
			public List<String> getAllValues(String headerName) {
				return response.getHeaders().get(headerName);
			}

			@Override
			public List<Map.Entry<String, String>> getAllHeaders() {
				MultivaluedMap<String, String> map = response.getHeaders();
				List<Map.Entry<String, String>> result = Lists.newArrayList();

				for (Map.Entry<String, List<String>> header : map.entrySet()) {
					String name = header.getKey();
					for (String value : header.getValue()) {
						result.add(new AbstractMap.SimpleEntry<>(name, value));
					}
				}
				return result;
			}

			@Override
			public boolean containsHeader(String headerName) {
				return response.getHeaders().containsKey(headerName);
			}
		};
	}

	public InputStream getRawEntity() {
		return response.getEntityInputStream();
	}

	@Override
	public <T> T getEntity(Class<T> type) throws Exception {
		return response.getEntity(type);
	}

	@Override
	public <T> T getEntity(Type type) throws Exception {
		return response.getEntity(new GenericType<T>(type));
	}

	@Override
	public <T> T getEntity(TypeToken<T> type) throws Exception {
		return response.getEntity(new GenericType<>(type.getType()));
	}

	@Override
	public int getStatus() {
		return response.getStatus();
	}

	@Override
	public String getStatusLine() {
		return response.getClientResponseStatus().toString();
	}

	@Override
	public Map<String, Collection<String>> getHeaders() {
		return headers.asMap();
	}

	@Override
	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	@Override
	public void close() {
		response.close();
	}

	@Override
	public InputStream getInputStream() {
		return getRawEntity();
	}

	@Override
	public boolean hasEntity() {
		return response.hasEntity();
	}

	@Override
	public Object getPayload() throws ClientException {
		if (hasEntity()) {
			return getRawEntity();
		}
		return null;
	}

	@Override
	public boolean hasPayload() {
		return hasEntity();
	}

	@Override
	public boolean isSuccess() {
		boolean isSuccess = false;
		ClientResponse.Status status = response != null ? response.getClientResponseStatus() : null;
		isSuccess = status != null ? status.getFamily() == Response.Status.Family.SUCCESSFUL : false;
		return isSuccess;
	}

	@Override
	public URI getRequestedURI() {
		return requestedURI;
	}
}
