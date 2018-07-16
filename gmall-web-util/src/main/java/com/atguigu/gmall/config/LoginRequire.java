package com.atguigu.gmall.config;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义的一个注解类
 */
//添加位置为方法
@Target(ElementType.METHOD)
//作用范围是运行时
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequire {
    // 设置一个默认值 ture时，需要登录
    boolean autoRedirect() default true;
}
