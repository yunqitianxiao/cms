package com.ylf.cms.guide.service;

import com.ylf.cms.auth.LoginUser;
import com.ylf.cms.common.constant.ArticleStatus;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.guide.model.GuideSuggestion;
import com.ylf.cms.module.article.service.ArticleService;
import com.ylf.cms.module.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 主动建议。全部来自 SQL 计数 + 规则，不经过模型——
 * 建议里出现的每个数字都能在数据库里查到，答不上"这个 3 是怎么来的"就是设计失误。
 */
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    public List<GuideSuggestion> forUser(LoginUser user) {
        List<GuideSuggestion> list = new ArrayList<>();

        long rejected = articleService.countByAuthorAndStatus(user.getId(), ArticleStatus.REJECTED);
        if (rejected > 0) {
            list.add(new GuideSuggestion(
                    "你有 " + rejected + " 篇文章被驳回，修改后可重新提交审核", "article.list", "WARN", rejected));
        }

        if (user.hasPermission(Perm.ARTICLE_AUDIT)) {
            long pending = articleService.countByStatus(ArticleStatus.PENDING);
            if (pending > 0) {
                list.add(new GuideSuggestion(
                        "有 " + pending + " 篇文章在等审核", "article.review", "INFO", pending));
            }
        }

        long draft = articleService.countByAuthorAndStatus(user.getId(), ArticleStatus.DRAFT);
        if (draft > 0 && user.hasPermission(Perm.ARTICLE_CREATE)) {
            list.add(new GuideSuggestion(
                    "你有 " + draft + " 篇草稿还没提交审核", "article.list", "INFO", draft));
        }

        if (user.hasPermission(Perm.ARTICLE_CREATE) && articleService.countByAuthor(user.getId()) == 0) {
            list.add(new GuideSuggestion("你还没有发过文章，从写第一篇开始？", "article.create", "INFO", null));
        }

        if (user.hasPermission(Perm.CATEGORY_WRITE) && categoryService.listFlat().isEmpty()) {
            list.add(new GuideSuggestion("系统还没有分类，先建分类才能写文章", "category.manage", "INFO", null));
        }

        if (list.isEmpty()) {
            list.add(new GuideSuggestion("你可以问我任何功能怎么用，比如「怎么发布文章」", null, "INFO", null));
        }
        return list;
    }
}
