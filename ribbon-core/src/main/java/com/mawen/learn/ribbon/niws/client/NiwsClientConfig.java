package com.mawen.learn.ribbon.niws.client;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.mawen.learn.ribbon.loadbalancer.DummyPing;
import com.mawen.learn.ribbon.niws.VipAddressResolver;
import com.netflix.config.AbstractDynamicPropertyListener;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.ExpandedConfigurationListenerAdapter;
import com.netflix.config.util.HttpVerbUriRegexPropertyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class NiwsClientConfig {

	private static final Logger log = LoggerFactory.getLogger(NiwsClientConfig.class);

	public static final Boolean DEFAULT_PRIORITIZE_VIP_ADDRESS_BASED_SERVERS = Boolean.TRUE;

	public static final String DEFAULT_NFLOADBALANCER_PING_CLASSNAME = DummyPing.class.getName();

	public static final String DEFAULT_NFLOADBALANER_RULE_CLASSNAME = ;

	public static final String DEFAULT_NFLOADBALANCER_CLASSNAME = ;

	public static final String DEFAULT_CLIENT_CLASSNAME = ;

	public static final String DEFAULT_VIPADDRESS_RESOLVER_CLASSNAME = ;

	public static final String DEFAULT_PRIME_CONNECTIONS_URI = "/";

	public static final int DEFAULT_MAX_TOTAL_TIME_TO_PRIME_CONNECTIONS = 30 * 1000;

	public static final int DEFAULT_MAX_RETRIES_PER_SERVER_PRIME_CONNECTION = 2;

	public static final Boolean DEFAULT_ENABLE_PRIME_CONNECTIONS = Boolean.FALSE;

	public static final int DEFAULT_MAX_REQUESTS_ALLOWED_PER_WINDOW = Integer.MAX_VALUE;

	public static final int DEFAULT_REQUEST_THROTTLING_WINDOW_IN_MILLIS = 50 * 1000;

	public static final Boolean DEFAULT_ENABLE_REQUEST_THROTTLING = Boolean.FALSE;

	public static final Boolean DEFAULT_ENABLE_GZIP_CONTENT_ENCODING_FILTER = Boolean.FALSE;

	public static final Boolean DEFAULT_CONNECTION_POOL_CLEANER_TASK_ENABLED = Boolean.TRUE;

	public static final Boolean DEFAULT_FOLLOW_REDIRECTS = Boolean.TRUE;

	public static final float DEFAULT_PERCENTAGE_NIWS_EVENT_LOGGED = 0.0f;

	public static final int DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER = 0;

	public static final int DEFAULT_MAX_AUTO_RETRIES = 0;

	public static final int DEFAULT_READ_TIMEOUT = 5000;

	public static final int DEFAULT_CONNECTION_MANAGER_TIMEOUT = 2000;

	public static final int DEFAULT_CONNECT_TIMEOUT = 2000;

	public static final int DEFAULT_MAX_HTTP_CONNECTIONS_PER_HOST = 50;

	public static final int DEFAULT_MAX_TOTAL_HTTP_CONNECTIONS = 200;

	public static final Boolean DEFAULT_ENABLE_NIWSSTATS = Boolean.TRUE;

	public static final Boolean DEFAULT_ENABLE_NIWSERRORSTATS = Boolean.TRUE;

	public static final Boolean DEFAULT_USE_HTTP_CLIENT4 = Boolean.FALSE;

	public static final float DEFAULT_MIN_PRIME_CONNECTIONS_RATIO = 1.0f;

	public static final String DEFAULT_PRIME_CONNECTIONS_CLASS = ;

	public static final String DEFAULT_SERVER_LIST_CLASS = ;

	public static final int DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS = 30 * 1000;

	public static final int DEFAULT_CONNECTION_IDLE_TIME_IN_MSECS = 30 * 1000;

	public static final int DEFAULT_POOL_MAX_THREADS = DEFAULT_MAX_TOTAL_HTTP_CONNECTIONS;

	public static final int DEFAULT_POOL_MIN_THREADS = 1;

	public static final long DEFAULT_POOL_KEEP_ALIVE_TIME = 15 * 60L;

	public static final TimeUnit DEFAULT_POOL_KEEP_ALIVE_TIME_UNITS = TimeUnit.SECONDS;

	public static final Boolean DEFAULT_ENABLE_ZONE_AFFINITY = Boolean.FALSE;

	public static final Boolean DEFAULT_ENABLE_ZONE_EXCLUSIVITY = Boolean.FALSE;

	public static final int DEFAULT_PORT = 7001;

	public static final Boolean DEFAULT_ENABLE_LOADBALANCER = Boolean.TRUE;

	private static final String PROPERTY_NAMESPACE = "niws.client";

	public static final Boolean DEFAULT_OK_TO_RETRY_ON_ALL_OPERATIONS = Boolean.FALSE;

	public static final Boolean DEFAULT_ENABLE_NIWS_EVENT_LOGGING = Boolean.TRUE;

	private static ConcurrentHashMap<String, NiwsClientConfig> namedConfig = new ConcurrentHashMap<>();

	private static ConcurrentHashMap<String, ConcurrentHashMap<String, Map<String, HttpVerbUriRegexPropertyValue>>> dynamicConfigMap = new ConcurrentHashMap<>();

	private static final String[] DYNAMIC_PROPERTY_PREFIX = {"SLA", "NIWSStats", "ResponseCache", "MethodURI"};

	static {
		ConfigurationManager.getConfigInstance().addConfigurationListener(
				new ExpandedConfigurationListenerAdapter(new NiwsConfigListener())
		);
	}

	private Map<String, DynamicStringProperty> dynamicProperties = new ConcurrentHashMap<>();

	private volatile Map<String, Object> properties = new ConcurrentHashMap<>();

	private String clientName;

	private VipAddressResolver resolver;

	private boolean enableDynamicProperties = true;

	public NiwsClientConfig () {
		this.dynamicProperties.clear();
		this.enableDynamicProperties = false;
	}

	public NiwsClientConfig(Map<String, Object> properties) {
		if (properties != null) {
			NiwsClientConfigKey.values
		}
	}




	public static void setProperty(Properties props, String restClientName, String key, String value) {
		props.setProperty(getInstancePropName(restClientName, key), value);
	}

	public static String getInstancePropName(String restClientName, NiwsClientConfigKey configKey) {
		return getInstancePropName(restClientName, configKey.key());
	}

	public static String getInstancePropName(String restClientName, String key) {
		return restClientName + "." + PROPERTY_NAMESPACE + "." + key;
	}

	public static NiwsClientConfig getNamedConfig(String name) {
		NiwsClientConfig config = namedConfig.get(name);
		if (config != null) {
			return config;
		}
		else {
			config = getConfigWithDefaultProperties();
			config.loadProperties(name);
			NiwsClientConfig old = namedConfig.put(name, config);
			if (old != null) {
				config = old;
			}
			return config;
		}
	}

	public enum NiwsClientConfigKey {

		// NIWS RestClient related
		AppName("AppName"),
		Version("Version"),
		Port("Port"),
		SecurePort("SecurePort"),
		VipAddress("VipAddress"),
		DeploymentContextBasedVipAddress("DeploymentContextBasedVipAddress"),
		MaxAutoRetries("MaxAutoRetries"),
		OkToRetryOnAllOperations("OkToRetryOnAllOperations"),
		RequestSpecifyRetryOn("RequestSpecifyRetryOn"),
		ReceiveBufferSize("ReceiveBufferSize"),
		EnableNIWSEventLogging("EnableNIWSEventLogging"),
		PercentageNIWSEventLogged("PercentageNIWSEventLogged"),
		EnableRequestThrottling("EnableRequestThrottling"),
		RequestThrottlingWindowInMSecs("RequestThrottlingWindowInMSecs"),
		MaxRequestsAllowedPerWindow("MaxRequestsAllowedPerWindow"),
		EnablePrimeConnections("EnablePrimeConnections"),
		PrimeConnectionsClassName("PrimeConnectionsClassName"),
		MaxRetriesPerServerPrimeConnection("MaxRetriesPerServerPrimeConnection"),
		MaxTotalTimeToPrimeConnections("MaxTotalTimeToPrimeConnections"),
		MaxPrimeConnectionsRatio("MaxPrimeConnectionsRatio"),
		PrimeConnectionsURI("PrimeConnectionsURI"),
		PoolMaxThreads("PoolMaxThreads"),
		PoolMinThreads("PoolMinThreads"),
		PoolKeepAliveTime("PoolKeepAliveTime"),
		PoolKeepAliveTimeUnits("PoolKeepAliveTimeUnits"),
		SLA("SLA"),
		SLAMinFailureResponseCode("SLAMinFailureResponseCode"),
		EnableNIWSStats("EnableNIWSStats"),
		EnableNIWSErrorStats("EnableNIWSErrorStats"),
		NIWSStats("NIWSStats"),

		//HTTP Client Related
		UseHttpClient4("UseHttpClient4"),
		MaxHttpConnectionsPerHost("MaxHttpConnectionsPerHost"),
		MaxTotalHttpConnections("MaxTotalHttpConnections"),
		IsSecure("IsSecure"),
		GZipPayload("GZipPayload"),
		ConnectTimeout("ConnectTimeout"),
		// TODO start here

		private final String configKey;

		NiwsClientConfigKey(String configKey) {
			this.configKey = configKey;
		}

		public String key() {
			return configKey;
		}
	}

	private static class NiwsConfigListener extends AbstractDynamicPropertyListener {

		private String getClientNameFromConfig(String name) {
			for (String prefix : DYNAMIC_PROPERTY_PREFIX) {
				if (name.contains(PROPERTY_NAMESPACE + "." + prefix)) {
					return name.substring(0, name.indexOf(PROPERTY_NAMESPACE + "." + prefix) - 1);
				}
			}
			return null;
		}

		@Override
		public void handlePropertyEvent(String name, Object value, EventType eventType) {
			try {
				String clientName = getClientNameFromConfig(name);

				if (clientName != null) {
					String niwsPropertyPrefix = clientName + "." + PROPERTY_NAMESPACE;

					for (String prefix : DYNAMIC_PROPERTY_PREFIX) {
						String configPrefix = niwsPropertyPrefix + "." + prefix + ".";

						if (name != null && name.startsWith(configPrefix)) {
							Map<String, HttpVerbUriRegexPropertyValue> aliasRuleMapForClient = dynamicConfigMap.get(prefix).get(clientName);

							if (aliasRuleMapForClient == null) {
								// no map exists so far, create one
								aliasRuleMapForClient = new ConcurrentHashMap<>();
								Map<String, HttpVerbUriRegexPropertyValue> prev = dynamicConfigMap.get(prefix).putIfAbsent(clientName, aliasRuleMapForClient);
								if (prev != null) {
									aliasRuleMapForClient = prev;
								}
							}

							String alias = name.substring(configPrefix.length());
							if (alias != null) {
								alias = alias.trim();
								switch (eventType) {
									case CLEAR:
										aliasRuleMapForClient.remove(alias);
										break;
									case ADD:
									case SET:
										if (value != null) {
											aliasRuleMapForClient.put(alias, HttpVerbUriRegexPropertyValue.getVerbUriRegex(value.toString()));
										}
										break;
								}
							}
						}
					}
				}
			}
			catch (Throwable e) {
				log.warn("Unexpected error when checking for dynamic Rest Client property updates", e);
			}
		}
	}
}
