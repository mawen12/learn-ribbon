package com.mawen.learn.ribbon.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.String.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class Utils {

	private Utils() {}

	public static <T> Method methodByName(Class<T> aClass, String name) {
		for (Method method : aClass.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		return null;
	}

	public static <T> T newInstance(Class<T> aClass) {
		try {
			return aClass.newInstance();
		}
		catch (Exception e) {
			throw new RibbonProxyException("Cannot instantiate object from class " + aClass, e);
		}
	}

	public static Object executeOnInstance(Object object, Method method, Object[] args) {
		Method targetMethod = methodByName(object.getClass(), method.getName());
		if (targetMethod == null) {
			throw new IllegalArgumentException(format("Signature of method %s is not compatible with the object %s",
					method.getName(), object.getClass().getSimpleName()));
		}

		try {
			return targetMethod.invoke(object, args);
		}
		catch (Exception e) {
			throw new RibbonProxyException(format("Failed to execute method %s on object %s",
					method.getName(), object.getClass().getSimpleName()), e);
		}
	}
}
