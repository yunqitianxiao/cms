package com.ylf.cms.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 日志参数脱敏。
 *
 * 说明一个容易忽略的点：脱敏不能只在"密码字段"上做。
 * 用户可能把口令写进 nickname、或把 API key 写进文章摘要，
 * 所以这里按"字段名关键字"匹配，并且截断超长参数，避免日志表被大字段撑爆。
 */
public final class MaskUtil {

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "oldPassword", "newPassword", "confirmPassword",
            "token", "apiKey", "api-key", "secret", "credential"
    ));

    private static final int MAX_LENGTH = 1500;

    private MaskUtil() {
    }

    public static String mask(Object[] args, ObjectMapper mapper) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (isInfrastructureBean(arg)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(toJson(arg, mapper));
        }
        String s = sb.toString();
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH) + "...(截断)" : s;
    }

    /**
     * Servlet 对象序列化会爆炸，直接跳过。
     * 不能只看类名前缀：容器传入的实例是 org.apache.catalina.connector.RequestFacade，
     * 包名一个都不沾 javax.servlet，却是标准的 HttpServletRequest。
     */
    private static boolean isInfrastructureBean(Object arg) {
        if (arg instanceof javax.servlet.ServletRequest || arg instanceof javax.servlet.ServletResponse) {
            return true;
        }
        String name = arg.getClass().getName();
        return name.startsWith("javax.servlet") || name.startsWith("jakarta.servlet")
                || name.startsWith("org.springframework.web")
                || name.startsWith("org.springframework.ui");
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object arg, ObjectMapper mapper) {
        try {
            Object node = mapper.convertValue(arg, Object.class);
            if (node instanceof Map) {
                ((Map<String, Object>) node).forEach((k, v) -> {
                    if (SENSITIVE_KEYS.contains(k)) {
                        ((Map<String, Object>) node).put(k, "***");
                    }
                });
            }
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return StringUtils.truncate(String.valueOf(arg), 200).toString();
        }
    }
}
