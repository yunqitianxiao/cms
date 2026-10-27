package com.ylf.cms.common.constant;

/**
 * 文章状态迁移动作。动作是"用户意图"，状态是"系统结果"，
 * 分开定义才能让非法迁移报错时告诉用户"你现在的状态做不了这个动作"。
 */
public enum ArticleAction {

    SUBMIT("提交审核"),
    APPROVE("审核通过"),
    REJECT("驳回"),
    WITHDRAW("撤回"),
    OFFLINE("下架"),
    RESUBMIT("重新提交"),
    MODIFY("修改");

    private final String label;

    ArticleAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
