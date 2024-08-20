package com.mawen.learn.ribbon.niws;

import java.util.regex.Pattern;

import com.mawen.learn.ribbon.niws.client.NiwsClientConfig;
import com.netflix.config.ConfigurationManager;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class SimpleVipAddressResolver implements VipAddressResolver {

	private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

	@Override
	public String resolve(String vipAddressMacro, NiwsClientConfig niwsClientConfig) {
		if (vipAddressMacro == null || vipAddressMacro.length() == 0) {
			return vipAddressMacro;
		}

		if (!VAR_PATTERN.matcher(vipAddressMacro).matches()) {
			return vipAddressMacro;
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String address : vipAddressMacro.split(",")) {
			if (!first) {
				sb.append(",");
			}

			String interpolated = ConfigurationManager.getConfigInstance().getString(address);
			if (interpolated != null) {
				sb.append(interpolated);
			}

			first = false;
		}

		return sb.toString();
	}
}
