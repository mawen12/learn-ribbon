package com.mawen.learn.ribbon.http;

import com.mawen.learn.ribbon.ResponseValidator;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public interface HttpResponseValidator extends ResponseValidator<HttpClientResponse<ByteBuf>> {

}
