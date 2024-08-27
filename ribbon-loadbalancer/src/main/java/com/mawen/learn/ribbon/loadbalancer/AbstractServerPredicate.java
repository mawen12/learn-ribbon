package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractServerPredicate implements Predicate<PredicateKey> {

	protected IRule rule;

	private volatile LoadBalancerStats lbStats;

	private final Random random = new Random();

	private final AtomicInteger nextIndex = new AtomicInteger();

	private final Predicate<Server> serverOnlyPredicate = input -> AbstractServerPredicate.this.apply(new PredicateKey(input));

	public static AbstractServerPredicate alwaysTrue() {
		return new AbstractServerPredicate() {
			@Override
			public boolean apply(@Nullable PredicateKey predicateKey) {
				return true;
			}
		};
	}

	public AbstractServerPredicate(IRule rule) {
		this.rule = rule;
	}

	public AbstractServerPredicate(IRule rule, IClientConfig clientConfig) {
		this.rule = rule;
	}

	public AbstractServerPredicate(LoadBalancerStats lbStats, IClientConfig clientConfig) {
		this.lbStats = lbStats;
	}

	protected LoadBalancerStats getLBStats() {
		if (lbStats != null) {
			return lbStats;
		}
		else if (rule != null) {
			ILoadBalancer lb = rule.getLoadBalancer();
			if (lb instanceof AbstractLoadBalancer) {
				LoadBalancerStats stats = ((AbstractLoadBalancer) lb).getLoadBalancerStats();
				setLoadBalancerStats(stats);
				return stats;
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	public void setLoadBalancerStats(LoadBalancerStats lbStats) {
		this.lbStats = lbStats;
	}

	public List<Server> getEligibleServers(List<Server> servers) {
		return getEligibleServers(servers, null);
	}

	public List<Server> getEligibleServers(List<Server> servers, Object loadBalancerKey) {
		if (loadBalancerKey == null) {
			return ImmutableList.copyOf(Iterables.filter(servers, this.getServerOnlyPredicate()));
		}
		else {
			List<Server> results = Lists.newArrayList();
			for (Server server : servers) {
				if (this.apply(new PredicateKey(loadBalancerKey, server))) {
					results.add(server);
				}
			}
			return results;
		}
	}

	public Optional<Server> chooseRoundRobinAfterFiltering(List<Server> servers) {
		List<Server> eligible = getEligibleServers(servers);
		if (eligible.isEmpty()) {
			return Optional.absent();
		}
		return Optional.of(eligible.get(nextIndex.getAndIncrement() % eligible.size()));
	}

	public Optional<Server> chooseRandomlyAfterFiltering(List<Server> servers) {
		List<Server> eligible = getEligibleServers(servers);
		if (eligible.isEmpty()) {
			return Optional.absent();
		}
		return Optional.of(eligible.get(random.nextInt(eligible.size())));
	}

	public Optional<Server> chooseRoundRobinAfterFiltering(List<Server> servers, Object loadBalancerKey) {
		List<Server> eligible = getEligibleServers(servers, loadBalancerKey);
		if (eligible.isEmpty()) {
			return Optional.absent();
		}
		return Optional.of(eligible.get(nextIndex.getAndIncrement() % eligible.size()));
	}

	public static AbstractServerPredicate ofKeyPredicate(final Predicate<PredicateKey> p) {
		return new AbstractServerPredicate() {
			@Override
			public boolean apply(@Nullable PredicateKey input) {
				return p.apply(input);
			}
		};
	}

	public static AbstractServerPredicate ofServerPredicate(final Predicate<Server> p) {
		return new AbstractServerPredicate() {
			@Override
			public boolean apply(@Nullable PredicateKey input) {
				return p.apply(input.getServer());
			}
		};
	}

}