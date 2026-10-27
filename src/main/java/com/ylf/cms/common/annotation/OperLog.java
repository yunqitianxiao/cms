package com.ylf.cms.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志标记，由 LogAspect 拦截并写入 sys_log。
 * 只标注"有副作用"的接口；查询接口不记，否则日志表会被读操作淹没。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 日志分类，对应 LogType */
    String type();

    /** 人可读的操作名，如"新建文章" */
    String value();
}
