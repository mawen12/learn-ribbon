package com.mawen.learn.ribbon.loadbalancer;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.stats.distribution.DataDistribution;
import com.netflix.stats.distribution.DataPublisher;
import com.netflix.stats.distribution.Distribution;
import com.netflix.util.MeasuredRate;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Setter
@Getter
public class ServerStats {

	private static final int DEFAULT_PUBLISH_INTERVAL = 60 * 1000;

	private static final int DEFAULT_BUFFER_SIZE = 60 * 1000;

	private static final DynamicIntProperty activeRequestsCountTimeout = DynamicPropertyFactory.getInstance()
			.getIntProperty("niws.loadbalancer.serverStats.activeRequestsCount.effectiveWindowSeconds", 60 * 10);

	private static final double[] PERCENTS = makePercentValues();

	private final DynamicIntProperty connectionFailureThreshold;

	private final DynamicIntProperty circuitTrippedTimeoutFactor;

	private final DynamicIntProperty maxCircuitTrippedTimeout;

	private DataDistribution dataDist = new DataDistribution(1, PERCENTS);

	private DataPublisher publisher;

	private final Distribution responseTimeDist = new Distribution();

	int bufferSize = DEFAULT_BUFFER_SIZE;

	int publishInterval = DEFAULT_PUBLISH_INTERVAL;

	long failureCountSlidingWindowInterval = 1000;

	private MeasuredRate serverFailureCounts = new MeasuredRate(failureCountSlidingWindowInterval);

	private MeasuredRate requestCountInWindow = new MeasuredRate(300000L);

	Server server;

	AtomicLong totalRequests = new AtomicLong();

	AtomicInteger successiveConnectionFailureCount = new AtomicInteger(0);

	AtomicInteger activeRequestsCount = new AtomicInteger(0);

	private volatile long lastConnectionFailedTimestamp;

	private volatile long lastActiveRequestsCountChangeTimestamp;

	private AtomicLong totalCircuitBreakerBlackOutPeriod = new AtomicLong(0);

	private volatile long lastAccessedTimestamp;

	private volatile long firstConnectionTimestamp;

	public ServerStats() {
		connectionFailureThreshold = DynamicPropertyFactory.getInstance().getIntProperty(
				"niws.loadbalancer.default.connectionFailureCountThreshold", 3);
		circuitTrippedTimeoutFactor = DynamicPropertyFactory.getInstance().getIntProperty(
				"niws.loadbalancer.default.circuitTripTimeoutFactorSeconds", 10);
		maxCircuitTrippedTimeout = DynamicPropertyFactory.getInstance().getIntProperty(
				"niws.loadbalancer.default.circuitTripMaxTimeoutSeconds", 30);
	}

	public ServerStats(LoadBalancerStats lbStats) {
		connectionFailureThreshold = lbStats.getConnectionFailureCountThreshold();
		circuitTrippedTimeoutFactor = lbStats.getCircuitTrippedTimeoutFactor();
		maxCircuitTrippedTimeout = lbStats.getCircuitTripMaxTimeoutSeconds();
	}

	public void initialize(Server server) {
		serverFailureCounts = new MeasuredRate(failureCountSlidingWindowInterval);
		requestCountInWindow = new MeasuredRate(300000L);
		if (publisher == null) {
			dataDist = new DataDistribution(getBufferSize(), PERCENTS);
			publisher = new DataPublisher(dataDist, getPublishInterval());
			publisher.start();
		}
		this.server = server;
	}

	public void close() {
		if (publisher != null) {
			publisher.stop();
		}
	}

	@Getter
	private enum Percent {
		TEN(10),
		TWENTY_FIVE(25),
		FIFTY(50),
		SEVENTY_FIVE(75),
		NINETY(90),
		NINETY_FIVE(95),
		NINETY_EIGHT(98),
		NINETY_NINE(99),
		NINETY_NINE_POINT_FIVE(99.5);

		private double val;

		Percent(double val) {
			this.val = val;
		}
	}

	private static double[] makePercentValues() {
		return Arrays.stream(Percent.values()).mapToDouble(Percent::getVal).toArray();
	}

	public void addToFailureCount() {
		serverFailureCounts.increment();
	}

	public long getFailureCount() {
		return serverFailureCounts.getCurrentCount();
	}

	public void noteResponseTime(double msecs) {
		dataDist.noteValue(msecs);
		responseTimeDist.noteValue(msecs);
	}

	public void incrementNumRequests() {
		totalRequests.incrementAndGet();
	}

	public void incrementActiveRequestsCount() {
		activeRequestsCount.incrementAndGet();
		requestCountInWindow.increment();
		long currentTime = System.currentTimeMillis();
		lastActiveRequestsCountChangeTimestamp = currentTime;
		lastAccessedTimestamp = currentTime;
		if (firstConnectionTimestamp == 0) {
			firstConnectionTimestamp = currentTime;
		}
	}

