package com.mawen.learn.ribbon.loadbalancer;

import java.io.IOException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class PingUrl implements IPing{

	String pingAppendString = "";

	boolean isSecure;

	String expectedContent;

	public PingUrl(boolean isSecure, String pingAppendString) {
		this.isSecure = isSecure;
		this.pingAppendString = pingAppendString != null ? pingAppendString : "";
	}

	public void setPingAppendString(String pingAppendString) {
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

		urlStr += server.getId();
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
}
