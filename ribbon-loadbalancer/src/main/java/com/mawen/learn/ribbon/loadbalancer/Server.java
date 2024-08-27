package com.mawen.learn.ribbon.loadbalancer;


import com.netflix.util.Pair;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
@Setter
@Getter
public class Server {

	public static final String UNKNOWN_ZONE = "UNKNOWN";

	public interface MetaInfo {

		String getAppName();

		String getServerGroup();

		String getServiceIdForDiscovery();

		String getInstanceId();
	}

	private String host;
	private int port = 80;
	private volatile String id;
	private boolean isAliveFlag;
	private String zone = UNKNOWN_ZONE;
	private volatile boolean readyToServe = true;

	private MetaInfo metaInfo = new MetaInfo() {
		@Override
		public String getAppName() {
			return null;
		}

		@Override
		public String getServerGroup() {
			return null;
		}

		@Override
		public String getServiceIdForDiscovery() {
			return null;
		}

		@Override
		public String getInstanceId() {
			return null;
		}
	};

	public Server(String host, int port) {
		this.host = host;
		this.port = port;
		this.id = host + ":" + port;
		isAliveFlag = false;
	}

	public Server(String id) {
		setId(id);
		isAliveFlag = false;
	}

	public void setId(String id) {
		Pair<String, Integer> hostPort = getHostPort(id);
		if (hostPort != null) {
			this.id = hostPort.first() + ":" + hostPort.second();
			this.host = hostPort.first();
			this.port = hostPort.second();
		}
		else {
			this.id = null;
		}
	}

	public void setPort(int port) {
		this.port = port;

		if (host != null) {
			this.id = host + ":" + port;
		}
	}

	public void setHost(String host) {
		if (host != null) {
			this.host = host;
			this.id = host + ":" + port;
		}
	}

	public boolean isAlive() {
		return isAliveFlag;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Server))
			return false;
		Server svc = (Server) obj;
		return svc.getId().equals(this.getId());

	}

	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (null == this.getId() ? 0 : this.getId().hashCode());
		return hash;
	}

	public static String normalized(String id) {
		Pair<String, Integer> hostPort = getHostPort(id);
		if (hostPort == null) {
			return null;
		}
		else {
			return hostPort.first() + ":" + hostPort.second();
		}
	}

	static Pair<String, Integer> getHostPort(String id) {
		if (id != null) {
			String host = null;
			int port = 80;

			if (id.toLowerCase().startsWith("http://")) {
				id = id.substring(7);
			}
			else if (id.toLowerCase().startsWith("https://")) {
				id = id.substring(8);
			}

			if (id.contains("/")) {
				int slash_idx = id.indexOf("/");
				id = id.substring(0, slash_idx);
			}

			int colon_idx = id.indexOf(":");

			if (colon_idx == -1) {
				host = id;
				port = 80;
			}
			else {
				host = id.substring(0, colon_idx);
				try {
					port = Integer.parseInt(id.substring(colon_idx + 1));
				}
				catch (NumberFormatException e) {
					throw e;
				}
			}

			return new Pair<>(host, port);
		}
		else {
			return null;
		}
	}
}
