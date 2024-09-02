package com.mawen.learn.ribbon.proxy.processor;

import java.lang.reflect.Method;

import com.mawen.learn.ribbon.CacheProvider;
import com.mawen.learn.ribbon.RibbonResourceFactory;

import static com.mawen.learn.ribbon.ResourceGroup.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class CacheProviderAnnotationProcessor implements AnnotationProcessor<GroupBuilder, TemplateBuilder> {

	@Override
	public void process(String templateName, TemplateBuilder templateBuilder, Method method) {
		CacheProvider annotation = method.getAnnotation(CacheProvider.class);
		if (annotation != null) {
			Utils.newInstance(annotation.provider());
		}
	}

	@Override
	public void process(String groupName, GroupBuilder groupBuilder, RibbonResourceFactory resourceFactory, Class<?> interfaceClass) {

	}
}
