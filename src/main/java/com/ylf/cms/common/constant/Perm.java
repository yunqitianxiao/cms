package com.ylf.cms.common.constant;

/**
 * 权限码常量。与 sys_permission.perm_code 一一对应，
 * SiteMapValidator 会在启动时校验注解里写的权限码都存在于这张表，防止写错字。
 */
public final class Perm {

    private Perm() {
    }

    public static final String ARTICLE_READ = "article:read";
    public static final String ARTICLE_CREATE = "article:create";
    public static final String ARTICLE_UPDATE_OWN = "article:update:own";
    public static final String ARTICLE_UPDATE_ANY = "article:update:any";
    public static final String ARTICLE_DELETE_OWN = "article:delete:own";
    public static final String ARTICLE_DELETE_ANY = "article:delete:any";
    public static final String ARTICLE_SUBMIT = "article:submit";
    public static final String ARTICLE_AUDIT = "article:audit";
    public static final String ARTICLE_PUBLISH = "article:publish";

    public static final String CATEGORY_READ = "category:read";
    public static final String CATEGORY_WRITE = "category:write";

    public static final String USER_MANAGE = "user:manage";
    public static final String ROLE_MANAGE = "role:manage";

    public static final String LOG_READ = "log:read";
    public static final String GUIDE_USE = "guide:use";
}
