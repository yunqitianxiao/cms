package com.ylf.cms.guide.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 返回给前端的卡片。
 *
 * 注意这里没有任何业务数据：卡片只知道"有哪些页面、要什么权限、怎么用"。
 * 用户想看的真实数据必须点进页面，由页面自己走权限校验去取。
 */
@Data
public class GuideCardVO {

    private String id;
    private String title;
    private String group;
    private String route;
    private String summary;
    private List<String> steps = new ArrayList<>();
    private List<String> tips = new ArrayList<>();
    private List<String> aliases = new ArrayList<>();
    /** 跳转后要高亮的元素选择器 */
    private String highlight;
    /** 访问这个页面需要的权限码（任一即可） */
    private Set<String> permissions = new LinkedHashSet<>();
    /** 后端真实接口，仅用于"这卡背后是哪条接口"的展示 */
    private String apiPath;
    private String httpMethod;
    private int order;
    /** 当前用户是否有权访问 */
    private boolean accessible = true;
    /** 无权限时的说明，告诉用户缺什么 */
    private String denyHint;
}
