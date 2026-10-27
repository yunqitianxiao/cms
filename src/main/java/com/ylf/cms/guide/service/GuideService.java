package com.ylf.cms.guide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ylf.cms.auth.LoginUser;
import com.ylf.cms.auth.UserContext;
import com.ylf.cms.common.constant.LogType;
import com.ylf.cms.guide.intent.IntentRouter;
import com.ylf.cms.guide.llm.LlmIntentRouter;
import com.ylf.cms.guide.model.GuideCardVO;
import com.ylf.cms.guide.model.GuideChatResponse;
import com.ylf.cms.guide.model.GuideSuggestion;
import com.ylf.cms.guide.sitemap.SiteMapHolder;
import com.ylf.cms.module.log.entity.SysLog;
import com.ylf.cms.module.log.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideService {

    private static final int MAX_QUESTION_ECHO = 20;

    private final SiteMapHolder siteMapHolder;
    private final IntentRouter intentRouter;
    private final LlmIntentRouter llmIntentRouter;
    private final SuggestionService suggestionService;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    public List<GuideCardVO> pages() {
        return decorateAll(UserContext.require());
    }

    public List<GuideSuggestion> suggestions() {
        return suggestionService.forUser(UserContext.require());
    }

    public GuideChatResponse chat(String message) {
        LoginUser user = UserContext.require();
        long start = System.currentTimeMillis();

        List<GuideCardVO> all = decorateAll(user);
        List<GuideCardVO> hits = intentRouter.route(message, all);
        String source = "RULE";
        String modelAnswer = null;

        // 规则没命中才问模型：能本地解决的问题不必增加一次网络往返和一份不确定性
        if (hits.isEmpty()) {
            Optional<LlmIntentRouter.LlmAnswer> llm = llmIntentRouter.route(message, all);
            if (llm.isPresent() && !llm.get().getCardIds().isEmpty()) {
                List<GuideCardVO> picked = pickByIds(all, llm.get().getCardIds());
                if (!picked.isEmpty()) {
                    hits = picked;
                    source = "LLM";
                    modelAnswer = llm.get().getAnswer();
                }
            }
        }

        GuideChatResponse response = new GuideChatResponse();
        response.setSource(source);
        response.setCards(hits);
        if (hits.isEmpty()) {
            response.setNotFound(true);
            response.setAnswer(notFoundAnswer(message));
        } else {
            response.setAnswer(modelAnswer != null && !modelAnswer.isBlank()
                    ? modelAnswer : buildAnswer(hits));
        }

        recordQuery(user, message, hits, source, start);
        return response;
    }

    public Map<String, Object> selfcheck() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cardCount", siteMapHolder.cards().size());
        data.put("uncoveredPermittedMethods", siteMapHolder.snapshot().getUncoveredPermittedMethods());
        data.put("duplicateIds", siteMapHolder.snapshot().getDuplicateIds());
        data.put("cards", siteMapHolder.cards().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("route", c.getRoute());
            m.put("api", c.getHttpMethod() + " " + c.getApiPath());
            m.put("permissions", c.getPermissions());
            return m;
        }).collect(Collectors.toList()));
        return data;
    }

    /**
     * 按当前用户权限给卡片打标。
     *
     * 无权限的卡片不隐藏而是置灰：用户需要知道"这件事存在，只是我做不了"，
     * 否则会反复尝试别的方式。但卡片里没有任何业务数据，跳转仍要过权限拦截。
     */
    private List<GuideCardVO> decorateAll(LoginUser user) {
        Map<String, String> permNames = siteMapHolder.permissionNames();
        List<GuideCardVO> result = new ArrayList<>();
        for (GuideCardVO source : siteMapHolder.cards()) {
            GuideCardVO copy = copyOf(source);
            boolean accessible = source.getPermissions().isEmpty()
                    || user.hasAnyPermission(source.getPermissions().toArray(new String[0]));
            copy.setAccessible(accessible);
            if (!accessible) {
                String missing = source.getPermissions().stream()
                        .map(code -> permNames.getOrDefault(code, code))
                        .collect(Collectors.joining(" / "));
                copy.setDenyHint("需要「" + missing + "」权限");
            }
            result.add(copy);
        }
        return result;
    }

    private GuideCardVO copyOf(GuideCardVO s) {
        GuideCardVO c = new GuideCardVO();
        c.setId(s.getId());
        c.setTitle(s.getTitle());
        c.setGroup(s.getGroup());
        c.setRoute(s.getRoute());
        c.setSummary(s.getSummary());
        c.setSteps(new ArrayList<>(s.getSteps()));
        c.setTips(new ArrayList<>(s.getTips()));
        c.setAliases(new ArrayList<>(s.getAliases()));
        c.setHighlight(s.getHighlight());
        c.setPermissions(new java.util.LinkedHashSet<>(s.getPermissions()));
        c.setApiPath(s.getApiPath());
        c.setHttpMethod(s.getHttpMethod());
        c.setOrder(s.getOrder());
        return c;
    }

    /** 模型给回来的 id 必须在站点地图里，且去重 */
    private List<GuideCardVO> pickByIds(List<GuideCardVO> all, List<String> ids) {
        Map<String, GuideCardVO> byId = all.stream()
                .collect(Collectors.toMap(GuideCardVO::getId, c -> c));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private String buildAnswer(List<GuideCardVO> hits) {
        GuideCardVO top = hits.get(0);
        if (!top.isAccessible()) {
            return "「" + top.getTitle() + "」" + top.getDenyHint()
                    + "，你当前的账号做不了这件事。可以联系管理员开通，或者先看看下面这些你能用的入口：";
        }
        if (hits.size() > 1) {
            return "这几个页面可能和你要做的事有关，点卡片可以直接过去：";
        }
        String summary = top.getSummary() == null ? "" : "——" + top.getSummary();
        return "「" + top.getTitle() + "」" + summary
                + "。点开卡片可以看到具体操作步骤。";
    }

    private String notFoundAnswer(String message) {
        String echo = message.length() > MAX_QUESTION_ECHO
                ? message.substring(0, MAX_QUESTION_ECHO) + "…" : message;
        return "系统里没有「" + echo + "」这个功能，我不会编一个不存在的按钮给你。"
                + "你可以换个说法，或者看看下面这些入口。";
    }

    /**
     * 记一条 AI_QUERY 日志，但不记用户原话：
     * 用户可能输入了人名、手机号之类的信息，引导日志没有留存原文的必要。
     */
    private void recordQuery(LoginUser user, String message, List<GuideCardVO> hits, String source, long start) {
        try {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("hitCards", hits.stream().map(GuideCardVO::getId).collect(Collectors.toList()));
            detail.put("source", source);
            detail.put("length", message.length());

            SysLog entry = new SysLog();
            entry.setUserId(user.getId());
            entry.setUsername(user.getUsername());
            entry.setLogType(LogType.AI_QUERY);
            entry.setOperation("站内引导提问");
            entry.setUri("/api/guide/chat");
            entry.setHttpMethod("POST");
            entry.setParams(objectMapper.writeValueAsString(detail));
            entry.setResult("SUCCESS");
            entry.setCostMs(System.currentTimeMillis() - start);
            logService.record(entry);
        } catch (Exception e) {
            log.warn("记录引导日志失败: {}", e.getMessage());
        }
    }
}
