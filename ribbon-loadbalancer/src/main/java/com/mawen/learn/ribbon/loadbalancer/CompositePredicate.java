package com.mawen.learn.ribbon.loadbalancer;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class CompositePredicate extends AbstractServerPredicate{

	private AbstractServerPredicate delegate;

	private List<AbstractServerPredicate> fallbacks = Lists.newArrayList();

	private int minimalFilteredServers = 1;

	private float minimalFilteredPercentage = 0;

	@Override
	public boolean apply(@Nullable PredicateKey predicateKey) {
		return delegate.apply(predicateKey);
	}

	public static class Builder {

		private CompositePredicate toBuild;

		Builder(AbstractServerPredicate primaryPredicate) {
			toBuild = new CompositePredicate();
			toBuild.delegate = primaryPredicate;
		}

		Builder(AbstractServerPredicate... primaryPredicates) {
			toBuild = new CompositePredicate();
			Predicate<PredicateKey> chain = Predicates.and(primaryPredicates);
			toBuild.delegate = AbstractServerPredicate.ofKeyPredicate(chain);
		}

		public Builder addFallbackPredicate(AbstractServerPredicate fallback) {
			toBuild.fallbacks.add(fallback);
			return this;
		}

		public Builder setFallbackThresholdAsMinimalFilteredNumberOfServers(int number) {
			toBuild.minimalFilteredServers = number;
			return this;
		}

		public Builder setFallbackPercentageAsMinimalFilteredPercentage(float percentage) {
			toBuild.minimalFilteredPercentage = percentage;
			return this;
		}

		public CompositePredicate build() {
			return toBuild;
		}
	}

	public static Builder withPredicates(AbstractServerPredicate... primaryPredicates) {
		return new Builder(primaryPredicates);
	}

	public static Builder withPredicate(AbstractServerPredicate primaryPredicate) {
		return new Builder(primaryPredicate);
	}

	@Override
	public List<Server> getEligibleServers(List<Server> servers, Object loadBalancerKey) {
		List<Server> result = super.getEligibleServers(servers, loadBalancerKey);
		Iterator<AbstractServerPredicate> i = fallbacks.iterator();
		while (!(result.size() >= minimalFilteredServers && result.size() > (int) (servers.size() * minimalFilteredPercentage)) && i.hasNext()) {
			AbstractServerPredicate predicate = i.next();
			result = predicate.getEligibleServers(servers, loadBalancerKey);
		}
		return result;
	}
}
