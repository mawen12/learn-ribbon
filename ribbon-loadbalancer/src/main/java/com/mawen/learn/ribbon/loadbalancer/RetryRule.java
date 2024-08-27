package com.mawen.learn.ribbon.loadbalancer;

import com.mawen.learn.ribbon.client.config.IClientConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Getter
@NoArgsConstructor
public class RetryRule extends AbstractLoadBalancerRule{

	IRule subRule = new RoundRobinRule();

	long maxRetryMillis = 500;

	public RetryRule(IRule subRule) {
		this.subRule = subRule != null ? subRule : new RoundRobinRule();
	}

	public RetryRule(IRule subRule, long maxRetryMillis) {
		this.subRule = subRule != null ? subRule : new RoundRobinRule();
		this.maxRetryMillis = maxRetryMillis > 0 ? maxRetryMillis : 500;
	}

	public void setSubRule(IRule subRule) {
		this.subRule = subRule != null ? subRule : new RoundRobinRule();
	}

	public void setMaxRetryMillis(long maxRetryMillis) {
		this.maxRetryMillis = maxRetryMillis > 0 ? maxRetryMillis : 500;
	}

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		super.setLoadBalancer(lb);
		subRule.setLoadBalancer(lb);
	}

	@Override
	public Server choose(Object key) {
		return choose(getLoadBalancer(), key);
	}

	public Server choose(ILoadBalancer lb, Object key) {
		long requestTime = System.currentTimeMillis();
		long deadline = requestTime + maxRetryMillis;

		Server answer = subRule.choose(key);

		if ((answer == null || !answer.isAlive()) && System.currentTimeMillis() < deadline) {
			InterruptTask task = new InterruptTask(deadline - System.currentTimeMillis());

			while (!Thread.interrupted()) {
				answer = subRule.choose(key);

				if ((answer == null || !answer.isAlive()) && System.currentTimeMillis() < deadline) {
					Thread.yield();
				}
				else {
					break;
				}
			}

			task.cancel();
		}

		if (answer == null || !answer.isAlive()) {
			return null;
		}
		return answer;
	}

	@Override
	public void initWithNIWSConfig(IClientConfig clientConfig) {
		// do nothing
	}
}
