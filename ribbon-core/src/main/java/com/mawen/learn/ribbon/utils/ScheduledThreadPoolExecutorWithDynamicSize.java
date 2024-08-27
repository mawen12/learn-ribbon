package com.mawen.learn.ribbon.utils;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import com.netflix.config.DynamicIntProperty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public class ScheduledThreadPoolExecutorWithDynamicSize extends ScheduledThreadPoolExecutor {

	private final Thread shutdownThread;

	public ScheduledThreadPoolExecutorWithDynamicSize(final DynamicIntProperty coorPoolSize, ThreadFactory threadFactory) {
		super(coorPoolSize.get(), threadFactory);
		coorPoolSize.addCallback(() -> setCorePoolSize(coorPoolSize.get()));

		shutdownThread = new Thread(new Runnable() {
			@Override
			public void run() {
				shutdown();

				if (shutdownThread != null) {
					try {
						Runtime.getRuntime().removeShutdownHook(shutdownThread);
					}
					catch (IllegalStateException e) {
						// NOPMD
					}
				}
			}
		});

		Runtime.getRuntime().addShutdownHook(shutdownThread);
	}
}
