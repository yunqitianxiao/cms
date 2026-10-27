package com.ylf.cms.guide.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GuideChatResponse {

    private String answer;

    private List<GuideCardVO> cards = new ArrayList<>();

    /** RULE / LLM / EMPTY，前端只在调试面板显示 */
    private String source = "RULE";

    /** 是否回答了"系统没有这个功能" */
    private boolean notFound;
}
