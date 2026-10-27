package com.ylf.cms.module.article.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ArticleSaveRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字")
    private String title;

    @Size(max = 500, message = "摘要最长 500 字")
    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    /** 乐观锁版本号，编辑页回传；新文章可为空 */
    private Integer version;
}
