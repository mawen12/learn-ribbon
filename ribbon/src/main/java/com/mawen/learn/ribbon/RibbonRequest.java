package com.mawen.learn.ribbon;

import java.util.concurrent.Future;

import rx.Observable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public interface RibbonRequest<T> {

	T execute();

	Future<T> queue();

	Observable<T> observe();

	Observable<T> toObservable();

	RequestWithMetaData<T> withMetadata();
}
