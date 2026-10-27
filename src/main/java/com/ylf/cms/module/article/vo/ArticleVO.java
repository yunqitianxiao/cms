package com.ylf.cms.module.article.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleVO {

    private Long id;
    private String title;
    private String summary;
    /** 仅详情接口返回，列表查询不查正文 */
    private String content;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private String status;
    private String statusLabel;
    private String auditOpinion;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer viewCount;
    private Integer version;
}
