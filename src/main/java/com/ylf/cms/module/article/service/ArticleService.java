package com.ylf.cms.module.article.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylf.cms.auth.LoginUser;
import com.ylf.cms.auth.UserContext;
import com.ylf.cms.common.constant.ArticleAction;
import com.ylf.cms.common.constant.ArticleStatus;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.PageResult;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.common.util.SortWhitelist;
import com.ylf.cms.module.article.dto.ArticleBatchRequest;
import com.ylf.cms.module.article.dto.ArticleQuery;
import com.ylf.cms.module.article.dto.ArticleSaveRequest;
import com.ylf.cms.module.article.entity.CmsArticle;
import com.ylf.cms.module.article.mapper.CmsArticleMapper;
import com.ylf.cms.module.article.vo.ArticleVO;
import com.ylf.cms.module.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleService {

    /** 逻辑排序名 → 物理列名。前端只能传左边，右边永远不会被拼接进 SQL */
    private static final Map<String, String> SORT_FIELDS = new LinkedHashMap<>();
    private static final String DEFAULT_ORDER = "a.create_time desc, a.id desc";

    static {
        SORT_FIELDS.put("createTime", "a.create_time");
        SORT_FIELDS.put("updateTime", "a.update_time");
        SORT_FIELDS.put("publishTime", "a.publish_time");
        SORT_FIELDS.put("title", "a.title");
        SORT_FIELDS.put("viewCount", "a.view_count");
    }

    private final CmsArticleMapper articleMapper;
    private final CategoryService categoryService;

    // ---------------------------------------------------------------- 查询

    public PageResult<ArticleVO> page(ArticleQuery query) {
        LoginUser user = UserContext.require();
        if (Boolean.TRUE.equals(query.getMine())) {
            query.setAuthorId(user.getId());
        }
        if (query.getCategoryId() != null) {
            query.setCategoryIds(categoryService.descendantIds(query.getCategoryId()));
        }
        query.setOrderBy(SortWhitelist.build(query.getSort(), SORT_FIELDS, DEFAULT_ORDER, "a.id desc"));

        IPage<ArticleVO> result = articleMapper.selectPageVO(
                new Page<>(query.getPageNum(), query.getPageSize()), query);
        result.getRecords().forEach(this::fillLabel);
        return PageResult.of(result);
    }

    /** 前台（游客）列表：只出已发布，不接受前端传 status */
    public PageResult<ArticleVO> portalPage(ArticleQuery query) {
        query.setStatus(ArticleStatus.PUBLISHED.name());
        if (query.getCategoryId() != null) {
            query.setCategoryIds(categoryService.descendantIds(query.getCategoryId()));
        }
        query.setOrderBy(SortWhitelist.build(query.getSort(), SORT_FIELDS, DEFAULT_ORDER, "a.id desc"));
        IPage<ArticleVO> result = articleMapper.selectPageVO(
                new Page<>(query.getPageNum(), query.getPageSize()), query);
        result.getRecords().forEach(this::fillLabel);
        return PageResult.of(result);
    }

    public ArticleVO detail(Long id) {
        ArticleVO vo = articleMapper.selectDetailById(id);
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文章不存在");
        }
        fillLabel(vo);
        LoginUser user = UserContext.get();
        boolean published = ArticleStatus.PUBLISHED.name().equals(vo.getStatus());
        if (!published) {
            if (user == null) {
                throw new BizException(ResultCode.UNAUTHORIZED);
            }
            boolean isOwner = user.getId().equals(vo.getAuthorId());
            if (!isOwner && !user.hasPermission(Perm.ARTICLE_UPDATE_ANY)
                    && !user.hasPermission(Perm.ARTICLE_AUDIT)) {
                throw new BizException(ResultCode.FORBIDDEN, "无权查看该文章");
            }
        }
        return vo;
    }

    /** 前台阅读详情：只允许看已发布的 */
    public ArticleVO portalDetail(Long id) {
        ArticleVO vo = articleMapper.selectDetailById(id);
        if (vo == null || !ArticleStatus.PUBLISHED.name().equals(vo.getStatus())) {
            throw new BizException(ResultCode.NOT_FOUND, "文章不存在");
        }
        fillLabel(vo);
        articleMapper.update(null, new UpdateWrapper<CmsArticle>()
                .eq("id", id)
                .setSql("view_count = view_count + 1"));
        return vo;
    }

    // ---------------------------------------------------------------- 写

    @Transactional
    public Long create(ArticleSaveRequest request) {
        LoginUser user = UserContext.require();
        categoryService.requireEnabled(request.getCategoryId());

        CmsArticle article = new CmsArticle();
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCategoryId(request.getCategoryId());
        article.setAuthorId(user.getId());
        article.setStatus(ArticleStatus.DRAFT.name());
        article.setViewCount(0);
        article.setVersion(0);
        articleMapper.insert(article);
        return article.getId();
    }

    @Transactional
    public void update(Long id, ArticleSaveRequest request) {
        CmsArticle article = requireArticle(id);
        checkOwner(article, Perm.ARTICLE_UPDATE_OWN, Perm.ARTICLE_UPDATE_ANY);
        ArticleStatus current = statusOf(article);
        if (current != ArticleStatus.DRAFT && current != ArticleStatus.REJECTED) {
            throw new BizException(ResultCode.INVALID_STATE,
                    "只有草稿或已驳回的文章可以编辑，当前状态：" + current.getLabel());
        }
        categoryService.requireEnabled(request.getCategoryId());

        // 乐观锁：版本对不上说明别人先改了，直接把冲突摆到用户面前，不要静默覆盖
        if (request.getVersion() != null && !request.getVersion().equals(article.getVersion())) {
            throw new BizException(ResultCode.CONFLICT, "文章已被他人修改，请刷新后重试");
        }
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCategoryId(request.getCategoryId());
        if (current == ArticleStatus.REJECTED) {
            article.setStatus(ArticleStateMachine.next(current, ArticleAction.MODIFY).name());
        }
        int rows = articleMapper.updateById(article);
        if (rows == 0) {
            throw new BizException(ResultCode.CONFLICT, "文章已被他人修改，请刷新后重试");
        }
    }

    @Transactional
    public void submit(Long id) {
        CmsArticle article = requireArticle(id);
        checkOwner(article, Perm.ARTICLE_SUBMIT, Perm.ARTICLE_UPDATE_ANY);
        article.setStatus(ArticleStateMachine.next(statusOf(article), ArticleAction.SUBMIT).name());
        article.setSubmitTime(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    @Transactional
    public void withdraw(Long id) {
        CmsArticle article = requireArticle(id);
        checkOwner(article, Perm.ARTICLE_SUBMIT, Perm.ARTICLE_UPDATE_ANY);
        article.setStatus(ArticleStateMachine.next(statusOf(article), ArticleAction.WITHDRAW).name());
        articleMapper.updateById(article);
    }

    /** 已下架的文章重新提交审核，状态 OFFLINE -> PENDING */
    @Transactional
    public void resubmit(Long id) {
        CmsArticle article = requireArticle(id);
        checkOwner(article, Perm.ARTICLE_SUBMIT, Perm.ARTICLE_UPDATE_ANY);
        article.setStatus(ArticleStateMachine.next(statusOf(article), ArticleAction.RESUBMIT).name());
        article.setSubmitTime(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    @Transactional
    public void approve(Long id, String opinion) {
        CmsArticle article = requireAuditable(id);
        // 投稿时分类是启用的，审核时可能已经被停用；发布这一刻再查一次
        categoryService.requireEnabled(article.getCategoryId());
        article.setStatus(ArticleStateMachine.next(statusOf(article), ArticleAction.APPROVE).name());
        article.setAuditorId(UserContext.require().getId());
        article.setAuditTime(LocalDateTime.now());
        article.setPublishTime(LocalDateTime.now());
        article.setAuditOpinion(opinion);
        articleMapper.updateById(article);
    }

    @Transactional
    public void reject(Long id, String opinion) {
        if (!StringUtils.hasText(opinion)) {
            throw new BizException(ResultCode.BAD_REQUEST, "驳回必须填写审核意见");
        }
        CmsArticle article = requireAuditable(id);
        article.setStatus(ArticleStateMachine.next(statusOf(article), ArticleAction.REJECT).name());
        article.setAuditorId(UserContext.require().getId());
        article.setAuditTime(LocalDateTime.now());
        article.setAuditOpinion(opinion);
        articleMapper.updateById(article);
    }

    @Transactional
    public void offline(Long id, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BizException(ResultCode.BAD_REQUEST, "下架必须填写原因");
        }
        CmsArticle article = requireArticle(id);
        article.setStatus(ArticleStateMachine.next(statusOf(article), ArticleAction.OFFLINE).name());
        article.setOfflineReason(reason);
        articleMapper.updateById(article);
    }

    @Transactional
    public void delete(Long id) {
        CmsArticle article = requireArticle(id);
        checkOwner(article, Perm.ARTICLE_DELETE_OWN, Perm.ARTICLE_DELETE_ANY);
        ArticleStatus current = statusOf(article);
        if (!ArticleStateMachine.canDelete(current)) {
            throw new BizException(ResultCode.CONFLICT,
                    "已发布或已下架的文章不能直接删除，请先下架并撤回为草稿");
        }
        articleMapper.deleteById(id);
    }

    /**
     * 批量审核/下架。逐条处理并返回失败明细：
     * 一条数据状态不对不应该让另外 99 条白做，所以这里不做整体事务。
     */
    public Map<String, Object> batchApprove(ArticleBatchRequest request) {
        List<Long> failed = new ArrayList<>();
        Map<Long, String> reasons = new LinkedHashMap<>();
        int success = 0;
        for (Long id : request.getIds()) {
            try {
                approve(id, request.getOpinion());
                success++;
            } catch (BizException e) {
                failed.add(id);
                reasons.put(id, e.getMessage());
            }
        }
        return batchResult(request.getIds().size(), success, failed, reasons);
    }

    public Map<String, Object> batchOffline(ArticleBatchRequest request) {
        List<Long> failed = new ArrayList<>();
        Map<Long, String> reasons = new LinkedHashMap<>();
        int success = 0;
        for (Long id : request.getIds()) {
            try {
                offline(id, request.getReason());
                success++;
            } catch (BizException e) {
                failed.add(id);
                reasons.put(id, e.getMessage());
            }
        }
        return batchResult(request.getIds().size(), success, failed, reasons);
    }

    private Map<String, Object> batchResult(int total, int success, List<Long> failed, Map<Long, String> reasons) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("reasons", reasons);
        return result;
    }

    // ---------------------------------------------------------------- 统计（智能体建议用）

    public long countByAuthorAndStatus(Long authorId, ArticleStatus status) {
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<CmsArticle>()
                .eq(CmsArticle::getAuthorId, authorId)
                .eq(CmsArticle::getStatus, status.name()));
        return count == null ? 0 : count;
    }

    public long countByStatus(ArticleStatus status) {
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<CmsArticle>()
                .eq(CmsArticle::getStatus, status.name()));
        return count == null ? 0 : count;
    }

    public long countByAuthor(Long authorId) {
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<CmsArticle>()
                .eq(CmsArticle::getAuthorId, authorId));
        return count == null ? 0 : count;
    }

    // ---------------------------------------------------------------- 内部

    private CmsArticle requireArticle(Long id) {
        CmsArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "文章不存在");
        }
        return article;
    }

    /** 审核的下限要求：不能审自己的文章，且必须处在待审核状态 */
    private CmsArticle requireAuditable(Long id) {
        CmsArticle article = requireArticle(id);
        if (UserContext.require().getId().equals(article.getAuthorId())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能审核自己提交的文章");
        }
        return article;
    }

    /**
     * 横向越权防线：有 :any 权限（管理员）直接放行；
     * 只有 :own 权限时必须确认这条数据确实属于当前用户。
     */
    private void checkOwner(CmsArticle article, String ownPerm, String anyPerm) {
        LoginUser user = UserContext.require();
        if (user.hasPermission(anyPerm)) {
            return;
        }
        if (!user.hasPermission(ownPerm) || !user.getId().equals(article.getAuthorId())) {
            throw new BizException(ResultCode.NOT_OWNER);
        }
    }

    private ArticleStatus statusOf(CmsArticle article) {
        ArticleStatus s = ArticleStatus.of(article.getStatus());
        if (s == null) {
            throw new BizException(ResultCode.INTERNAL, "未知的文章状态：" + article.getStatus());
        }
        return s;
    }

    private void fillLabel(ArticleVO vo) {
        ArticleStatus s = ArticleStatus.of(vo.getStatus());
        vo.setStatusLabel(s == null ? vo.getStatus() : s.getLabel());
    }
}
