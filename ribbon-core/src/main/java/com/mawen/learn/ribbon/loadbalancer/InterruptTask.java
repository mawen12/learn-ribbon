package com.mawen.learn.ribbon.loadbalancer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class InterruptTask extends TimerTask {

	private static final Timer timer = new Timer("InterruptTimer", true);

	private final Thread target;

	public InterruptTask(long millis) {
		this.target = Thread.currentThread();
		timer.schedule(this, millis);
	}

	public InterruptTask(Thread target, long millis) {
		this.target = target;
		timer.schedule(this, millis);
	}

	public boolean cancel() {
		try {
			return super.cancel();
		}
		catch (Exception e) {
			return false;
		}
	}

	@Override
	public void run() {
		if (target != null && target.isAlive()) {
			target.interrupt();
		}
	}
}
