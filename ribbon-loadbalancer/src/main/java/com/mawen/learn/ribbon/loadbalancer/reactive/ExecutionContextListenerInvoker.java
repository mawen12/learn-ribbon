package com.mawen.learn.ribbon.loadbalancer.reactive;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.mawen.learn.ribbon.client.config.CommonClientConfigKey;
import com.mawen.learn.ribbon.client.config.IClientConfig;
import com.mawen.learn.ribbon.client.config.IClientConfigKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
@Slf4j
public class ExecutionContextListenerInvoker<I, O> {

	private final ExecutionContext<I> context;
	private final List<ExecutionListener<I, O>> listeners;
	private final IClientConfig clientConfig;
	private final ConcurrentHashMap<String, IClientConfigKey> classConfigKeyMap;

	public ExecutionContextListenerInvoker(ExecutionContext<I> context, List<ExecutionListener<I, O>> listeners) {
		this(context, listeners, null);
	}

	public ExecutionContextListenerInvoker(List<ExecutionListener<I, O>> listeners, IClientConfig clientConfig) {
		this(null, listeners, clientConfig);
	}

	public ExecutionContextListenerInvoker(List<ExecutionListener<I, O>> listeners) {
		this(null, listeners);
	}

	public ExecutionContextListenerInvoker(ExecutionContext<I> context, List<ExecutionListener<I, O>> listeners, IClientConfig clientConfig) {
		this.context = context;
		this.listeners = Collections.unmodifiableList(listeners);
		this.clientConfig = clientConfig;
		if (clientConfig != null) {
			classConfigKeyMap = new ConcurrentHashMap<>();
		}
		else {
			this.classConfigKeyMap = null;
		}
	}

	public void onExecutionStart() {
		onExecutionStart(this.context);
	}

	public void onExecutionStart(ExecutionContext<I> context) {
		for (ExecutionListener<I, O> listener : listeners) {
			try {
				if (!isListenerDisabled(listener)) {
					listener.onExecutionStart(context.getChildContext(listener));
				}
			}
			catch (Throwable e) {
				if (e instanceof ExecutionListener.AbortExecutionException) {
					throw (ExecutionListener.AbortExecutionException) e;
				}
				log.error("Error invoking listener " + listener, e);
			}
		}
	}

	public void onStartWithServer(ExecutionInfo info) {
		onStartWithServer(this.context, info);
	}

	public void onStartWithServer(ExecutionContext<I> context, ExecutionInfo info) {
		for (ExecutionListener<I, O> listener : listeners) {
			try {
				if (!isListenerDisabled(listener)) {
					listener.onStartWithServer(context.getChildContext(listener), info);
				}
			}
			catch (Throwable e) {
				if (e instanceof ExecutionListener.AbortExecutionException) {
					throw (ExecutionListener.AbortExecutionException) e;
				}
				log.error("Error invoking listener " + listener, e);
			}
		}
	}

	public void onExceptionWithServer(Throwable e, ExecutionInfo info) {
		onExceptionWithServer(this.context, e, info);
	}

	public void onExceptionWithServer(ExecutionContext<I> context, Throwable exception, ExecutionInfo info) {
		for (ExecutionListener<I, O> listener : listeners) {
			try {
				if (!isListenerDisabled(listener)) {
					listener.onExceptionWithServer(context.getChildContext(listener), exception, info);
				}
			}
			catch (Throwable e) {
				if (e instanceof ExecutionListener.AbortExecutionException) {
					throw (ExecutionListener.AbortExecutionException) e;
				}
				log.error("Error invoking listener " + listener, e);
			}
		}
	}

	public void onExecutionSuccess(O response, ExecutionInfo info) {
		onExecutionSuccess(this.context, response, info);
	}

	public void onExecutionSuccess(ExecutionContext<I> context, O response, ExecutionInfo info) {
		for (ExecutionListener<I, O> listener : listeners) {
			try {
				if (!isListenerDisabled(listener)) {
					listener.onExecutionSuccess(context.getChildContext(listener), response, info);
				}
			}
			catch (Throwable e) {
				if (e instanceof ExecutionListener.AbortExecutionException) {
					throw (ExecutionListener.AbortExecutionException) e;
				}
				log.error("Error invoking listener " + listener, e);
			}
		}
	}

	public void onExecutionFailed(Throwable e, ExecutionInfo info) {
		onExecutionFailed(this.context, e, info);
	}

	public void onExecutionFailed(ExecutionContext<I> context, Throwable finalException, ExecutionInfo info) {
		for (ExecutionListener<I, O> listener : listeners) {
			try {
				if (!isListenerDisabled(listener)) {
					listener.onExecutionFailed(context.getChildContext(listener), finalException, info);
				}
			}
			catch (Throwable e) {
				if (e instanceof ExecutionListener.AbortExecutionException) {
					throw (ExecutionListener.AbortExecutionException) e;
				}
				log.error("Error invoking listener " + listener, e);
			}
		}
	}

	private boolean isListenerDisabled(ExecutionListener<I, O> listener) {
		if (clientConfig == null) {
			return false;
		}
		else {
			String className = listener.getClass().getName();
			IClientConfigKey key = classConfigKeyMap.get(className);
			if (key == null) {
				key = CommonClientConfigKey.valueOf("listener." + className + ".disabled");
				IClientConfigKey old = classConfigKeyMap.put(className, key);
				if (old != null) {
					key = old;
				}
			}
			return clientConfig.getPropertyAsBoolean(key, false);
		}
	}
}
