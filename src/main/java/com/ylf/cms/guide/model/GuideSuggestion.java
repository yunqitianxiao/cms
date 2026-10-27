package com.ylf.cms.guide.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GuideSuggestion {

    private String text;

    private String cardId;

    /** INFO / WARN */
    private String level;

    /** 建议附带的数字，前端可显示成徽标 */
    private Long count;
}
