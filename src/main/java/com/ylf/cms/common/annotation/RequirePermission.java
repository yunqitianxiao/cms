package com.ylf.cms.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限校验，由 PermissionAspect 拦截。
 *
 * value 里写多个权限码时默认"任一满足即可"，这样同一个接口可以同时放行
 * article:update:own（作者）和 article:update:any（管理员），
 * 具体的"是不是本人数据"由 Service 再判一次——切面只管"能不能碰这类接口"。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    String[] value();

    Logic logic() default Logic.ANY;

    enum Logic {
        ANY, ALL
    }
}
