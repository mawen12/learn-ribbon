package com.mawen.learn.ribbon.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.mawen.learn.ribbon.proxy.annotation.Content;
import com.mawen.learn.ribbon.proxy.annotation.ContentTransformerClass;
import com.mawen.learn.ribbon.proxy.annotation.TemplateName;
import com.mawen.learn.ribbon.proxy.annotation.Var;
import io.reactivex.netty.channel.ContentTransformer;

import static java.lang.String.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
class MethodTemplate {

	private final Method method;
	private final String templateName;
	private final String[] paramNames;
	private final int[] valueIdxs;
	private final int contentArgPosition;
	private final Class<? extends ContentTransformer<?>> contentTransformerClass;
	private final Class<?> resultType;
	private final Class<?> genericContentType;


	MethodTemplate(Method method) {
		this.method = method;
		MethodAnnotationValues values = new MethodAnnotationValues(method);
		this.templateName = values.templateName();
	}

	private static class MethodAnnotationValues {

		private final Method method;
		private String templateName;
		private String[] paramNames;
		private int[] valueIdxs;
		private int contentArgPosition;
		private Class<? extends ContentTransformer<?>> contentTransformerClass;
		private Class<?> resultType;
		private Class<?> genericContentType;

		private MethodAnnotationValues(Method method) {
			this.method = method;
			extractTemplateName();
			extractParamNamesWithIndexs();
			extractContentArgPosition();
			extractContentTransformerClass();
			extractResultType();
		}

		private void extractTemplateName() {
			TemplateName annotation = method.getAnnotation(TemplateName.class);
			templateName = annotation == null ? method.getName() : annotation.value();
		}

		private void extractParamNamesWithIndexs() {
			List<String> nameList = new ArrayList<>();
			List<Integer> idxList = new ArrayList<>();

			Annotation[][] params = method.getParameterAnnotations();
			for (int i = 0; i < params.length; i++) {
				for (Annotation a : params[i]) {
					if (a.annotationType().equals(Var.class)) {
						String name = ((Var) a).value();
						nameList.add(name);
						idxList.add(i);
					}
				}
			}

			int size = nameList.size();
			paramNames = new String[size];
			valueIdxs = new int[size];
			for (int i = 0; i < size; i++) {
				paramNames[i] = nameList.get(i);
				valueIdxs[i] = idxList.get(i);
			}
		}

		private void extractContentArgPosition() {
			Annotation[][] params = method.getParameterAnnotations();
			int pos = -1, count = 0;

			for (int i = 0; i < params.length; i++) {
				for (Annotation a : params[i]) {
					if (a.annotationType().equals(Content.class)) {
						pos = i;
						count++;
					}
				}
			}

			if (count > 1) {
				throw new ProxyAnnotationException(format("Method %s annotation multiple parameters as @Content - at most one is allowed ", methodName()));
			}

			contentArgPosition = pos;
			if (contentArgPosition >= 0) {
				Type type = method.getGenericParameterTypes()[contentArgPosition];
				if (type instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) type;
					if (pt.getActualTypeArguments() != null) {
						genericContentType = (Class<?>) pt.getActualTypeArguments()[0];
					}
				}
			}
		}

		private void extractContentTransformerClass() {
			ContentTransformerClass annotation = method.getAnnotation(ContentTransformerClass.class);
			if (contentArgPosition == -1) {
				if (annotation != null) {
					throw new ProxyAnnotationException(format("ContentTransformerClass defined on method %s with no @Content parameter", method.getName()));
				}
				return;
			}

			if (annotation == null) {
				Class<?> contentType = method.getParameterTypes()[contentArgPosition];

			}

		}
	}
}
