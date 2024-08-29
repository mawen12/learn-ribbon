package com.mawen.learn.ribbon.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.netflix.servo.monitor.Monitors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class ClientFactory {

	private static Map<String, IClient<?, ?>> simpleClientMap = new ConcurrentHashMap<>();
	private static Map<String, ILoadBalancer> namedLBMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, IClientConfig> namedConfig = new ConcurrentHashMap<>();

	public static synchronized IClient<?, ?> registerClientFromProperties(String restClientName, IClientConfig clientConfig) throws ClientException {
		IClient<?, ?> client = null;
		ILoadBalancer loadBalancer = null;
		if (simpleClientMap.get(restClientName) != null) {
			throw new ClientException(ClientException.ErrorType.GENERAL,
					"A Rest Client with this name is already registered. Please use a different name");
		}

		try {
			String clientClassName = (String) clientConfig.getProperty(CommonClientConfigKey.ClientClassName);
			client = (IClient<?, ?>)instantiateInstanceWithClientConfig(clientClassName, clientConfig);
			boolean initializeNFLoadBalancer = Boolean.parseBoolean(clientConfig.getProperty(CommonClientConfigKey.InitializeNFLoadBalancer, DefaultClientConfigImpl.DEFAULT_ENABLE_LOADBALANCER).toString());
			if (initializeNFLoadBalancer) {
				loadBalancer = registerNamedLoadBalancerFromClientConfig(restClientName, clientConfig);
			}
			if (client instanceof AbstractLoadBalancerAwareClient) {
				((AbstractLoadBalancerAwareClient)client).setLoadBalancer(loadBalancer);
			}
		}
		catch (Throwable e) {
			String message = "Unable to InitializeAndAssociateNFLoadBalancer set for RestClient:" + restClientName;
			log.warn(message, e);
			throw new ClientException(ClientException.ErrorType.CONFIGURATION, message, e);
		}

		simpleClientMap.put(restClientName, client);

		Monitors.registerObject("Client_" + restClientName, client);

		log.info("Client registered: " + client.toString());

		return client;
	}

	public static synchronized IClient getNamedClient(String name, Class<? extends IClientConfig> configClass) {
		if (simpleClientMap.get(name) != null) {
			return simpleClientMap.get(name);
		}

		try {
			return createNamedClient(name, configClass);
		}
		catch (ClientException e) {
			throw new RuntimeException("Unable to create client", e);
		}
	}

	public static synchronized IClient createNamedClient(String name, Class<? extends IClientConfig> configClass) throws ClientException {
		IClientConfig config = getNamedConfig(name, configClass);
		return registerClientFromProperties(name, config);
	}

	public static synchronized ILoadBalancer getNamedLoadBalancer(String name) {
		return getNamedLoadBalancer(name, DefaultClientConfigImpl.class);
	}

	public static synchronized ILoadBalancer getNamedLoadBalancer(String name, Class<? extends IClientConfig> configClass) {
		ILoadBalancer lb = namedLBMap.get(name);
		if (lb != null) {
			return lb;
		}
		else {
			try {
				lb = registerNamedLoadBalancerFromProperties(name, configClass);
			}
			catch (ClientException e) {
				throw new RuntimeException("Unable to create load balancer", e);
			}
			return lb;
		}
	}

	public static ILoadBalancer registerNamedLoadBalancerFromClientConfig(String name, IClientConfig clientConfig) throws ClientException {
		if (namedLBMap.get(name) != null) {
			throw new ClientException("LoadBalancer for name " + name + " already exist");
		}

		ILoadBalancer lb = null;
		try {
			String loadBalancerClassName = (String) clientConfig.getProperty(CommonClientConfigKey.NFLoadBalancerClassName);
			lb = (ILoadBalancer) ClientFactory.instantiateInstanceWithClientConfig(loadBalancerClassName, clientConfig);
			namedLBMap.put(name, lb);
			log.info("Client: {} instantiated a LoadBalancer: {}", name, lb);
			return lb;
		}
		catch (Exception e) {
			throw new ClientException("Unable to instantiate/associate LoadBalancer with Client:" + name, e);
		}
	}

	public static synchronized ILoadBalancer registerNamedLoadBalancerFromProperties(String name, Class<? extends IClientConfig> configClass) throws ClientException {
		if (namedLBMap.get(name) != null) {
			throw new ClientException("LoadBalancer for name " + name + " already exist");
		}
		IClientConfig clientConfig = getNamedConfig(name, configClass);
		return registerNamedLoadBalancerFromClientConfig(name, clientConfig);
	}

	public static Object instantiateInstanceWithClientConfig(String className, IClientConfig clientConfig) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class clazz = Class.forName(className);
		if (IClientConfigAware.class.isAssignableFrom(clazz)) {
			IClientConfigAware object = (IClientConfigAware) clazz.newInstance();
			object.initWithNIWSConfig(clientConfig);
			return object;
		}
		else {
			try {
				if (clazz.getConstructor(IClientConfig.class) != null) {
					return clazz.getConstructor(IClientConfig.class).newInstance(clientConfig);
				}
			}
			catch (Exception e) {

			}
		}
		log.warn("Class {} neither implements IClientConfigAware nor provides a constructor with IClientConfig as the parameter. Only default constructor will be used.");
		return clazz.newInstance();
	}

	public static IClientConfig getNamedConfig(String name) {
		return getNamedConfig(name, DefaultClientConfigImpl.class);
	}

	public static IClientConfig getNamedConfig(String name, Class<? extends IClientConfig> configClass) {
		IClientConfig config = namedConfig.get(name);
		if (config != null) {
			return config;
		}
		else {
			try {
				config = configClass.newInstance();
				config.loadProperties(name);
			}
			catch (Throwable e) {
				log.error("Unable to create client config instance", e);
				return null;
			}

			config.loadProperties(name);
			IClientConfig old = namedConfig.putIfAbsent(name, config);
			if (old != null) {
				return old;
			}
			return config;
		}
	}
}
