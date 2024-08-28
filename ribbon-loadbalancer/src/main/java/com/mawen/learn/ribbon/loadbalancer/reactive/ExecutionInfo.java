package com.mawen.learn.ribbon.loadbalancer.reactive;

import com.mawen.learn.ribbon.loadbalancer.Server;
import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
@Getter
public class ExecutionInfo {

	private final Server server;

	private final int numberOfPastAttemptsOnServer;

	private final int numberOfPastServersAttempted;

	private ExecutionInfo(Server server, int numberOfPastAttemptsOnServer, int numberOfPastServersAttempted) {
		this.server = server;
		this.numberOfPastAttemptsOnServer = numberOfPastAttemptsOnServer;
		this.numberOfPastServersAttempted = numberOfPastServersAttempted;
	}

	public static ExecutionInfo create(Server server, int numberOfPastAttemptsOnServer, int numberOfPastServersAttempted) {
		return new ExecutionInfo(server, numberOfPastAttemptsOnServer, numberOfPastServersAttempted);
	}

	@Override
	public String toString() {
		return "ExecutionInfo{" +
				"server=" + server +
				", numberOfPastAttemptsOnServer=" + numberOfPastAttemptsOnServer +
				", numberOfPastServersAttempted=" + numberOfPastServersAttempted +
				'}';
	}
}
