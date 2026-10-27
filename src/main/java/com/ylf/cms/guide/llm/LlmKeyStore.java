package com.ylf.cms.guide.llm;

import com.ylf.cms.config.GuideProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * 模型密钥的来源与存放。
 *
 * 用户在引导对话框里填自己的 DeepSeek key 时，只写进**当前会话**：
 * 不落库（避免明文密钥进备份和库导出）、不写前端 localStorage（XSS 能直接读到）、
 * 不进操作日志（MaskUtil 会把 apiKey 字段替换成 ***）。
 * 退出登录时 Session 失效，密钥随之消失。
 *
 * 查找顺序：会话密钥 → 服务端配置（apiKey / DEEPSEEK_API_KEY 环境变量）。
 */
@Component
@RequiredArgsConstructor
public class LlmKeyStore {

    /** Session 属性名。不要复用 SessionKeys 里的登录标识，两者生命周期不同 */
    private static final String SESSION_ATTR = "CMS_GUIDE_LLM_KEY";

    private final GuideProperties properties;

    public Optional<String> effectiveKey() {
        String sessionKey = sessionKey();
        if (StringUtils.hasText(sessionKey)) {
            return Optional.of(sessionKey);
        }
        String configured = properties.getLlm().getApiKey();
        return StringUtils.hasText(configured) ? Optional.of(configured.trim()) : Optional.empty();
    }

    /** USER = 本会话填写；CONFIG = 服务端配置；NONE = 没有 */
    public String source() {
        if (StringUtils.hasText(sessionKey())) {
            return "USER";
        }
        return StringUtils.hasText(properties.getLlm().getApiKey()) ? "CONFIG" : "NONE";
    }

    public void save(String apiKey) {
        HttpSession session = session(true);
        if (session == null) {
            throw new IllegalStateException("没有可用会话");
        }
        session.setAttribute(SESSION_ATTR, apiKey.trim());
    }

    public void clear() {
        HttpSession session = session(false);
        if (session != null) {
            session.removeAttribute(SESSION_ATTR);
        }
    }

    public String sessionKey() {
        HttpSession session = session(false);
        Object value = session == null ? null : session.getAttribute(SESSION_ATTR);
        return value == null ? "" : value.toString();
    }

    /**
     * 只用于界面回显，证明"填过"，而不是把密钥再发回浏览器。
     * 短于 8 位的密钥不显示尾部字符。
     */
    public String keyHint(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        String key = apiKey.trim();
        if (key.length() < 8) {
            return "****";
        }
        return key.substring(0, 3) + "…" + key.substring(key.length() - 4);
    }

    private HttpSession session(boolean create) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest().getSession(create);
    }
}
