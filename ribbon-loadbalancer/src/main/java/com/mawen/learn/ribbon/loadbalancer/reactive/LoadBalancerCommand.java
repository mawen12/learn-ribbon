package com.mawen.learn.ribbon.loadbalancer.reactive;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.mawen.learn.ribbon.client.ClientException;
import com.mawen.learn.ribbon.client.RetryHandler;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.LoadBalancerContext;
import com.mawen.learn.ribbon.loadbalancer.Server;
import com.mawen.learn.ribbon.loadbalancer.ServerStats;
import com.netflix.servo.monitor.Stopwatch;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
@Slf4j
public class LoadBalancerCommand<T> {

	private final URI loadBalancerURI;
	private final Object loadBalancerKey;
	private final LoadBalancerContext loadBalancerContext;
	private final RetryHandler retryHandler;
	private volatile ExecutionInfo executionInfo;
	private final Server server;
	private final ExecutionContextListenerInvoker<?, T> listenerInvoker;

	public LoadBalancerCommand(Builder<T> builder) {
		this.loadBalancerURI = builder.loadBalancerURI;
		this.loadBalancerKey = builder.loadBalancerKey;
		this.loadBalancerContext = builder.loadBalancerContext;
		this.retryHandler = builder.retryHandler != null ? builder.retryHandler : loadBalancerContext.getRetryHandler();
		this.server = builder.server;
		this.listenerInvoker = builder.invoker;
	}

	public Observable<T> submit(final ServerOperation<T> operation) {
		final ExecutionInfoContext context = new ExecutionInfoContext();

		if (listenerInvoker != null) {
			try {
				listenerInvoker.onExecutionStart();
			}
			catch (ExecutionListener.AbortExecutionException e) {
				return Observable.error(e);
			}
		}

		final int maxRetriesSame = retryHandler.getMaxRetriesOnSameServer();
		final int maxRetriesNext = retryHandler.getMaxRetriesOnNextServer();

		Observable<T> o = (server == null ? selectServer() : Observable.just(server))
				.concatMap(new Func1<Server, Observable<T>>() {
					@Override
					public Observable<T> call(Server server) {
						context.setServer(server);
						final ServerStats stats = loadBalancerContext.getServerStats(server);

						Observable<T> o = Observable.just(server)
								.concatMap(new Func1<Server, Observable<T>>() {
									@Override
									public Observable<T> call(Server server) {
										context.incAttemptCount();
										loadBalancerContext.noteOpenConnection(stats);

										if (listenerInvoker != null) {
											try {
												listenerInvoker.onStartWithServer(context.toExecutionInfo());
											}
											catch (ExecutionListener.AbortExecutionException e) {
												return Observable.error(e);
											}
										}

										final Stopwatch tracer = loadBalancerContext.getExecuteTracer().start();

										return operation.call(server).doOnEach(new Observer<T>() {

											private T entity;

											@Override
											public void onCompleted() {
												recordStats(tracer, stats, entity, null);
											}

											@Override
											public void onError(Throwable e) {
												recordStats(tracer, stats, null, e);
												log.debug("Got error {} when executed on server {}", e, server);
												if (listenerInvoker != null) {
													listenerInvoker.onExceptionWithServer(e, context.toExecutionInfo());
												}
											}

											@Override
											public void onNext(T t) {
												this.entity = t;
												if (listenerInvoker != null) {
													listenerInvoker.onExecutionSuccess(entity, context.toExecutionInfo());
												}
											}

											private void recordStats(Stopwatch tracer, ServerStats stats, T entity, Throwable exception) {
												tracer.stop();
												loadBalancerContext.noteRequestCompletion(stats, entity, exception, tracer.getDuration(TimeUnit.MILLISECONDS), retryHandler);
											}
										});
									}
								});

						if (maxRetriesSame > 0) {
							o = o.retry(retryPolicy(maxRetriesSame, true));
						}
						return o;
					}
				});

		if (maxRetriesNext > 0 && server == null) {
			o = o.retry(retryPolicy(maxRetriesNext, false));
		}

		return o.onErrorResumeNext(new Func1<Throwable, Observable<? extends T>>() {
			@Override
			public Observable<? extends T> call(Throwable e) {
				if (context.getAttemptCount() > 0) {
					if (maxRetriesNext > 0 && context.getServerAttemptCount() == maxRetriesNext + 1) {
						e = new ClientException(ClientException.ErrorType.NUMBEROF_RETRIES_NEXTSERVER_EXCEEDED,
								"Number of retries on next server exceeded max " + maxRetriesNext
										+ " retries, while making a call for: " + context.getServer(), e);
					}
					else if (maxRetriesSame > 0 && context.getAttemptCount() == maxRetriesSame + 1) {
						e = new ClientException(ClientException.ErrorType.NUMBEROF_RETRIES_EXEEDED,
								"Number of retries exceeded max " + maxRetriesSame
										+ " retries, while making a call for: " + context.getServer(), e);
					}
				}
				if (listenerInvoker != null) {
					listenerInvoker.onExecutionFailed(e, context.toFinalExecutionInfo());
				}
				return Observable.error(e);
			}
		});
	}

