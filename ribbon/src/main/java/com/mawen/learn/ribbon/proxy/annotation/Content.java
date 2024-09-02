package com.mawen.learn.ribbon.proxy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.nashorn.internal.ir.annotations.Reference;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/9/2
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Content {
}
