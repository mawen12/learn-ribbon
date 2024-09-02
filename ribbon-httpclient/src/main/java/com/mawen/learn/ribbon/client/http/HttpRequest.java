package com.mawen.learn.ribbon.client.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mawen.learn.ribbon.client.ClientRequest;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections.map.CaseInsensitiveMap;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class HttpRequest extends ClientRequest {

	protected CaseInsensitiveMultiMap httpHeaders;
	protected Multimap<String, String> queryParams = ArrayListMultimap.create();
	protected Verb verb;
	private Object entity;

	HttpRequest() {
		this.verb = Verb.GET;
	}

	public Map<String, Collection<String>> getQueryParams() {
		return queryParams.asMap();
	}

	public Verb getVerb() {
		return verb;
	}

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	public Object getEntity() {
		return entity;
	}

	@Override
	public Boolean getIsRetriable() {
		if (this.verb == Verb.GET && isRetriable == null) {
			return true;
		}
		return super.getIsRetriable();
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(HttpRequest toCopy) {
		return new Builder(toCopy);
	}

	@Override
	public ClientRequest replaceUri(URI newURI) {
		return new Builder().uri(newURI)
				.headers(this.httpHeaders)
				.overrideConfig(this.getOverrideConfig())
				.queryParams(this.queryParams)
				.setRetriable(this.getIsRetriable())
				.loadBalancerKey(this.getLoadBalancerKey())
				.verb(this.getVerb())
				.entity(this.entity)
				.build();
	}

	public static class Builder {

		private HttpRequest request = new HttpRequest();

		public Builder() {}

		public Builder(HttpRequest request) {
			this.request = request;
		}

		public Builder uri(URI uri) {
			request.setUri(uri);
			return this;
		}

		public Builder uri(String uri) {
			try {
				request.setUri(new URI(uri));
			}
			catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		public Builder overrideConfig(IClientConfig config) {
			request.setOverrideConfig(config);
			return this;
		}

		Builder queryParams(Multimap<String, String> queryParams) {
			request.queryParams = queryParams;
			return this;
		}

		Builder headers(CaseInsensitiveMultiMap headers) {
			request.httpHeaders = headers;
			return this;
		}

		public Builder setRetriable(boolean retriable) {
			request.setIsRetriable(retriable);
			return this;
		}

		public Builder queryParam(String name, String value) {
			request.queryParams.put(name, value);
			return this;
		}

		public Builder entity(Object entity) {
			request.entity = entity;
			return this;
		}

		public Builder verb(Verb verb) {
			request.verb = verb;
			return this;
		}

		public Builder loadBalancerKey(Object loadBalancerKey) {
			request.setLoadBalancerKey(loadBalancerKey);
			return this;
		}

		public HttpRequest build() {
			return request;
		}
	}

	@Getter
	@AllArgsConstructor
	public enum Verb {
		GET("GET"),
		PUT("PUT"),
		POST("POST"),
		DELETE("DELETE"),
		OPTIONS("OPTIONS"),
		HEAD("HEAD")
		;

		private final String verb;
	}
}
