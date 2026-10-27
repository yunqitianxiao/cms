package com.ylf.cms.module.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ylf.cms.module.article.dto.ArticleQuery;
import com.ylf.cms.module.article.entity.CmsArticle;
import com.ylf.cms.module.article.vo.ArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CmsArticleMapper extends BaseMapper<CmsArticle> {

    /**
     * 列表一次 join 出分类名和作者名，避免"先查文章再逐条查分类/用户"的 N+1。
     * 自定义 SQL 不会被逻辑删除插件自动加条件，deleted = 0 要自己写。
     */
    IPage<ArticleVO> selectPageVO(IPage<ArticleVO> page, @Param("q") ArticleQuery query);

    ArticleVO selectDetailById(@Param("id") Long id);
}
