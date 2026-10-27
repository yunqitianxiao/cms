package com.ylf.cms.guide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个"可引导的界面"。
 *
 * 这个注解是站点地图的唯一结构性来源：SiteMapCollector 在启动后反射扫描
 * 所有 Controller 方法，把注解和真实的映射路径对起来。文案（一句话说明、
 * 操作步骤、提示）放在 resources/guide/site-map.yaml，本注解只管结构。
 *
 * 为什么不把文案也写在注解里：文案要反复改，改一遍就要重新编译；
 * 而且注解里的字符串没有换行，写步骤很别扭。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GuideCard {

    /** 全局唯一，如 article.create；yaml 用它做 key */
    String id();

    /** 卡片标题 */
    String title();

    /** 分组：内容 / 账号与权限 / 系统 / 前台 */
    String group();

    /** 前端页面路径，点卡片跳这里 */
    String route();

    /** 卡片展示顺序，越小越靠前 */
    int order() default 100;

    /** false 表示这是纯数据接口，不出卡片 */
    boolean page() default true;

    /** 检索别名，用户可能用这些词找这个页面 */
    String[] aliases() default {};
}
