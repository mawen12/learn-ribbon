package com.mawen.learn.ribbon.http4;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.AbstractConnPool;
import org.apache.http.impl.conn.tsccm.ConnPoolByRoute;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
public class MonitoredConnectionManager extends ThreadSafeClientConnManager {

	public MonitoredConnectionManager(String name) {
		super();
		initMonitors(name);
	}

	public MonitoredConnectionManager(String name, SchemeRegistry schemeRegistry, long connTTL, TimeUnit connTTLTimeUnit) {
		super(schemeRegistry, connTTL, connTTLTimeUnit);
		initMonitors(name);
	}

	public MonitoredConnectionManager(String name, SchemeRegistry schemeRegistry) {
		super(schemeRegistry);
		initMonitors(name);
	}

	void initMonitors(String name) {
		if (this.pool instanceof NamedConnectionPool) {
			((NamedConnectionPool) this.pool).initMonitors(name);
		}
	}

	@Override
	protected AbstractConnPool createConnectionPool(HttpParams params) {
		return new NamedConnectionPool(connOperator, params);
	}

	@Override
	protected ConnPoolByRoute createConnectionPool(long connTTL, TimeUnit connTTLTimeUnit) {
		return new NamedConnectionPool(connOperator, connPerRoute, 20, connTTL, connTTLTimeUnit);
	}

	ConnPoolByRoute getConnectionPool() {
		return pool;
	}

	@Override
	public ClientConnectionRequest requestConnection(HttpRoute route, Object state) {
		return super.requestConnection(route, state);
	}
}
