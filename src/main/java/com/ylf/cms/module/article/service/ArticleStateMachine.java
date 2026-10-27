package com.ylf.cms.module.article.service;

import com.ylf.cms.common.constant.ArticleAction;
import com.ylf.cms.common.constant.ArticleStatus;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 文章状态机。
 *
 * 把"哪条边可以走"集中在一张表里，而不是散落在 Service 的 if 里：
 * ① 加一条边只改一个地方；② 非法迁移能同时报出"当前状态"和"被拒绝的动作"；
 * ③ 这张表可以直接被单元测试逐条覆盖（9 条边 + 若干非法组合）。
 */
public final class ArticleStateMachine {

    private static final Map<ArticleAction, Map<ArticleStatus, ArticleStatus>> TRANSITIONS =
            new EnumMap<>(ArticleAction.class);

    private static final Map<ArticleStatus, Set<ArticleAction>> ALLOWED =
            new EnumMap<>(ArticleStatus.class);

    static {
        edge(ArticleAction.SUBMIT, ArticleStatus.DRAFT, ArticleStatus.PENDING);
        edge(ArticleAction.APPROVE, ArticleStatus.PENDING, ArticleStatus.PUBLISHED);
        edge(ArticleAction.REJECT, ArticleStatus.PENDING, ArticleStatus.REJECTED);
        edge(ArticleAction.WITHDRAW, ArticleStatus.PENDING, ArticleStatus.DRAFT);
        edge(ArticleAction.OFFLINE, ArticleStatus.PUBLISHED, ArticleStatus.OFFLINE);
        edge(ArticleAction.RESUBMIT, ArticleStatus.OFFLINE, ArticleStatus.PENDING);
        edge(ArticleAction.MODIFY, ArticleStatus.REJECTED, ArticleStatus.DRAFT);

        for (Map.Entry<ArticleAction, Map<ArticleStatus, ArticleStatus>> e : TRANSITIONS.entrySet()) {
            for (ArticleStatus from : e.getValue().keySet()) {
                ALLOWED.computeIfAbsent(from, k -> EnumSet.noneOf(ArticleAction.class)).add(e.getKey());
            }
        }
    }

    private ArticleStateMachine() {
    }

    private static void edge(ArticleAction action, ArticleStatus from, ArticleStatus to) {
        TRANSITIONS.computeIfAbsent(action, k -> new EnumMap<>(ArticleStatus.class)).put(from, to);
    }

    public static ArticleStatus next(ArticleStatus current, ArticleAction action) {
        Map<ArticleStatus, ArticleStatus> byFrom = TRANSITIONS.get(action);
        ArticleStatus target = byFrom == null ? null : byFrom.get(current);
        if (target == null) {
            throw new BizException(ResultCode.INVALID_STATE,
                    String.format("当前状态【%s】不能执行【%s】", current.getLabel(), action.getLabel()));
        }
        return target;
    }

    public static Set<ArticleAction> allowedActions(ArticleStatus current) {
        return ALLOWED.getOrDefault(current, Collections.emptySet());
    }

    /** 删除是状态机之外的规则：已发布/已下架的文章必须先回到草稿，不能直接删 */
    public static boolean canDelete(ArticleStatus current) {
        return current == ArticleStatus.DRAFT || current == ArticleStatus.REJECTED;
    }
}
