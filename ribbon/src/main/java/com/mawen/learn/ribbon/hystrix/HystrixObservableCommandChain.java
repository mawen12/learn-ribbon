package com.mawen.learn.ribbon.hystrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netflix.hystrix.HystrixObservableCommand;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class HystrixObservableCommandChain<T> {

	private final List<HystrixObservableCommand<T>> hystrixCommands;

	public HystrixObservableCommandChain(List<HystrixObservableCommand<T>> hystrixCommands) {
		this.hystrixCommands = hystrixCommands;
	}

	public HystrixObservableCommandChain(HystrixObservableCommand<T>... commands) {
		this.hystrixCommands = new ArrayList<>(commands.length);
		Collections.addAll(hystrixCommands, commands);
	}

	public Observable<ResultCommandPair<T>> toResultCommandPairObservable() {
		Observable<ResultCommandPair<T>> rootObservable = null;

		for (HystrixObservableCommand<T> command : hystrixCommands) {
			Observable<ResultCommandPair<T>> observable = command.toObservable().map(result -> new ResultCommandPair<>(result, command));
			rootObservable = rootObservable == null ? observable : rootObservable.onErrorResumeNext(observable);
		}

		return rootObservable;
	}

	public Observable<T> toObservable() {
		Observable<T> rootObservable = null;

		for (HystrixObservableCommand<T> command : hystrixCommands) {
			Observable<T> observable = command.toObservable();
			rootObservable = rootObservable == null ? observable : rootObservable.onErrorResumeNext(observable);
		}

		return rootObservable;
	}

	public List<HystrixObservableCommand<T>> getCommands() {
		return Collections.unmodifiableList(hystrixCommands);
	}

	public HystrixObservableCommand getLastCommand() {
		return hystrixCommands.get(hystrixCommands.size() - 1);
	}
}