	private Observable<Server> selectServer() {
		return Observable.create(next -> {
			try {
				Server server = loadBalancerContext.getServerFromLoadBalancer(loadBalancerURI, loadBalancerKey);
				next.onNext(server);
				next.onCompleted();
			}
			catch (Exception e) {
				next.onError(e);
			}
		});
	}

	private Func2<Integer, Throwable, Boolean> retryPolicy(final int maxRetries, final boolean same) {
		return new Func2<Integer, Throwable, Boolean>() {
			@Override
			public Boolean call(Integer tryCount, Throwable e) {
				if (e instanceof ExecutionListener.AbortExecutionException) {
					return false;
				}

				if (tryCount > maxRetries) {
					return false;
				}

				if (e.getCause() != null && e instanceof RuntimeException) {
					e = e.getCause();
				}
				return retryHandler.isRetriableException(e, same);
			}
		};
	}

	public static class Builder<T> {

		private RetryHandler retryHandler;
		private ILoadBalancer loadBalancer;
		private IClientConfig config;
		private LoadBalancerContext loadBalancerContext;
		private List<? extends ExecutionListener<?, T>> listeners;
		private Object loadBalancerKey;
		private ExecutionContext<?> executionContext;
		private ExecutionContextListenerInvoker invoker;
		private URI loadBalancerURI;
		private Server server;

		private Builder() {
		}

		public Builder<T> withLoadBalancer(ILoadBalancer loadBalancer) {
			this.loadBalancer = loadBalancer;
			return this;
		}

		public Builder<T> withLoadBalancerURI(URI loadBalancerURI) {
			this.loadBalancerURI = loadBalancerURI;
			return this;
		}

		public Builder<T> withListeners(List<? extends ExecutionListener<?, T>> listeners) {
			if (this.listeners == null) {
				this.listeners = new LinkedList<ExecutionListener<?, T>>(listeners);
			}
			else {
				this.listeners.addAll((Collection) listeners);
			}
			return this;
		}

		public Builder<T> withRetryHandler(RetryHandler retryHandler) {
			this.retryHandler = retryHandler;
			return this;
		}

		public Builder<T> withClientConfig(IClientConfig config) {
			this.config = config;
			return this;
		}

		public Builder<T> withServerLocator(Object key) {
			this.loadBalancerKey = key;
			return this;
		}

		public Builder<T> withLoadBalancerContext(LoadBalancerContext loadBalancerContext) {
			this.loadBalancerContext = loadBalancerContext;
			return this;
		}

		public Builder<T> withExecutionContext(ExecutionContext<?> executionContext) {
			this.executionContext = executionContext;
			return this;
		}

		public Builder<T> withServer(Server server) {
			this.server = server;
			return this;
		}

		public LoadBalancerCommand<T> build() {
			if (loadBalancerContext == null && loadBalancer == null) {
				throw new IllegalArgumentException("Either LoadBalancer or LoadBalancerContext needs to be set");
			}

			if (listeners != null && listeners.size() > 0) {
				this.invoker = new ExecutionContextListenerInvoker(executionContext, listeners, config);
			}

			if (loadBalancerContext == null) {
				loadBalancerContext = new LoadBalancerContext(loadBalancer, config);
			}

			return new LoadBalancerCommand<T>(this);
		}
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	@Getter
	class ExecutionInfoContext {
		Server server;
		int serverAttemptCount = 0;
		int attemptCount = 0;

		public void setServer(Server server) {
			this.server = server;
			this.serverAttemptCount++;
			this.attemptCount = 0;
		}

		public void incAttemptCount() {
			this.attemptCount++;
		}

		public ExecutionInfo toExecutionInfo() {
			return ExecutionInfo.create(server, attemptCount - 1, serverAttemptCount - 1);
		}

		public ExecutionInfo toFinalExecutionInfo() {
			return ExecutionInfo.create(server, attemptCount, serverAttemptCount - 1);
		}
	}
}
