package com.ylf.cms.common.util;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public final class IpUtil {

    private IpUtil() {
    }

    /**
     * 取真实客户端 IP。X-Forwarded-For 是可伪造的，仅用于审计参考，
     * 绝不能用于任何安全判定。
     */
    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
