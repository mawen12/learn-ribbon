package com.mawen.learn.ribbon.loadbalancer.reactive;

import com.mawen.learn.ribbon.loadbalancer.Server;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/8/28
 */
public interface ServerOperation<T> extends Func1<Server, Observable<T>> {

	Observable<T> call(Server server);
}
