package com.mawen.learn.ribbon.loadbalancer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mawen.learn.ribbon.client.IClientConfigAware;
import com.mawen.learn.ribbon.client.config.DefaultClientConfigImpl;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.netflix.config.DynamicFloatProperty;
import com.netflix.config.DynamicIntProperty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public class ServerListSubsetFilter<T extends Server> extends ZoneAffinityServerListFilter<T> implements IClientConfigAware, Comparator<T> {

	private Random random = new Random();
	private volatile Set<T> currentSubSet = Sets.newHashSet();
	private DynamicIntProperty sizeProp = new DynamicIntProperty(
			DefaultClientConfigImpl.DEFAULT_PROPERTY_NAME_SPACE + ".ServerListSubsetFilter.size", 20);
	private DynamicFloatProperty eliminationPercent = new DynamicFloatProperty(
			DefaultClientConfigImpl.DEFAULT_PROPERTY_NAME_SPACE + ".ServerListSubsetFilter.forceEliminatePercent", 0.1f);
	private DynamicIntProperty eliminationFailureCountThreshold = new DynamicIntProperty(
			DefaultClientConfigImpl.DEFAULT_PROPERTY_NAME_SPACE + ".ServerListSubsetFilter.eliminationFailureThreshold", 0);
	private DynamicIntProperty eliminationConnectionCountThreshold = new DynamicIntProperty(
			DefaultClientConfigImpl.DEFAULT_PROPERTY_NAME_SPACE + ".ServerListSubsetFilter.eliminationConnectionCountThreshold", 0);

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		super.initWithNIWSConfig(clientConfig);
		sizeProp = new DynamicIntProperty(clientConfig.getClientName() + "." + clientConfig.getNameSpace() + ".ServerListSubsetFilter.size", 20);
		eliminationPercent = new DynamicFloatProperty(clientConfig.getClientName() + "." + clientConfig.getNameSpace() + ".ServerListSubsetFilter.forceEliminatePercent", 0.1f);
		eliminationFailureCountThreshold = new DynamicIntProperty(clientConfig.getClientName() + "." + clientConfig.getNameSpace() + ".ServerListSubsetFilter.eliminationFailureThreshold", 0);
		eliminationConnectionCountThreshold = new DynamicIntProperty(clientConfig.getClientName() + "." + clientConfig.getNameSpace() + ".ServerListSubsetFilter.eliminationConnectionCountThreshold", 0);
	}

	@Override
	public List<T> getFilteredListOfServers(List<T> servers) {
		List<T> zoneAffinityFiltered = super.getFilteredListOfServers(servers);
		Set<T> candidates = Sets.newHashSet(zoneAffinityFiltered);
		Set<T> newSubSet = Sets.newHashSet(currentSubSet);
		LoadBalancerStats lbStats = getLoadBalancerStats();
		for (T server : currentSubSet) {
			if (!candidates.contains(server)) {
				newSubSet.remove(server);
			}
			else {
				ServerStats stats = lbStats.getSingleServerStat(server);
				if (stats.getActiveRequestsCount() > eliminationConnectionCountThreshold.get()
						|| stats.getFailureCount() > eliminationFailureCountThreshold.get()) {
					newSubSet.remove(server);
					candidates.remove(server);
				}
			}
		}

		int targetedListSize = sizeProp.get();
		int numEliminated = currentSubSet.size() - newSubSet.size();
		int minElimination = (int) (targetedListSize * eliminationPercent.get());
		int numToForceEliminate = 0;
		if (targetedListSize < newSubSet.size()) {
			numToForceEliminate = newSubSet.size() - targetedListSize;
		}
		else if (minElimination > numEliminated) {
			numToForceEliminate = minElimination - numEliminated;
		}

		if (numToForceEliminate > newSubSet.size()) {
			numToForceEliminate = newSubSet.size();
		}

		if (numToForceEliminate > 0) {
			List<T> sortedSubSet = Lists.newArrayList(newSubSet);
			Collections.sort(sortedSubSet, this);
			List<T> forceEliminated = sortedSubSet.subList(0, numToForceEliminate);
			newSubSet.removeAll(forceEliminated);
			candidates.removeAll(forceEliminated);
		}

		if (newSubSet.size() < targetedListSize) {
			int numToChoose = targetedListSize - newSubSet.size();
			candidates.removeAll(newSubSet);
			if (numToChoose > candidates.size()) {
				candidates = Sets.newHashSet(zoneAffinityFiltered);
				candidates.removeAll(newSubSet);
			}
			List<T> chosen = randomChoose(Lists.newArrayList(candidates), numToChoose);
			for (T server : chosen) {
				newSubSet.add(server);
			}
		}
		currentSubSet = newSubSet;
		return Lists.newArrayList(newSubSet);
	}

	private List<T> randomChoose(List<T> servers, int numToChoose) {
		int size = servers.size();
		if (numToChoose >= size || numToChoose < 0) {
			return servers;
		}

		for (int i = 0; i < numToChoose; i++) {
			int index = random.nextInt(size);
			T tmp = servers.get(index);
			servers.set(index, servers.get(i));
			servers.set(i, tmp);
		}
		return servers.subList(0, numToChoose);
	}

	@Override
	public int compare(T server1, T server2) {
		LoadBalancerStats lbStats = getLoadBalancerStats();
		ServerStats stats1 = lbStats.getSingleServerStat(server1);
		ServerStats stats2 = lbStats.getSingleServerStat(server2);
		int failuresDiff = (int) (stats2.getFailureCount() - stats1.getFailureCount());
		if (failuresDiff != 0) {
			return failuresDiff;
		} else {
			return (stats2.getActiveRequestsCount() - stats1.getActiveRequestsCount());
		}
	}
}
