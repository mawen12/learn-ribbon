package com.mawen.learn.ribbon;

import com.mawen.learn.ribbon.client.config.ClientConfigFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public abstract class RibbonResourceFactory {

	protected final ClientConfigFactory clientConfigFactory;
	protected final RibbonTransportFactory transportFactory;
	protected final AnnotationProcessorProvider annotationProcessors;



}
