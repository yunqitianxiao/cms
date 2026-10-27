package com.ylf.cms.module.article.dto;

import com.ylf.cms.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleQuery extends PageQuery {

    /** 关键词：标题或摘要包含 */
    private String keyword;

    private Long categoryId;

    /** 由 Service 展开成"该分类及其子分类"的 id 列表 */
    private List<Long> categoryIds;

    private String status;

    private Long authorId;

    /** 只看我的文章 */
    private Boolean mine;

    /** 排序入参，形如 "createTime,desc;title,asc"，经白名单转换后才可使用 */
    private String sort;

    /** 由 Service 写入的物理 ORDER BY 片段，禁止直接来自客户端 */
    private String orderBy;
}
