package com.mawen.learn.ribbon.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Http {

	enum HttpMethod {
		DELETE,
		GET,
		POST,
		PATCH,
		PUT
	}

	@interface Header {
		String name();

		String value();
	}

	HttpMethod method();

	String uri() default "";

	Header[] headers() default {};
}