	public void decrementActiveRequestsCount() {
		if (activeRequestsCount.decrementAndGet() < 0) {
			activeRequestsCount.set(0);
		}
		lastActiveRequestsCountChangeTimestamp = System.currentTimeMillis();
	}

	public int getActiveRequestsCount() {
		return getActiveRequestsCount(System.currentTimeMillis());
	}

	public int getActiveRequestsCount(long currentTime) {
		int count = activeRequestsCount.get();
		if (count == 0) {
			return 0;
		}
		else if (currentTime - lastActiveRequestsCountChangeTimestamp > activeRequestsCountTimeout.get()) {
			activeRequestsCount.set(0);
			return 0;
		}
		else {
			return count;
		}
	}

	public long getMeasureRequestsCount() {
		return requestCountInWindow.getCount();
	}

	@Monitor(name = "ActiveRequestsCount", type = DataSourceType.GAUGE)
	public int getMonitoredActiveRequestsCount() {
		return activeRequestsCount.get();
	}

	@Monitor(name = "CircuitBreakerTripped", type = DataSourceType.INFORMATIONAL)
	public boolean isCircuitBreakerTripped() {
		return isCircuitBreakerTripped(System.currentTimeMillis());
	}

	public boolean isCircuitBreakerTripped(long currentTime) {
		long circuitBreakerTimeout = getCircuitBreakerTimeout();
		if (circuitBreakerTimeout <= 0) {
			return false;
		}
		return circuitBreakerTimeout > currentTime;
	}

	private long getCircuitBreakerTimeout() {
		long blackOutPeriod = getCircuitBreakerBlackoutPeriod();
		if (blackOutPeriod <= 0) {
			return 0;
		}
		return lastConnectionFailedTimestamp + blackOutPeriod;
	}

	private long getCircuitBreakerBlackoutPeriod() {
		int failureCount = successiveConnectionFailureCount.get();
		int threshold = connectionFailureThreshold.get();
		if (failureCount < threshold) {
			return 0;
		}

		int diff = (failureCount - threshold) > 16 ? 16 : failureCount - threshold;
		int blackOutSeconds = (1 << diff) * circuitTrippedTimeoutFactor.get();
		if (blackOutSeconds > maxCircuitTrippedTimeout.get()) {
			blackOutSeconds = maxCircuitTrippedTimeout.get();
		}
		return blackOutSeconds * 1000L;
	}

	public void incrementSuccessiveConnectionFailureCount() {
		lastConnectionFailedTimestamp = System.currentTimeMillis();
		successiveConnectionFailureCount.incrementAndGet();
		totalCircuitBreakerBlackOutPeriod.addAndGet(getCircuitBreakerBlackoutPeriod());
	}

	public void clearSuccessiveConnectionFailureCount() {
		successiveConnectionFailureCount.set(0);
	}

	@Monitor(name = "SuccessiveConnectionFailureCount", type = DataSourceType.GAUGE)
	public int getSuccessiveConnectionFailureCount() {
		return successiveConnectionFailureCount.get();
	}

	@Monitor(name = "OverallResponseTimeMillisAvg", type = DataSourceType.INFORMATIONAL,
			description = "Average total time for a request, in milliseconds")
	public double getResponseTimeAvg() {
		return responseTimeDist.getMean();
	}

	@Monitor(name = "OverallResponseTimeMillisMax", type = DataSourceType.INFORMATIONAL,
			description = "Max total time for a request, in milliseconds")
	public double getResponseTimeMax() {
		return responseTimeDist.getMaximum();
	}

	@Monitor(name = "OverallResponseTimeMillisMin", type = DataSourceType.INFORMATIONAL,
			description = "Min total time for a request, in milliseconds")
	public double getResponseTimeMin() {
		return responseTimeDist.getMinimum();
	}

	@Monitor(name = "OverallRespoonseTimeMillisStdDev", type = DataSourceType.INFORMATIONAL,
			description = "Standard Deviation in total time to handle a request, in milliseconds")
	public double getResponseTimeStdDev() {
		return responseTimeDist.getStdDev();
	}

	@Monitor(name = "ResponseTimePercentileNumValues", type = DataSourceType.GAUGE,
			description = "The number of data points used to compute the currently reported percentile values")
	public int getResponseTimePercentileNumValues() {
		return dataDist.getSampleSize();
	}

	@Monitor(name = "ResponseTimePercentileWhen", type = DataSourceType.INFORMATIONAL,
			description = "The time the percentile values were computed")
	public String getResponseTimePercentileWhen() {
		return dataDist.getTimestamp();
	}

