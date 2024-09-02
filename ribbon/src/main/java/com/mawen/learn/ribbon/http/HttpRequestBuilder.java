package com.mawen.learn.ribbon.http;

import java.util.Map;

import com.mawen.learn.ribbon.RequestTemplate.RequestBuilder;
import com.mawen.learn.ribbon.RibbonRequest;
import com.mawen.learn.ribbon.template.ParsedTemplate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.reactivex.netty.channel.ContentTransformer;
import rx.Observable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public class HttpRequestBuilder<T> extends RequestBuilder<T> {

	private static final ContentTransformer<ByteBuf> passThroughContentTransformer = new ContentTransformer<ByteBuf>() {
		@Override
		public ByteBuf call(ByteBuf byteBuf, ByteBufAllocator byteBufAllocator) {
			return byteBuf;
		}
	};

	private final HttpRequestTemplate<T> requestTemplate;
	private final Map<String, Object> vars;
	private final ParsedTemplate parsedUriTemplate;
	private Observable rawContentSource;
	private ContentTransformer contentTransformer;

	HttpRequestBuilder(HttpRequestTemplate<T> requestTemplate) {
		this.requestTemplate = requestTemplate;
		this.parsedUriTemplate = requestTemplate.uriTemplate();
	}

	@Override
	public RequestBuilder<T> withRequestProperty(String key, Object value) {
		return null;
	}

	@Override
	public RibbonRequest<T> build() {
		return null;
	}
}
