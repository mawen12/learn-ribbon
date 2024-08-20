package com.mawen.learn.ribbon.loadbalancer;

/**
 *
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/20
 */
public class PingConstant implements IPing {

	private boolean constant = true;

	@Override
	public boolean isAlive(Server server) {
		return constant;
	}

	public void setConstant(String constantStr) {
		constant = (constantStr != null) && (constantStr.toLowerCase().equals("true"));
	}

	public void setConstant(boolean constant) {
		this.constant = constant;
	}

	public boolean isConstant() {
		return constant;
	}
}
