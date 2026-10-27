package com.ylf.cms.guide.service;

import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.config.GuideProperties;
import com.ylf.cms.guide.llm.LlmIntentRouter;
import com.ylf.cms.guide.llm.LlmKeyStore;
import com.ylf.cms.guide.model.LlmSettingsVO;
import com.ylf.cms.guide.model.LlmTestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 模型通道的设置面：查看状态、保存/清除密钥、连通性探测。
 *
 * 密钥只在当前会话有效（见 {@link LlmKeyStore}）。保存前做最小的格式校验：
 * 只拒绝明显不对的输入（空、过短、含空白），不校验 sk- 前缀——
 * 换成任何 OpenAI 兼容端点时前缀并不统一，硬校验会把合法 key 挡在外面。
 */
@Service
@RequiredArgsConstructor
public class GuideLlmService {

    private static final int MIN_KEY_LENGTH = 8;

    private final GuideProperties properties;
    private final LlmKeyStore keyStore;
    private final LlmIntentRouter llmIntentRouter;

    public LlmSettingsVO settings() {
        GuideProperties.Llm cfg = properties.getLlm();
        LlmSettingsVO vo = new LlmSettingsVO();
        vo.setEnabled(cfg.isEnabled());
        Optional<String> key = keyStore.effectiveKey();
        vo.setConfigured(cfg.isEnabled() && key.isPresent());
        vo.setSource(keyStore.source());
        vo.setKeyHint(key.map(keyStore::keyHint).orElse(""));
        vo.setModel(cfg.getModel());
        vo.setBaseUrl(cfg.getBaseUrl());
        return vo;
    }

    public LlmSettingsVO save(String apiKey) {
        String cleaned = apiKey == null ? "" : apiKey.trim();
        if (cleaned.length() < MIN_KEY_LENGTH) {
            throw new BizException(ResultCode.BAD_REQUEST, "密钥长度不足，请粘贴完整的 API Key");
        }
        if (cleaned.length() > 200) {
            throw new BizException(ResultCode.BAD_REQUEST, "密钥过长，请确认没有多粘贴内容");
        }
        if (!cleaned.equals(cleaned.replaceAll("\\s", ""))) {
            throw new BizException(ResultCode.BAD_REQUEST, "密钥里不能有空格或换行");
        }
        keyStore.save(cleaned);
        return settings();
    }

    public LlmSettingsVO clear() {
        keyStore.clear();
        return settings();
    }

    public LlmTestResult test() {
        GuideProperties.Llm cfg = properties.getLlm();
        if (!cfg.isEnabled()) {
            return new LlmTestResult(false, 0, "模型通道已被服务端关闭");
        }
        Optional<String> key = keyStore.effectiveKey();
        if (key.isEmpty()) {
            return new LlmTestResult(false, 0, "还没有配置密钥");
        }
        if (!StringUtils.hasText(cfg.getBaseUrl())) {
            return new LlmTestResult(false, 0, "服务端没有配置 base-url");
        }
        try {
            long latency = llmIntentRouter.probe(key.get());
            return new LlmTestResult(true, latency, "连接成功，模型 " + cfg.getModel() + " 可用");
        } catch (Exception e) {
            return new LlmTestResult(false, 0, "连接失败：" + llmIntentRouter.describeError(e, key.get()));
        }
    }
}
