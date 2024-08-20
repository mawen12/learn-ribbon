package com.mawen.learn.ribbon.loadbalancer;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class RetryRule implements IRule {

	private IRule subRule = new RoundRobinRule();

	private long maxRetryMillis = 500;

	public RetryRule() {

	}

	public RetryRule(IRule subRule) {
		this.subRule = subRule != null ? subRule : new RoundRobinRule();
	}

	public IRule getSubRule() {
		return subRule;
	}

	public void setSubRule(IRule subRule) {
		this.subRule = subRule != null ? subRule : new RoundRobinRule();
	}

	public long getMaxRetryMillis() {
		return maxRetryMillis;
	}

	public void setMaxRetryMillis(long maxRetryMillis) {
		if (maxRetryMillis > 0) {
			this.maxRetryMillis = maxRetryMillis;
		}
		else {
			this.maxRetryMillis = 500;
		}
	}

	@Override
	public Server choose(BaseLoadBalancer lb, Object key) {

		long requestTime = System.currentTimeMillis();
		long deadline = requestTime + maxRetryMillis;

		Server answer = subRule.choose(lb, key);

		if ((answer == null || !answer.isAlive()) && (System.currentTimeMillis() < deadline)) {
			InterruptTask task = new InterruptTask(deadline - System.currentTimeMillis());

			while (!Thread.interrupted()) {
				answer = subRule.choose(lb, key);

				if ((answer == null || !answer.isAlive()) && (System.currentTimeMillis() < deadline)) {
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
		else {
			return answer;
		}
	}
}
