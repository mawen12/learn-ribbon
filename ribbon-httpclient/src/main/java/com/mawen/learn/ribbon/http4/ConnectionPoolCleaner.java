package com.mawen.learn.ribbon.http4;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ClientConnectionManager;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
@Getter
@Setter
@Slf4j
public class ConnectionPoolCleaner {

	String name = "default";
	ClientConnectionManager connMgr;
	ScheduledExecutorService scheduler;

	private DynamicIntProperty connIdleEvictTimeMilliSeconds = DynamicPropertyFactory.getInstance().getIntProperty("default.nfhttpclient.connIdleEvictTimeMilliSeconds",
			NFHttpClientConstants.DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS);

	volatile boolean enableConnectionPoolCleanerTask = false;
	long connectionCleanerTimerDelay = 10;
	long connectionCleanerRepeatInterval = NFHttpClientConstants.DEFAULT_CONNECTION_IDLE_TIMERTASK_REPEAT_IN_MSECS;
	private volatile ScheduledFuture<?> scheduledFuture;

	public ConnectionPoolCleaner(String name, ClientConnectionManager connMgr, ScheduledExecutorService scheduler) {
		this.name = name;
		this.connMgr = connMgr;
		this.scheduler = scheduler;
	}

	public void initTask() {
		if (enableConnectionPoolCleanerTask) {
			scheduledFuture = scheduler.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						if (enableConnectionPoolCleanerTask) {
							log.debug("Connection pool clean up started for client: {}", name);
						}
					}
					catch (Throwable e) {
						log.error("Exception in ConnectionPoolCleanerThread", e);
					}
				}
			}, connectionCleanerTimerDelay, connectionCleanerRepeatInterval, TimeUnit.MILLISECONDS);
			log.info("Initializing ConnectionPoolCleanerTask for NFHttpClient: {}", name);
		}
	}

	void cleanupConnections() {
		connMgr.closeExpiredConnections();
		connMgr.closeIdleConnections(connIdleEvictTimeMilliSeconds.get(), TimeUnit.MILLISECONDS);
	}

	public void shutdown() {
		enableConnectionPoolCleanerTask = false;
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
		}
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("ConnectionPoolCleaner:" + name);
		sb.append(", connIdleEvictTimeMilliSeconds:" + connIdleEvictTimeMilliSeconds.get());
		sb.append(", connectionCleanerTimerDelay:" + connectionCleanerTimerDelay);
		sb.append(", connectionCleanerRepeatInterval:" + connectionCleanerRepeatInterval);

		return sb.toString();
	}
}
