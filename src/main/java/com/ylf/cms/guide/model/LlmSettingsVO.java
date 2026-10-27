package com.ylf.cms.guide.model;

import lombok.Data;

/**
 * 模型通道状态。注意这里**没有 key 字段**：
 * 界面只需要知道"配没配、从哪来的、尾号是多少"，拿不到完整密钥。
 */
@Data
public class LlmSettingsVO {

    /** 服务端总开关 */
    private boolean enabled;
    /** 当前是否会走模型（开关打开且存在密钥） */
    private boolean configured;
    /** USER 本会话填写 / CONFIG 服务端配置 / NONE */
    private String source;
    /** 形如 sk-…a1b2，仅用于确认"填的是哪一把" */
    private String keyHint;
    private String model;
    private String baseUrl;
}
