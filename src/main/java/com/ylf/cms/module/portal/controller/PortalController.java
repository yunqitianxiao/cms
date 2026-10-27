package com.ylf.cms.module.portal.controller;

import com.ylf.cms.common.result.PageResult;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.module.article.dto.ArticleQuery;
import com.ylf.cms.module.article.service.ArticleService;
import com.ylf.cms.module.article.vo.ArticleVO;
import com.ylf.cms.module.category.service.CategoryService;
import com.ylf.cms.module.category.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台（游客）接口。只读、只出已发布内容，不需要登录，
 * 因此也不挂 @RequirePermission —— 权限注解不在这条链路上起作用。
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    @GetMapping("/articles")
    @GuideCard(id = "portal.list", title = "前台文章列表", group = "前台",
            route = "/portal", order = 90,
            aliases = {"前台", "首页", "读者", "浏览文章", "游客"})
    public Result<PageResult<ArticleVO>> articles(ArticleQuery query) {
        return Result.ok(articleService.portalPage(query));
    }

    @GetMapping("/articles/{id}")
    public Result<ArticleVO> article(@PathVariable Long id) {
        return Result.ok(articleService.portalDetail(id));
    }

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(categoryService.tree(true));
    }
}
