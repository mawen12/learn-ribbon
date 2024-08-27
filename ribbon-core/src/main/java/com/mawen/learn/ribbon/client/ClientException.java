package com.mawen.learn.ribbon.client;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/26
 */
@Getter
public class ClientException extends Exception {

	private static final long serialVersionUID = -4168849249971846611L;

	public enum ErrorType {
		GENERAL,
		CONFIGURATION,
		NUMBEROF_RETRIES_EXEEDED,
		NUMBEROF_RETRIES_NEXTSERVER_EXCEEDED,
		SOCKET_TIMEOUT_EXCEPTION,
		READ_TIMEOUT_EXCEPTION,
		UNKNOWN_TIMEOUT_EXCEPTION,
		CONNECT_EXCEPTION,
		CLIENT_EXCEPTION,
		SERVER_EXCEPTION,
		NO_ROUTE_TO_HOST_EXCEPTION,
		CACHE_MISSING;

		static String getName(int errorCode) {
			if (ErrorType.values().length >= errorCode) {
				return ErrorType.values()[errorCode].name();
			}
			else {
				return "UNKNOWN ERROR CODE";
			}
		}
	}

	private int errorCode;
	private String message;
	private Object errorObject;
	private ErrorType errorType = ErrorType.GENERAL;

	public ClientException(String message) {
		this(null, message, null);
	}

	public ClientException(int errorCode) {
		this(errorCode, null, null);
	}

	public ClientException(int errorCode, String message) {
		this(errorCode, message, null);
	}

	public ClientException(Throwable chainedException) {
		this(null, null, chainedException);
	}

	public ClientException(int errorCode, String message, Throwable chainedException) {
		super((message == null && errorCode != 0) ? ", code = " + errorCode + "->" + ErrorType.getName(errorCode) : message, chainedException);
		this.errorCode = errorCode;
		this.message = message;
	}

	public ClientException(ErrorType error) {
		this(error.ordinal(), null, null);
		this.errorType = error;
	}

	public ClientException(ErrorType error, String message) {
		this(error.ordinal(), message, null);
		this.errorType = error;
	}

	public ClientException(ErrorType error, String message, Throwable chainedException) {
		super((message == null && error.ordinal() != 0) ? ", code = " + error.ordinal() + "->" + error.ordinal() : message, chainedException);
		this.errorCode = error.ordinal();
		this.message = message;
		this.errorType = error;
	}

	public String getInternalMessage() {
		return "{no message: " + errorCode + "}";
	}

	public static HashMap getErrorCodes(Class clazz) {
		HashMap map = new HashMap(23);

		Field[] fields = clazz.getDeclaredFields();

		for (int i = 0; i < fields.length; i++) {
			int mods = fields[i].getModifiers();

			if (Modifier.isFinal(mods) && Modifier.isStatic(mods) && Modifier.isPublic(mods)) {
				try {
					map.put(fields[i].get(null), fields[i].getName());
				}
				catch (Throwable e) {
					// ignore this
				}
			}
		}

		return map;
	}
}
