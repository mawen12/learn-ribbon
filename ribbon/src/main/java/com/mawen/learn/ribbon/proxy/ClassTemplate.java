package com.mawen.learn.ribbon.proxy;

import com.mawen.learn.ribbon.http.HttpResourceGroup;
import com.mawen.learn.ribbon.proxy.annotation.ResourceGroup;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
class ClassTemplate<T> {

	private final Class<T> clientInterface;
	private final String resourceGroupName;
	private final Class<? extends HttpResourceGroup> resourceGroupClass;

	ClassTemplate(Class<T> clientInterface) {
		this.clientInterface = clientInterface;

		ResourceGroup annotation = clientInterface.getAnnotation(ResourceGroup.class);
		if (annotation != null) {
			String name = annotation.name().trim();
			resourceGroupName = name.isEmpty() ? null : annotation.name();
			if (annotation.resourceGroupClass().length == 0) {
				resourceGroupClass = null;
			}
			else if (annotation.resourceGroupClass().length == 1) {
				resourceGroupClass = annotation.resourceGroupClass()[0];
			}
			else {
				throw new ProxyAnnotationException("only one resource group may be defined with @ResourceGroup annotation ");
			}
			verify();
		}
		else {
			resourceGroupName = null;
			resourceGroupClass = null;
		}
	}

	public static <T> ClassTemplate<T> from(Class<T> clientInterface) {
		return new ClassTemplate<T>(clientInterface);
	}

	public Class<T> getClientInterface() {
		return clientInterface;
	}

	public String getResourceGroupName() {
		return resourceGroupName;
	}

	public Class<? extends HttpResourceGroup> getResourceGroupClass() {
		return resourceGroupClass;
	}

	private void verify() {
		if (resourceGroupName != null && resourceGroupClass != null) {
			throw new RibbonProxyException("Both resource group name and class defined with @ResourceGroup");
		}
	}
}
