package com.mawen.learn.ribbon;


import java.util.concurrent.Future;

import rx.Observable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
public interface RequestWithMetaData<T> {

	Observable<RibbonResponse<Observable<T>>> observe();

	Observable<RibbonResponse<Observable<T>>> obObservable();

	Future<RibbonResponse<T>> queue();

	RibbonResponse<T> execute();
}
