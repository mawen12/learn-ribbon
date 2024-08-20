package com.mawen.learn.ribbon.loadbalancer;

import java.util.Objects;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/19
 */
public class Server {

	public static final String UNKNOWN_ZONE = "UNKNOWN";

	private String host;

	private int port = 80;

	private String id;

	private boolean isAliveFlag;

	private String zone = UNKNOWN_ZONE;

	private volatile boolean readToServe = true;

	public Server(String host, int port) {
		this.host = host;
		this.port = port;
		this.id = host + ":" + port;
		this.isAliveFlag = false;
	}

	public Server(String id) {
		setId(id);
		isAliveFlag = false;
	}

	public boolean isAlive() {
		return isAliveFlag;
	}

	public void setAlive(boolean aliveFlag) {
		isAliveFlag = aliveFlag;
	}

	public void setHostPort(String hostPort) {
		setId(hostPort);
	}

	public void setId(String id) {
		this.id = normalizeId(id);
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

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getId() {
		return id;
	}

	public String getHostPort() {
		return host + ":" + port;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public boolean isReadToServe() {
		return readToServe;
	}

	public void setReadToServe(boolean readToServe) {
		this.readToServe = readToServe;
	}

	@Override
	public String toString() {
		return this.getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Server)) {
			return false;
		}

		Server server = (Server) obj;
		return server.getId().equals(this.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId());
	}

	public static String normalizeId(String id) {
		if (id != null) {
			String host = null;
			int port = 80;

			if (id.toLowerCase().startsWith("http://")) {
				id = id.substring(7);
			}
			else if (id.toLowerCase().startsWith("https://")) {
				id = id.substring(8);
			}

			int slashIdx = id.indexOf('/');
			if (slashIdx != -1) {
				id = id.substring(0, slashIdx);
			}

			int colonIdx = id.indexOf(':');
			if (colonIdx != -1) {
				host = id;
				port = 80;
			}
			else {
				host = id.substring(0, colonIdx);
				try {
					port = Integer.parseInt(id.substring(colonIdx + 1));
				}
				catch (NumberFormatException e) {
					throw e;
				}
			}

			if (null == host) {
				return null;
			}

			return host + ":" + port;
		}
		else {
			return null;
		}
	}
}
