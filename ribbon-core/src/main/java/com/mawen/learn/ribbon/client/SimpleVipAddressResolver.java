package com.mawen.learn.ribbon.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public class SimpleVipAddressResolver implements VipAddressResolver{

	private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

	@Override
	public String resolve(String vipAddressMacro, IClientConfig niwsClientConfig) {
		if (vipAddressMacro == null || vipAddressMacro.length() == 0) {
			return vipAddressMacro;
		}
		return replaceMacrosFromConfig(vipAddressMacro);
	}

	private static String replaceMacrosFromConfig(String macro) {
		String result = macro;
		Matcher matcher = VAR_PATTERN.matcher(macro);
		while (matcher.find()) {
			String key = matcher.group(1);
			String value = ConfigurationManager.getConfigInstance().getString(key);
			if (value != null) {
				result = result.replaceAll("\\$\\{" + key + "\\}", value);
				matcher = VAR_PATTERN.matcher(result);
			}
		}
		return result.trim();
	}
}
