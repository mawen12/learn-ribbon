package com.mawen.learn.ribbon.loadbalancer;

import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/27
 */
@Setter
@Getter
public class PingConstant implements IPing{

	boolean constant = true;

	public void setConstant(String constantStr) {
		constant = constantStr != null && constantStr.toLowerCase().equals("true");
	}

	@Override
	public boolean isAlive(Server server) {
		return constant;
	}
}
