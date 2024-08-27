package com.mawen.learn.ribbon.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mawen.learn.ribbon.client.config.IClientConfigKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Slf4j
public class ResponseTimeWeightedRule extends RoundRobinRule{

	public static final IClientConfigKey<Integer> WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY = WeightedResponseTimeRule.WEIGHT_TASK_TIMER_INTERVAL_CONFIG_KEY;

	public static final int DEFAULT_TIMER_INTERVAL = 30 * 1000;

	private int serverWeightTaskTimerInterval = DEFAULT_TIMER_INTERVAL;

	private volatile List<Double> accumulatedWeights = new ArrayList<>();

	private final Random random = new Random();

	protected Timer serverWeightTimer;

	protected AtomicBoolean serverWeightAssignmentInProgress = new AtomicBoolean(false);

	String name = "unknown";

	public ResponseTimeWeightedRule() {
		super();
	}

	public ResponseTimeWeightedRule(ILoadBalancer lb) {
		super(lb);
	}

	@Override
	public void setLoadBalancer(ILoadBalancer lb) {
		super.setLoadBalancer(lb);
		if (lb instanceof BaseLoadBalancer) {
			name = ((BaseLoadBalancer)lb).getName();
		}
		initialize(lb);
	}
}
