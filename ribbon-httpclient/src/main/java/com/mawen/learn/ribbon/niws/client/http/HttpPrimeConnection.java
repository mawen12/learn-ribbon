package com.mawen.learn.ribbon.niws.client.http;

import com.mawen.learn.ribbon.client.IPrimeConnection;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.http.HttpResponse;
import com.mawen.learn.ribbon.http4.NFHttpClient;
import com.mawen.learn.ribbon.loadbalancer.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpConnectionParams;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
@Slf4j
public class HttpPrimeConnection implements IPrimeConnection {

	private NFHttpClient client;

	@Override
	public boolean connect(Server server, String uriPath) throws Exception {
		String url = "http://" + server.getHost() + uriPath;
		log.debug("Trying URL: {}", url);
		HttpUriRequest get = new HttpGet(url);
		HttpResponse response = null;
		try {
			response = client.execute(get);
			if (log.isDebugEnabled() && response.getStatusLine() != null) {
				log.debug("Response code: {}", response.getStatusLine().getStatusCode());
			}
		}
		finally {
			get.abort();
		}
		return false;
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		client = NFHttpClientFactory.getNamedNFHttpClient(clientConfig.getClientName() + "-PrimeConnsClient", false);
		HttpConnectionParams.setConnectionTimeout(client.getParams(), 2000);
	}
}
