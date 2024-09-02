package com.mawen.learn.ribbon.template;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
class TemplateVar {
	private final String val;

	TemplateVar(String val) {
		this.val = val;
	}

	@Override
	public String toString() {
		return val;
	}
}
