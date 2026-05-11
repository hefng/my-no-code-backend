package com.hefng.mynocodebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Redis key 前缀
     */
    String keyPrefix();

    /**
     * 时间窗口
     */
    long time();

    /**
     * 时间单位
     */
    TimeUnit unit();

    /**
     * 时间窗口内允许请求次数
     */
    long count();
}
