package com.mawen.learn.ribbon.client;

import java.net.URI;

import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.LoadBalancerContext;
import com.mawen.learn.ribbon.loadbalancer.Server;
import com.mawen.learn.ribbon.loadbalancer.reactive.LoadBalancerCommand;
import com.mawen.learn.ribbon.loadbalancer.reactive.ServerOperation;
import rx.Observable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractLoadBalancerAwareClient<S extends ClientRequest, T extends IResponse>
		extends LoadBalancerContext implements IClient<S, T>, IClientConfigAware {

	public AbstractLoadBalancerAwareClient(ILoadBalancer lb) {
		super(lb);
	}

	public AbstractLoadBalancerAwareClient(ILoadBalancer lb, IClientConfig clientConfig) {
		super(lb, clientConfig);
	}

	protected boolean isCircuitBreakerException(Throwable e) {
		if (getRetryHandler() != null) {
			return getRetryHandler().isCircuitTrippingException(e);
		}
		return false;
	}

	protected boolean isRetriableException(Throwable e) {
		if (getRetryHandler() != null) {
			return getRetryHandler().isRetriableException(e, true);
		}
		return false;
	}

	public T executeWithLoadBalancer(S request) throws ClientException {
		return executeWithLoadBalancer(request, null);
	}

	public T executeWithLoadBalancer(final S request, final IClientConfig requestConfig) throws ClientException {
		RequestSpecificRetryHandler handler = getRequestSpecificRetryHandler(request, requestConfig);
		LoadBalancerCommand<T> command = LoadBalancerCommand.<T>builder()
				.withLoadBalancerContext(this)
				.withRetryHandler(handler)
				.withLoadBalancerURI(request.getUri())
				.build();

		try {
			return command.submit(new ServerOperation<T>() {
						@Override
						public Observable<T> call(Server server) {
							URI finalUri = reconstructURIWithServer(server, request.getUri());
							S requestForServer = (S) request.replaceUri(finalUri);
							try {
								return Observable.just(AbstractLoadBalancerAwareClient.this.execute(requestForServer, requestConfig));
							}
							catch (Exception e) {
								return Observable.error(e);
							}
						}
					})
					.toBlocking()
					.single();
		}
		catch (Exception e) {
			Throwable t = e.getCause();
			if (t instanceof ClientException) {
				throw (ClientException) t;
			}
			else {
				throw new ClientException(t);
			}
		}
	}

	public abstract RequestSpecificRetryHandler getRequestSpecificRetryHandler(S request, IClientConfig requestConfig);

	protected boolean isRetriable(S request) {
		if (request.getIsRetriable()) {
			return true;
		}
		else {
			boolean retryOkayOnOperation = okToRetryOnAllOperations;
			IClientConfig overrideClientConfig = request.getOverrideConfig();
			if (overrideClientConfig != null) {
				retryOkayOnOperation = overrideClientConfig.getPropertyAsBoolean(CommonClientConfigKey.RequestSpecificRetryOn, okToRetryOnAllOperations);
			}
			return retryOkayOnOperation;
		}
	}
}
