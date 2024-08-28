package com.mawen.learn.ribbon.client;

import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.loadbalancer.ILoadBalancer;
import com.mawen.learn.ribbon.loadbalancer.LoadBalancerContext;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
public abstract class AbstractLoadBalancerAwareClient<S extends ClientRequest, T extends IResponse>
		extends LoadBalancerContext implements IClientConfigAware {

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

	public T executeWithLoadBalancer(S request) {
		return executeWithLoadBalancer(request, null);
	}

	public T executeWithLoadBalancer(final S request, final IClientConfig requestConfig) {
		RequestSpecificRetryHandler handler = getRequestSpecificRetryHandler(request, requestConfig);
		LoadBalancerCommand.builder();
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
