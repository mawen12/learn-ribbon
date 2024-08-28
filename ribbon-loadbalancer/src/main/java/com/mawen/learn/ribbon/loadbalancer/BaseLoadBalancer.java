package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.ImmutableList;
import com.mawen.learn.ribbon.client.ClientFactory;
import com.mawen.learn.ribbon.client.IClientConfigAware;
import com.mawen.learn.ribbon.client.PrimeConnections;
import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import com.netflix.util.concurrent.ShutdownEnabledTimer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class BaseLoadBalancer extends AbstractLoadBalancer implements PrimeConnections.PrimeConnectionListener, IClientConfigAware {

	private static final IRule DEFAULT_RULE = new RoundRobinRule();
	private static final String DEFAULT_NAME = "default";
	private static final String PREFIX = "LoadBalancer_";

	protected IRule rule = DEFAULT_RULE;
	protected IPing ping;

	@Monitor(name = PREFIX + "AllServerList", type = DataSourceType.INFORMATIONAL)
	protected volatile List<Server> allServerList = Collections.synchronizedList(new ArrayList<Server>());

	@Monitor(name = PREFIX + "UpServerList", type = DataSourceType.INFORMATIONAL)
	protected volatile List<Server> upServerList = Collections.synchronizedList(new ArrayList<>());

	protected ReadWriteLock allServerLock = new ReentrantReadWriteLock();

	protected ReadWriteLock upServerLock = new ReentrantReadWriteLock();

	protected String name = DEFAULT_NAME;

	protected Timer lbTimer;

	protected int pingIntervalSeconds = 10;

	protected int maxTotalPingTimeSeconds = 5;

	protected Comparator<Server> serverComparator = new ServerComparator();

	protected AtomicBoolean pingInProgress = new AtomicBoolean(false);

	protected LoadBalancerStats lbStats;

	private volatile Counter counter = Monitors.newCounter("LoadBalancer_ChooseServer");

	private PrimeConnections primeConnections;

	private volatile boolean enablePrimingConnections = false;

	private IClientConfig config;

	private List<ServerListChangeListener> changeListeners = new CopyOnWriteArrayList<>();

	public BaseLoadBalancer() {
		this.name = DEFAULT_NAME;
		this.ping = null;
		setRule(null);
		setupPingTask();
		lbStats = new LoadBalancerStats(DEFAULT_NAME);
	}

	public BaseLoadBalancer(String lbName, IRule rule, LoadBalancerStats lbStats) {
		this(lbName, rule, lbStats, null);
	}

	public BaseLoadBalancer(IPing ping, IRule rule) {
		this(DEFAULT_NAME, rule, new LoadBalancerStats(DEFAULT_NAME), ping);
	}

	public BaseLoadBalancer(String name, IRule rule, LoadBalancerStats lbStats, IPing ping) {
		if (log.isDebugEnabled()) {
			log.debug("LoadBalancer: initialized");
		}

		this.name = name;
		this.ping = ping;
		setRule(rule);
		setupPingTask();
		this.lbStats = lbStats;
		init();
	}

	public BaseLoadBalancer(IClientConfig config) {
		initWithNIWSConfig(config);
	}

	public BaseLoadBalancer(IClientConfig config, IRule rule, IPing ping) {
		initWithConfig(config, rule, ping);
	}

	void initWithConfig(IClientConfig config, IRule rule, IPing ping) {
		this.config = config;
		String clientName = config.getClientName();
		this.name = clientName;
		int pingIntervalTime = Integer.parseInt("" + config.getProperty(CommonClientConfigKey.NFLoadBalancerPingInterval, Integer.parseInt("30")));
		int maxTotalPingTime = Integer.parseInt("" + config.getProperty(CommonClientConfigKey.NFLoadBalancerMaxTotalPingTime, Integer.parseInt("2")));

		setPingInterval(pingIntervalTime);
		setMaxTotalPingTime(maxTotalPingTime);

		setRule(rule);
		setPing(ping);
		setLoadBalancerStats(new LoadBalancerStats(clientName));
		rule.setLoadBalancer(this);
		if (ping instanceof AbstractLoadBalancerPing) {
			((AbstractLoadBalancerPing) ping).setLoadBalancer(this);
		}
		log.info("Client: {} instantiated a LoadBalancer: {}.", clientName, this.toString());

		boolean enablePrimeConnections = config.get(CommonClientConfigKey.EnablePrimeConnections, DefaultClientConfigImpl.DEFAULT_ENABLE_PRIME_CONNECTIONS);

		if (enablePrimeConnections) {
			this.setEnablePrimingConnections(true);
			PrimeConnections primeConnections = new PrimeConnections(this.getName(), config);
			this.setPrimeConnections(primeConnections);
		}

		init();
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		String ruleClassName = (String) clientConfig.getProperty(CommonClientConfigKey.NFLoadBalancerRuleClassName);
		String pingClassName = (String) clientConfig.getProperty(CommonClientConfigKey.NFLoadBalancerPingClassName);

		IRule rule;
		IPing ping;

		try {
			rule = (IRule) ClientFactory.instantiateInstanceWithClientConfig(ruleClassName, clientConfig);
			ping = (IPing) ClientFactory.instantiateInstanceWithClientConfig(pingClassName, clientConfig);
		}
		catch (Exception e) {
			throw new RuntimeException("Error initializing load balancer", e);
		}

		initWithConfig(clientConfig, rule, ping);
	}

	public void addServerListChangeListener(ServerListChangeListener listener) {
		changeListeners.add(listener);
	}

	public void removeServerListChangeListener(ServerListChangeListener listener) {
		changeListeners.remove(listener);
	}

	public IClientConfig getClientConfig() {
		return config;
	}

	private boolean canSkipPing() {
		if (ping == null || ping.getClass().getName().equals(DummyPing.class.getName())) {
			return true;
		}
		else {
			return false;
		}
	}

	private void setupPingTask() {
		if (canSkipPing()) {
			return;
		}
		if (lbTimer != null) {
			lbTimer.cancel();
		}
		lbTimer = new ShutdownEnabledTimer("NFLoadBalancer-PingTimer-" + name, true);
		lbTimer.schedule(new PingTask(), 0, pingIntervalSeconds * 1000);
		forceQuickPing();
	}

	void setName(String name) {
		this.name = name;
		if (this.lbStats == null) {
			lbStats = new LoadBalancerStats(name);
		}
		else {
			lbStats.setName(name);
		}
	}

	public String getName() {
		return name;
	}

	public void setLoadBalancerStats(LoadBalancerStats lbStats) {
		this.lbStats = lbStats;
	}

	public Lock lockAllServerList(boolean write) {
		Lock aproposLock = write ? allServerLock.writeLock() : allServerLock.readLock();
		aproposLock.lock();
		return aproposLock;
	}

	public Lock lockUpServerList(boolean write) {
		Lock aproposLock = write ? upServerLock.writeLock() : upServerLock.readLock();
		aproposLock.lock();
		return aproposLock;
	}

	public void setPingInterval(int pingIntervalSeconds) {
		if (pingIntervalSeconds < 1) {
			return;
		}

		this.pingIntervalSeconds = pingIntervalSeconds;
		if (log.isDebugEnabled()) {
			log.debug("LoadBalancer: pingIntervalSeconds set to {}", pingIntervalSeconds);
		}

		setupPingTask();
	}

	public void setMaxTotalPingTime(int maxTotalPingTimeSeconds) {
		if (maxTotalPingTimeSeconds < 1) {
			return;
		}

		this.maxTotalPingTimeSeconds = maxTotalPingTimeSeconds;
		if (log.isDebugEnabled()) {
			log.debug("LoadBalancer: maxTotalPingTime set to {}", maxTotalPingTimeSeconds);
		}
	}

	public int getMaxTotalPingTime() {
		return maxTotalPingTimeSeconds;
	}

	public IRule getRule() {
		return rule;
	}

	public IPing getPing() {
		return ping;
	}

	public boolean isPingInProgress() {
		return pingInProgress.get();
	}

	public void setPing(IPing ping) {
		if (ping != null) {
			if (!ping.equals(this.ping)) {
				this.ping = ping;
				setupPingTask();
			}
		}
		else {
			this.ping = null;
			lbTimer.cancel();
		}
	}

	public void setRule(IRule rule) {
		if (rule != null) {
			this.rule = rule;
		}
		else {
			this.rule = new RoundRobinRule();
		}
		if (this.rule.getLoadBalancer() != this) {
			this.rule.setLoadBalancer(this);
		}
	}

	public int getServerCount(boolean onlyAvailable) {
		if (onlyAvailable) {
			return upServerList.size();
		}
		else {
			return allServerList.size();
		}
	}

	public void addServer(Server newServer) {
		if (newServer != null) {
			try {
				List<Server> newList = new ArrayList<>();
				newList.addAll(allServerList);
				newList.add(newServer);
				setServersList(newList);
			}
			catch (Exception e) {
				log.error("Exception while adding a newServer", e);
			}
		}
	}

	@Override
	public void primeCompleted(Server s, Throwable exception) {
		s.setReadyToServe(true);
	}

	@Override
	public List<Server> getServerList(ServerGroup serverGroup) {
		switch (serverGroup) {
			case ALL:
				return allServerList;
			case STATUS_UP:
				return upServerList;
			case STATUS_NOT_UP:
				List<Server> notAvailableServers = new ArrayList<>(allServerList);
				List<Server> upServers = new ArrayList<>(upServerList);
				notAvailableServers.removeAll(upServers);
				return notAvailableServers;
		}

		return new ArrayList<>();
	}

	public void cancelPingTask() {
		if (lbTimer != null) {
			lbTimer.cancel();
		}
	}

	@Override
	public LoadBalancerStats getLoadBalancerStats() {
		return lbStats;
	}

	@Override
	public void addServers(List<Server> newServers) {
		if (newServers != null && !newServers.isEmpty()) {
			try {
				List<Server> newList = new ArrayList<>();
				newList.addAll(allServerList);
				newList.addAll(newServers);
				setServersList(newList);
			}
			catch (Exception e) {
				log.error("Exception while adding Servers", e);
			}
		}
	}

	void addServers(Object[] newServers) {
		if (newServers != null && newServers.length > 0) {
			try {
				List<Server> newList = new ArrayList<>();
				newList.addAll(allServerList);

				for (Object server : newServers) {
					if (server != null) {
						if (server instanceof String) {
							server = new Server((String) server);
						}
						if (server instanceof Server) {
							newList.add((Server) server);
						}
					}
				}
				setServersList(newList);
			}
			catch (Exception e) {
				log.error("Exception while adding Servers", e);
			}
		}
	}

	public void setServersList(List serversList) {
		Lock writeLock = allServerLock.writeLock();
		if (log.isDebugEnabled()) {
			log.debug("LoadBalancer: clearing server list (SET op)");
		}
		List<Server> newServers = new ArrayList<>();
		writeLock.lock();
		try {
			List<Server> allServers = new ArrayList<>();
			for (Object server : serversList) {
				if (server == null) {
					continue;
				}

				if (server instanceof String) {
					server = new Server((String) server);
				}

				if (server instanceof Server) {
					if (log.isDebugEnabled()) {
						log.debug("LoadBalancer: addServer [" + ((Server) server).getId() + "]");
					}
					allServers.add((Server) server);
				}
				else {
					throw new IllegalArgumentException("Type String or Server expected, instead found: " + server.getClass());
				}
			}

			boolean listChanged = false;
			if (!allServerList.equals(allServers)) {
				listChanged = true;
				if (changeListeners != null && changeListeners.size() > 0) {
					List<Server> oldList = ImmutableList.copyOf(allServerList);
					List<Server> newList = ImmutableList.copyOf(allServers);
					for (ServerListChangeListener l : changeListeners) {
						try {
							l.serverListChanged(oldList, newList);
						}
						catch (Throwable e) {
							log.error("Error invoking server list change listener", e);
						}
					}
				}
			}

			if (isEnablePrimingConnections()) {
				for (Server server : allServers) {
					if (!allServerList.contains(server)) {
						server.setReadyToServe(false);
						newServers.add(server);
					}
				}

				if (primeConnections != null) {
					primeConnections.primeConnectionsAsync(newServers, this);
				}
			}

			allServerList = allServers;
			if (canSkipPing()) {
				for (Server s : allServerList) {
					s.setAliveFlag(true);
				}
				upServerList = allServerList;
			}
			else if (listChanged) {
				forceQuickPing();
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	void setServers(String srvString) {
		if (srvString != null) {
			try {
				String[] serverAddr = srvString.split(",");
				List<Server> newList = new ArrayList<>();

				for (String serverStr : serverAddr) {
					if (serverStr != null) {
						serverStr = serverStr.trim();
						if (serverStr.length() > 0) {
							Server server = new Server(serverStr);
							newList.add(server);
						}
					}
				}

				setServersList(newList);
			}
			catch (Exception e) {
				log.error("Exception while adding Servers", e);
			}
		}
	}

	public Server getServerByIndex(int index, boolean availableOnly) {
		try {
			return availableOnly ? upServerList.get(index) : allServerList.get(index);
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public Server chooseServer(Object key) {
		if (counter == null) {
			counter = createCounter();
		}
		counter.increment();

		if (rule == null) {
			return null;
		}
		else {
			try {
				return rule.choose(key);
			}
			catch (Throwable e) {
				return null;
			}
		}
	}

	@Override
	public void markServerDown(Server server) {
		if (server == null) {
			return;
		}

		if (!server.isAlive()) {
			return;
		}

		log.error("LoadBalancer: markServerDown called on [{}]", server.getId());
		server.setAliveFlag(false);
	}

	public void markServerDown(String id) {
		boolean triggered = false;

		id = Server.normalized(id);
		if (id == null) {
			return;
		}

		Lock writeLock = upServerLock.writeLock();
		try {
			for (Server server : upServerList) {
				if (server.isAlive() && server.getId().equals(id)) {
					triggered = true;
					server.setAliveFlag(false);
				}
			}

			if (triggered) {
				log.error("LoadBalancer: markServerDown called on [{}]", id);
			}
		}
		finally {
			try {
				writeLock.unlock();
			}
			catch (Exception e) {}
		}
	}

	public void forceQuickPing() {
		if (canSkipPing()) {
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug("LoadBalancer: forceQuickPing invoked");
		}

		Pinger ping = new Pinger();
		try {
			ping.runPinger();
		}
		catch (Throwable t) {
			log.error("Throwable caught while running forceQuickPing() for {}", name, t);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{NFLoadBalancer:name=").append(this.getName())
				.append(",current list of Servers=").append(this.allServerList)
				.append(",Load balancer stats=")
				.append(this.lbStats.toString()).append("}");
		return sb.toString();
	}

	protected void init() {
		Monitors.registerObject("LoadBalancer_" + name, this);
		Monitors.registerObject("Rule_" + name, this.getRule());
		if (enablePrimingConnections && primeConnections != null) {
			primeConnections.primeConnections(getServerList(true));
		}
	}

	public final PrimeConnections getPrimeConnections() {
		return primeConnections;
	}

	public final void setPrimeConnections(PrimeConnections primeConnections) {
		this.primeConnections = primeConnections;
	}

	@Override
	public List<Server> getServerList(boolean availableOnly) {
		return availableOnly ? Collections.unmodifiableList(upServerList) : Collections.unmodifiableList(allServerList);
	}

	public String choose(Object key) {
		if (rule == null) {
			return null;
		}
		else {
			try {
				Server srv = rule.choose(key);
				return srv != null ? srv.getId() : null;
			}
			catch (Throwable e) {
				return null;
			}
		}
	}

	private final Counter createCounter() {
		return Monitors.newCounter("LoadBalancer_ChooseServer");
	}

	public boolean isEnablePrimingConnections() {
		return enablePrimingConnections;
	}

	public final void setEnablePrimingConnections(boolean enablePrimingConnections) {
		this.enablePrimingConnections = enablePrimingConnections;
	}

	public void shutdown() {
		cancelPingTask();
		if (primeConnections != null) {
			primeConnections.shutdown();
		}

		Monitors.unregisterObject("LoadBalancer_" + name, this);
		Monitors.unregisterObject("Rule_" + name, this.getRule());
	}

	class PingTask extends TimerTask {

		@Override
		public void run() {
			Pinger ping = new Pinger();
			try {
				ping.runPinger();
			}
			catch (Throwable e) {
				log.error("Throwable caugt while running extends for {}", name, e);
			}
		}
	}

	class Pinger {

		public void runPinger() {

			if (pingInProgress.get()) {
				return;
			}
			else {
				pingInProgress.set(true);
			}

			Object[] allServers = null;
			boolean[] results = null;

			Lock allLock = null;
			Lock upLock = null;

			try {
				allLock = allServerLock.readLock();
				allLock.lock();
				allServers = allServerList.toArray();
				allLock.unlock();

				int numCandidates = allServers.length;
				results = new boolean[numCandidates];

				if (log.isDebugEnabled()) {
					log.debug("LoadBalancer: PingTask executing: [{}] servers configured", numCandidates);
				}

				for (int i = 0; i < numCandidates; i++) {
					results[i] = false;
					try {
						if (ping != null) {
							results[i] = ping.isAlive((Server) allServers[i]);
						}
					}
					catch (Throwable e) {
						log.error("Exception while pinging Server: {}", allServers[i], e);
					}
				}

				List<Server> newUpList = new ArrayList<>();

				for (int i = 0; i < numCandidates; i++) {
					boolean isAlive = results[i];
					Server svr = (Server) allServers[i];
					boolean oldIsAlive = svr.isAlive();

					svr.setAliveFlag(isAlive);

					if(oldIsAlive != isAlive && log.isDebugEnabled()) {
						log.debug("LoadBalancer: Server [{}] status changed to {}", svr.getId(), (isAlive ? "ALIVE" : "DEAD"));
					}

					if (isAlive) {
						newUpList.add(svr);
					}
				}

				upLock = upServerLock.writeLock();
				upLock.lock();
				upServerList = newUpList;
				upLock.unlock();
			}
			catch (Throwable e) {
				log.error("Throwable caught while running the Pinger-{}", name, e);
			}
			finally {
				pingInProgress.set(false);
			}
		}

	}
}
