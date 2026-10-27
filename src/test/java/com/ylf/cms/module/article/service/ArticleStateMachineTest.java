package com.ylf.cms.module.article.service;

import com.ylf.cms.common.constant.ArticleAction;
import com.ylf.cms.common.constant.ArticleStatus;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机的每一条边都单独断言。少一条边、连错一条边，这里会红。
 */
class ArticleStateMachineTest {

    @Test
    @DisplayName("七条合法迁移")
    void legalTransitions() {
        assertEquals(ArticleStatus.PENDING, ArticleStateMachine.next(ArticleStatus.DRAFT, ArticleAction.SUBMIT));
        assertEquals(ArticleStatus.PUBLISHED, ArticleStateMachine.next(ArticleStatus.PENDING, ArticleAction.APPROVE));
        assertEquals(ArticleStatus.REJECTED, ArticleStateMachine.next(ArticleStatus.PENDING, ArticleAction.REJECT));
        assertEquals(ArticleStatus.DRAFT, ArticleStateMachine.next(ArticleStatus.PENDING, ArticleAction.WITHDRAW));
        assertEquals(ArticleStatus.OFFLINE, ArticleStateMachine.next(ArticleStatus.PUBLISHED, ArticleAction.OFFLINE));
        assertEquals(ArticleStatus.PENDING, ArticleStateMachine.next(ArticleStatus.OFFLINE, ArticleAction.RESUBMIT));
        assertEquals(ArticleStatus.DRAFT, ArticleStateMachine.next(ArticleStatus.REJECTED, ArticleAction.MODIFY));
    }

    @Test
    @DisplayName("非法迁移抛 40901，且错误信息带当前状态和动作")
    void illegalTransitions() {
        BizException draftApprove = assertThrows(BizException.class,
                () -> ArticleStateMachine.next(ArticleStatus.DRAFT, ArticleAction.APPROVE));
        assertEquals(ResultCode.INVALID_STATE, draftApprove.getResultCode());
        assertTrue(draftApprove.getMessage().contains("草稿"));
        assertTrue(draftApprove.getMessage().contains("审核通过"));

        assertThrows(BizException.class,
                () -> ArticleStateMachine.next(ArticleStatus.PUBLISHED, ArticleAction.MODIFY));
        assertThrows(BizException.class,
                () -> ArticleStateMachine.next(ArticleStatus.OFFLINE, ArticleAction.OFFLINE));
        assertThrows(BizException.class,
                () -> ArticleStateMachine.next(ArticleStatus.REJECTED, ArticleAction.SUBMIT));
    }

    @Test
    @DisplayName("每个状态下可执行的动作集合与预期一致")
    void allowedActions() {
        assertEquals(java.util.EnumSet.of(ArticleAction.SUBMIT),
                ArticleStateMachine.allowedActions(ArticleStatus.DRAFT));
        assertEquals(java.util.EnumSet.of(ArticleAction.APPROVE, ArticleAction.REJECT, ArticleAction.WITHDRAW),
                ArticleStateMachine.allowedActions(ArticleStatus.PENDING));
        assertEquals(java.util.EnumSet.of(ArticleAction.OFFLINE),
                ArticleStateMachine.allowedActions(ArticleStatus.PUBLISHED));
        assertEquals(java.util.EnumSet.of(ArticleAction.MODIFY),
                ArticleStateMachine.allowedActions(ArticleStatus.REJECTED));
        assertEquals(java.util.EnumSet.of(ArticleAction.RESUBMIT),
                ArticleStateMachine.allowedActions(ArticleStatus.OFFLINE));
    }

    @Test
    @DisplayName("只有草稿和已驳回可以删除")
    void deleteRule() {
        assertTrue(ArticleStateMachine.canDelete(ArticleStatus.DRAFT));
        assertTrue(ArticleStateMachine.canDelete(ArticleStatus.REJECTED));
        assertFalse(ArticleStateMachine.canDelete(ArticleStatus.PENDING));
        assertFalse(ArticleStateMachine.canDelete(ArticleStatus.PUBLISHED));
        assertFalse(ArticleStateMachine.canDelete(ArticleStatus.OFFLINE));
    }
}
