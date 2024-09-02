package com.mawen.learn.ribbon.proxy.processor;

import java.lang.reflect.Method;

import com.mawen.learn.ribbon.RibbonResourceFactory;

import static com.mawen.learn.ribbon.ResourceGroup.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public interface AnnotationProcessor<T extends GroupBuilder, S extends TemplateBuilder> {

	void process(String templateName, S templateBuilder, Method method);

	void process(String groupName, T groupBuilder, RibbonResourceFactory resourceFactory, Class<?> interfaceClass);
}
