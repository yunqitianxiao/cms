package com.ylf.cms.module.article.controller;

import com.ylf.cms.auth.UserContext;
import com.ylf.cms.common.constant.ArticleStatus;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.module.article.service.ArticleService;
import com.ylf.cms.module.category.service.CategoryService;
import com.ylf.cms.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作台数据。数字全部来自 SQL 计数，智能体的"主动建议"复用的也是这一套口径，
 * 不做第二份统计逻辑——两份口径迟早会对不上。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final SysUserMapper userMapper;

    @GetMapping("/stats")
    @GuideCard(id = "dashboard", title = "工作台", group = "系统",
            route = "/app/dashboard", order = 1,
            aliases = {"首页", "工作台", "概览", "统计", "多少篇"})
    public Result<Map<String, Object>> stats() {
        Long me = UserContext.require().getId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("myDraft", articleService.countByAuthorAndStatus(me, ArticleStatus.DRAFT));
        data.put("myPending", articleService.countByAuthorAndStatus(me, ArticleStatus.PENDING));
        data.put("myRejected", articleService.countByAuthorAndStatus(me, ArticleStatus.REJECTED));
        data.put("myPublished", articleService.countByAuthorAndStatus(me, ArticleStatus.PUBLISHED));
        data.put("pendingTotal", articleService.countByStatus(ArticleStatus.PENDING));
        data.put("publishedTotal", articleService.countByStatus(ArticleStatus.PUBLISHED));
        data.put("categoryCount", categoryService.listFlat().size());
        data.put("userCount", userMapper.selectCount(null));
        data.put("permissions", UserContext.require().getPermissions());
        return Result.ok(data);
    }
}
