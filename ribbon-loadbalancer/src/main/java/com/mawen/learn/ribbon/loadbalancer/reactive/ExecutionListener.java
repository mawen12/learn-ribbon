package com.mawen.learn.ribbon.loadbalancer.reactive;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
public interface ExecutionListener<I, O> {

	class AbortExecutionException extends RuntimeException {

		public AbortExecutionException(String message) {
			super(message);
		}

		public AbortExecutionException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	void onExecutionStart(ExecutionContext<I> context) throws AbortExecutionException;

	void onStartWithServer(ExecutionContext<I> context, ExecutionInfo info) throws AbortExecutionException;

	void onExceptionWithServer(ExecutionContext<I> context, Throwable exception, ExecutionInfo info) throws AbortExecutionException;

	void onExecutionSuccess(ExecutionContext<I> context, O response, ExecutionInfo info) throws AbortExecutionException;

	void onExecutionFailed(ExecutionContext<I> context, Throwable finalException, ExecutionInfo info) throws AbortExecutionException;
}