	@Monitor(name = "ResponseTimePercentileWhenMillis", type = DataSourceType.INFORMATIONAL,
			description = "The time the percentile values where computed in milliseconds since the epoch")
	public long getResponseTimePercentileWhenMillis() {
		return dataDist.getTimestampMillis();
	}

	@Monitor(name = "ResponseTimeMillisAvg", type = DataSourceType.GAUGE,
			description = "Average total time for a request in the recent time slice, in milliseconds")
	public double getResponseTimeAvgRecent() {
		return responseTimeDist.getMean();
	}

	@Monitor(name = "ResponseTimeMillis10Percentile", type = DataSourceType.INFORMATIONAL,
			description = "10th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime10thPercentile() {
		return getResponseTimePercentile(Percent.TEN);
	}

	@Monitor(name = "ResponseTimeMillis25Percentile", type = DataSourceType.INFORMATIONAL,
			description = "25th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime25thPercentile() {
		return getResponseTimePercentile(Percent.TWENTY_FIVE);
	}

	@Monitor(name = "ResponseTimeMillis50Percentile", type = DataSourceType.INFORMATIONAL,
			description = "50th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime50thPercentile() {
		return getResponseTimePercentile(Percent.FIFTY);
	}

	@Monitor(name = "ResponseTimeMillis75Percentile", type = DataSourceType.INFORMATIONAL,
			description = "75th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime75thPercentile() {
		return getResponseTimePercentile(Percent.SEVENTY_FIVE);
	}

	@Monitor(name = "ResponseTimeMillis90Percentile", type = DataSourceType.INFORMATIONAL,
			description = "90th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime90thPercentile() {
		return getResponseTimePercentile(Percent.NINETY);
	}

	@Monitor(name = "ResponseTimeMillis95Percentile", type = DataSourceType.INFORMATIONAL,
			description = "95th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime95thPercentile() {
		return getResponseTimePercentile(Percent.NINETY_FIVE);
	}

	@Monitor(name = "ResponseTimeMillis98Percentile", type = DataSourceType.INFORMATIONAL,
			description = "98th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime98thPercentile() {
		return getResponseTimePercentile(Percent.NINETY_EIGHT);
	}

	@Monitor(name = "ResponseTimeMillis99Percentile", type = DataSourceType.GAUGE,
			description = "99th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime99thPercentile() {
		return getResponseTimePercentile(Percent.NINETY_NINE);
	}

	@Monitor(name = "ResponseTimeMillis99_5Percentile", type = DataSourceType.GAUGE,
			description = "99.5th percentile in total time to handle a request, in milliseconds")
	public double getResponseTime99point5thPercentile() {
		return getResponseTimePercentile(Percent.NINETY_NINE_POINT_FIVE);
	}

	public long getTotalRequestCount() {
		return totalRequests.get();
	}

	private double getResponseTimePercentile(Percent percent) {
		return dataDist.getPercentiles()[percent.ordinal()];
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("[Server:" + server + ";");
		sb.append("\tZone:" + server.getZone() + ";");
		sb.append("\tTotal Requests:" + totalRequests + ";");
		sb.append("\tSuccessive connection failure:" + getSuccessiveConnectionFailureCount() + ";");
		if (isCircuitBreakerTripped()) {
			sb.append("\tBlackout until: " + new Date(getCircuitBreakerTimeout()) + ";");
		}
		sb.append("\tTotal blackout seconds:" + totalCircuitBreakerBlackOutPeriod.get() / 1000 + ";");
		sb.append("\tLast connection made:" + new Date(lastAccessedTimestamp) + ";");
		if (lastConnectionFailedTimestamp > 0) {
			sb.append("\tLast connection failure: " + new Date(lastConnectionFailedTimestamp)  + ";");
		}
		sb.append("\tFirst connection made: " + new Date(firstConnectionTimestamp)  + ";");
		sb.append("\tActive Connections:" + getMonitoredActiveRequestsCount()  + ";");
		sb.append("\ttotal failure count in last (" + failureCountSlidingWindowInterval + ") msecs:" + getFailureCount()  + ";");
		sb.append("\taverage resp time:" + getResponseTimeAvg()  + ";");
		sb.append("\t90 percentile resp time:" + getResponseTime90thPercentile()  + ";");
		sb.append("\t95 percentile resp time:" + getResponseTime95thPercentile()  + ";");
		sb.append("\tmin resp time:" + getResponseTimeMin()  + ";");
		sb.append("\tmax resp time:" + getResponseTimeMax()  + ";");
		sb.append("\tstddev resp time:" + getResponseTimeStdDev());
		sb.append("]\n");

		return sb.toString();
	}
}
