package com.mawen.learn.ribbon.client;

import java.net.URI;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequest implements Cloneable{

	protected URI uri;
	protected Object loadBalancerKey;
	protected Boolean isRetriable;
	protected IClientConfig overrideConfig;

	public ClientRequest(URI uri) {
		this.uri = uri;
	}

	public ClientRequest(URI uri, Object loadBalancerKey, Boolean isRetriable) {
		this.uri = uri;
		this.loadBalancerKey = loadBalancerKey;
		this.isRetriable = isRetriable;
	}

	public ClientRequest(ClientRequest request) {
		this.uri = request.uri;
		this.loadBalancerKey = request.loadBalancerKey;
		this.isRetriable = request.isRetriable;
		this.overrideConfig = request.overrideConfig;
	}

	public ClientRequest replaceUri(URI newURI) {
		ClientRequest req;
		try {
			req = (ClientRequest) this.clone();
		}
		catch (CloneNotSupportedException e) {
			req = new ClientRequest(this);
		}
		req.uri = newURI;
		return req;
	}
}
