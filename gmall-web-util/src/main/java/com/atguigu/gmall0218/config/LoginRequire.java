package com.atguigu.gmall0218.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequire {
    //如果是 true 则需要登录，否则不需要登陆
    boolean autoRedirect() default true;
}
