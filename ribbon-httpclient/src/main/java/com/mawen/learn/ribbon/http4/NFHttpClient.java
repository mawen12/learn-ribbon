package com.mawen.learn.ribbon.http4;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/29
 */
@Slf4j
public class NFHttpClient extends DefaultHttpClient {

	protected static final String EXECUTE_TRACE = "HttpClient-ExecuteTimer";
}
