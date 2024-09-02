package com.mawen.learn.ribbon.hystrix;

import com.netflix.hystrix.HystrixObservableCommand;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class ResultCommandPair<T> {

	private final T result;
	private final HystrixObservableCommand<T> command;

	public ResultCommandPair(T result, HystrixObservableCommand<T> command) {
		this.result = result;
		this.command = command;
	}

	public T getResult() {
		return result;
	}

	public HystrixObservableCommand<T> getCommand() {
		return command;
	}
}
