package com.ylf.cms.common.constant;

public final class SessionKeys {

    private SessionKeys() {
    }

    /** Session 里只存 userId，其余身份信息每请求重查，保证权限变更即时生效 */
    public static final String LOGIN_USER_ID = "CMS_LOGIN_USER_ID";
}
