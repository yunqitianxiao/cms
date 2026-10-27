package com.ylf.cms.common.constant;

public final class RoleCode {

    private RoleCode() {
    }

    public static final String GUEST = "GUEST";
    public static final String AUTHOR = "AUTHOR";
    public static final String AUDITOR = "AUDITOR";
    public static final String ADMIN = "ADMIN";

    /** 新注册用户默认角色 */
    public static final String DEFAULT = AUTHOR;
}
