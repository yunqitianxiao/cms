package com.ylf.cms.guide.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ylf.cms.config.GuideProperties;
import com.ylf.cms.guide.model.GuideCardVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * L1 大模型意图通道（可选，默认关闭）。
 *
 * 三条硬约束，缺一不可：
 * 1. 模型只能从"候选卡片 id"里选，不允许生成新的页面；
 * 2. 返回值必须通过白名单校验，不在站点地图里的 id 一律丢弃；
 * 3. 任何异常/超时都静默回落 L0，前端无感。
 *
 * 换句话说：卡片内容永远不经过模型。即使模型幻觉，也幻觉不出一个不存在的按钮。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmIntentRouter {

    private final GuideProperties properties;
    private final LlmKeyStore keyStore;
    private final ObjectMapper objectMapper;

    public Optional<LlmAnswer> route(String message, List<GuideCardVO> candidates) {
        GuideProperties.Llm cfg = properties.getLlm();
        Optional<String> key = keyStore.effectiveKey();
        if (!cfg.isEnabled() || key.isEmpty() || candidates.isEmpty()) {
            return Optional.empty();
        }
        try {
            String content = callModel(message, candidates, key.get());
            return parse(content);
        } catch (Exception e) {
            log.warn("LLM 意图通道失败，回落规则路由: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 连通性探测：给界面上的"测试连接"用。
     * 只问一句极短的话，不消耗多少 token，也不经过站点地图。
     */
    public long probe(String apiKey) throws Exception {
        long start = System.currentTimeMillis();
        callModel("ping", List.of(), apiKey);
        return System.currentTimeMillis() - start;
    }

    /** 把异常转成能显示给用户的一句话；防御性地抹掉可能回显的密钥 */
    public String describeError(Exception e, String apiKey) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (apiKey != null && !apiKey.isBlank()) {
            message = message.replace(apiKey, "***");
        }
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 200 ? message.substring(0, 200) + "…" : message;
    }

    private String callModel(String message, List<GuideCardVO> candidates, String apiKey) throws Exception {
        GuideProperties.Llm cfg = properties.getLlm();

        String cardList = candidates.stream()
                .map(c -> c.getId() + " | " + c.getTitle() + " | " + safe(c.getSummary()))
                .collect(Collectors.joining("\n"));

        String system = "你是站内引导助手。你只能从给定的页面清单中选择页面，不能编造新页面。"
                + "\n页面清单（id | 标题 | 说明）：\n" + cardList
                + "\n请只输出 JSON：{\"answer\":\"一句话回答\",\"cardIds\":[\"id1\"]}"
                + "\n如果清单里没有能完成用户所求的页面，cardIds 返回空数组，"
                + "answer 里明确说明系统没有这个功能。";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModel());
        body.put("temperature", 0);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", message)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) cfg.getTimeoutMs());
        factory.setReadTimeout((int) cfg.getTimeoutMs());
        RestTemplate restTemplate = new RestTemplate(factory);

        String url = cfg.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        String response = restTemplate.postForObject(url,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    private Optional<LlmAnswer> parse(String content) throws Exception {
        String text = content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return Optional.empty();
        }
        JsonNode node = objectMapper.readTree(text.substring(start, end + 1));
        List<String> ids = new ArrayList<>();
        for (JsonNode id : node.path("cardIds")) {
            if (StringUtils.hasText(id.asText())) {
                ids.add(id.asText().trim());
            }
        }
        return Optional.of(new LlmAnswer(node.path("answer").asText(""), ids));
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    @Data
    public static class LlmAnswer {
        private final String answer;
        private final List<String> cardIds;
    }
}
