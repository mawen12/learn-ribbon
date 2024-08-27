package com.mawen.learn.ribbon.client.util;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
@Slf4j
public abstract class Resources {

	public static URL getResource(String resourceName) {
		URL url = null;

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		if (loader != null) {
			url = loader.getResource(resourceName);
		}
		if (url == null) {
			url = ClassLoader.getSystemResource(resourceName);
		}
		if (url == null) {
			try {
				resourceName = URLDecoder.decode(resourceName, "UTF-8");
				url = new File(resourceName).toURI().toURL();
			}
			catch (Exception e) {
				log.error("Problem loading resource", e);
			}
		}
		return url;
	}
}
