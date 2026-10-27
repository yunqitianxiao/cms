package com.ylf.cms.module.article.controller;

import com.ylf.cms.common.annotation.OperLog;
import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.common.constant.LogType;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.result.PageResult;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.module.article.dto.ArticleBatchRequest;
import com.ylf.cms.module.article.dto.ArticleQuery;
import com.ylf.cms.module.article.dto.ArticleSaveRequest;
import com.ylf.cms.module.article.service.ArticleService;
import com.ylf.cms.module.article.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    @RequirePermission(Perm.ARTICLE_READ)
    @GuideCard(id = "article.list", title = "文章列表", group = "内容",
            route = "/app/articles", order = 20,
            aliases = {"文章", "稿件", "内容", "稿件列表", "我的文章", "批量", "下架"})
    public Result<PageResult<ArticleVO>> page(ArticleQuery query) {
        return Result.ok(articleService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePermission(Perm.ARTICLE_READ)
    public Result<ArticleVO> detail(@PathVariable Long id) {
        return Result.ok(articleService.detail(id));
    }

    @PostMapping
    @RequirePermission(Perm.ARTICLE_CREATE)
    @OperLog(type = LogType.ARTICLE, value = "新建文章")
    @GuideCard(id = "article.create", title = "写新文章", group = "内容",
            route = "/app/article-edit", order = 10,
            aliases = {"写文章", "发布", "投稿", "新建", "起草", "发稿", "写稿"})
    public Result<Long> create(@Valid @RequestBody ArticleSaveRequest request) {
        return Result.ok(articleService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission({Perm.ARTICLE_UPDATE_OWN, Perm.ARTICLE_UPDATE_ANY})
    @OperLog(type = LogType.ARTICLE, value = "编辑文章")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleSaveRequest request) {
        articleService.update(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    @RequirePermission({Perm.ARTICLE_SUBMIT, Perm.ARTICLE_UPDATE_ANY})
    @OperLog(type = LogType.ARTICLE, value = "提交审核")
    public Result<Void> submit(@PathVariable Long id) {
        articleService.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/withdraw")
    @RequirePermission({Perm.ARTICLE_SUBMIT, Perm.ARTICLE_UPDATE_ANY})
    @OperLog(type = LogType.ARTICLE, value = "撤回审核")
    public Result<Void> withdraw(@PathVariable Long id) {
        articleService.withdraw(id);
        return Result.ok();
    }

    @PostMapping("/{id}/resubmit")
    @RequirePermission({Perm.ARTICLE_SUBMIT, Perm.ARTICLE_UPDATE_ANY})
    @OperLog(type = LogType.ARTICLE, value = "重新提交审核")
    public Result<Void> resubmit(@PathVariable Long id) {
        articleService.resubmit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    @RequirePermission(Perm.ARTICLE_AUDIT)
    @OperLog(type = LogType.AUDIT, value = "审核通过")
    @GuideCard(id = "article.review", title = "内容审核", group = "内容",
            route = "/app/review", order = 25,
            aliases = {"审核", "通过", "驳回", "待审核", "审稿", "过审"})
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody(required = false) ArticleBatchRequest request) {
        articleService.approve(id, request == null ? null : request.getOpinion());
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    @RequirePermission(Perm.ARTICLE_AUDIT)
    @OperLog(type = LogType.AUDIT, value = "审核驳回")
    public Result<Void> reject(@PathVariable Long id, @RequestBody ArticleBatchRequest request) {
        articleService.reject(id, request.getOpinion());
        return Result.ok();
    }

    @PostMapping("/{id}/offline")
    @RequirePermission(Perm.ARTICLE_PUBLISH)
    @OperLog(type = LogType.ARTICLE, value = "下架文章")
    public Result<Void> offline(@PathVariable Long id, @RequestBody ArticleBatchRequest request) {
        articleService.offline(id, request.getReason());
        return Result.ok();
    }

    @PostMapping("/batch/approve")
    @RequirePermission(Perm.ARTICLE_PUBLISH)
    @OperLog(type = LogType.AUDIT, value = "批量审核通过")
    public Result<Map<String, Object>> batchApprove(@Valid @RequestBody ArticleBatchRequest request) {
        return Result.ok(articleService.batchApprove(request));
    }

    @PostMapping("/batch/offline")
    @RequirePermission(Perm.ARTICLE_PUBLISH)
    @OperLog(type = LogType.ARTICLE, value = "批量下架")
    public Result<Map<String, Object>> batchOffline(@Valid @RequestBody ArticleBatchRequest request) {
        return Result.ok(articleService.batchOffline(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission({Perm.ARTICLE_DELETE_OWN, Perm.ARTICLE_DELETE_ANY})
    @OperLog(type = LogType.ARTICLE, value = "删除文章")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.ok();
    }
}
