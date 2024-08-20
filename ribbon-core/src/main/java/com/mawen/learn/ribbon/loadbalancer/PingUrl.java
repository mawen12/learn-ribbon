package com.mawen.learn.ribbon.loadbalancer;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use the Ping implementation if you want to do a "health check"
 * kind of Ping.
 * <p>
 * This will be a "real" ping. As in a real http/s call is made to this url e.g.
 * <p>
 * Some services/clients choose PingDiscovery - which is quick but is not a real ping.
 * As in - it just asks discovery in-memory cache if the server is present in its Roster.
 * <p>
 * PingUrl on the other hand, makes a real call. This is more expensive - but its the
 * "standard" way most VIPs and other services perform HealthChecks.
 * <p>
 * Choose your Ping based on your needs.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class PingUrl implements IPing{

	private static final Logger log = LoggerFactory.getLogger(PingUrl.class);

	private String pingAppendString = "";

	private boolean isSecure;

	private String expectedContent;

	public PingUrl(){}

	public PingUrl(boolean isSecure, String pingAppendString) {
		this.isSecure = isSecure;
		this.pingAppendString = pingAppendString != null ? pingAppendString : "";
	}

	@Override
	public boolean isAlive(Server server) {
		String urlStr = "";
		if (isSecure) {
			urlStr = "https://";
		}
		else {
			urlStr = "http://";
		}

		urlStr += server.id;
		urlStr += getPingAppendString();

		boolean isAlive = false;

		HttpClient httpClient = new DefaultHttpClient();
		HttpUriRequest getRequest = new HttpGet(urlStr);
		String content = null;
		try {
			HttpResponse response = httpClient.execute(getRequest);
			content = EntityUtils.toString(response.getEntity());
			isAlive = response.getStatusLine().getStatusCode() == 200;
			if (getExpectedContent() != null) {
				log.debug("content: {}", content);
				if (content == null) {
					isAlive = false;
				}
				else {
					if (content.equals(getExpectedContent())) {
						isAlive = true;
					}
					else {
						isAlive = false;
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			getRequest.abort();
		}

		return isAlive;
	}

	public void setPingAppendString(String pingAppendString) {
		this.pingAppendString = pingAppendString;
	}

	public String getPingAppendString() {
		return pingAppendString;
	}

	public boolean isSecure() {
		return isSecure;
	}

	public void setSecure(boolean secure) {
		isSecure = secure;
	}

	public String getExpectedContent() {
		return expectedContent;
	}

	public void setExpectedContent(String expectedContent) {
		this.expectedContent = expectedContent;
	}
}


