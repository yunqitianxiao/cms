package com.ylf.cms.module.article.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ArticleBatchRequest {

    @NotNull(message = "请选择文章")
    private List<Long> ids;

    /** 驳回必须填，通过可空 */
    private String opinion;

    /** 下架必须填 */
    private String reason;
}
