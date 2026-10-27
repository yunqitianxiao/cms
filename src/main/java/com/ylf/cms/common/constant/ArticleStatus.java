package com.ylf.cms.common.constant;

/**
 * 文章状态。所有状态迁移必须经过 ArticleStateMachine，禁止业务代码直接 setStatus。
 */
public enum ArticleStatus {

    DRAFT("草稿"),
    PENDING("待审核"),
    PUBLISHED("已发布"),
    REJECTED("已驳回"),
    OFFLINE("已下架");

    private final String label;

    ArticleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ArticleStatus of(String name) {
        for (ArticleStatus s : values()) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }
}
