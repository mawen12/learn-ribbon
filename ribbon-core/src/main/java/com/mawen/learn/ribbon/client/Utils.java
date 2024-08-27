package com.mawen.learn.ribbon.client;

import java.util.Collection;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
public class Utils {

	public static boolean isPresentAsCause(Throwable throwableToSearchIn,
			Collection<Class<? extends Throwable>> throwableToSearchFor) {
		int infiniteLoopPreventionCounter = 10;
		while (throwableToSearchIn != null && infiniteLoopPreventionCounter > 0) {
			infiniteLoopPreventionCounter--;
			for (Class<? extends Throwable> c : throwableToSearchFor) {
				if (c.isAssignableFrom(throwableToSearchIn.getClass())) {
					return true;
				}
			}
			throwableToSearchIn = throwableToSearchIn.getCause();
		}
		return false;
	}
}
