package com.ylf.cms.guide.service;

import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.config.GuideProperties;
import com.ylf.cms.guide.llm.LlmKeyStore;
import com.ylf.cms.guide.model.LlmSettingsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密钥的三种来源与格式校验。
 *
 * 这里断言的是"会话密钥覆盖服务端配置、清除后回落"这类状态组合——
 * 只测"保存能读出来"会漏掉回落分支，而回落正是用户清除密钥后期望的行为。
 */
class GuideLlmServiceTest {

    private GuideProperties properties;
    private LlmKeyStore keyStore;
    private GuideLlmService service;

    @BeforeEach
    void setUp() {
        properties = new GuideProperties();
        properties.getLlm().setApiKey("");
        keyStore = new LlmKeyStore(properties);
        service = new GuideLlmService(properties, keyStore, null);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 没有会话密钥也没有服务端密钥时是规则模式() {
        LlmSettingsVO vo = service.settings();
        assertTrue(vo.isEnabled());
        assertFalse(vo.isConfigured());
        assertEquals("NONE", vo.getSource());
        assertEquals("", vo.getKeyHint());
    }

    @Test
    void 保存后只回显尾号且来源为USER() {
        LlmSettingsVO vo = service.save("sk-0123456789abcdef");
        assertTrue(vo.isConfigured());
        assertEquals("USER", vo.getSource());
        assertEquals("sk-…cdef", vo.getKeyHint());
        // 接口对象里根本没有 key 字段，这里取不到完整密钥
        assertEquals("sk-0123456789abcdef", keyStore.sessionKey());
    }

    @Test
    void 清除会话密钥后回落到服务端配置() {
        properties.getLlm().setApiKey("sk-config-0000111122223333");
        service.save("sk-user-4444555566667777");
        assertEquals("USER", service.settings().getSource());

        LlmSettingsVO after = service.clear();
        assertEquals("CONFIG", after.getSource());
        assertTrue(after.isConfigured());
        assertEquals("sk-…3333", after.getKeyHint());
    }

    @Test
    void 空短含空白的密钥都被拒绝() {
        assertCode(service, "", ResultCode.BAD_REQUEST);
        assertCode(service, "   ", ResultCode.BAD_REQUEST);
        assertCode(service, "sk-1", ResultCode.BAD_REQUEST);
        assertCode(service, "sk-1234 5678", ResultCode.BAD_REQUEST);
        // 合法密钥大小写、连字符都允许
        assertTrue(service.save("sk-AbC_123-456789").isConfigured());
    }

    @Test
    void 过短的密钥尾号不泄露字符() {
        assertEquals("****", keyStore.keyHint("abc"));
        assertEquals("", keyStore.keyHint(" "));
    }

    private void assertCode(GuideLlmService target, String key, ResultCode expected) {
        BizException e = assertThrows(BizException.class, () -> target.save(key));
        assertEquals(expected.getCode(), e.getResultCode().getCode());
    }
}
