package com.ylf.cms.auth;

import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;

/**
 * 当前请求的登录用户。
 *
 * 用 ThreadLocal 而不是把用户塞进方法参数：权限校验发生在切面里，
 * 切面无法改方法的签名，只能旁路取上下文。
 *
 * 清理是必须的——Tomcat 复用线程，不清理会让下一个请求读到上一个用户的身份，
 * 这类 bug 在生产环境表现为"偶发越权"，极难复现。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
