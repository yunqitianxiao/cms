package com.ylf.cms.module.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.module.category.dto.CategorySaveRequest;
import com.ylf.cms.module.category.entity.CmsCategory;
import com.ylf.cms.module.category.mapper.CmsCategoryMapper;
import com.ylf.cms.module.category.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    public static final int MAX_LEVEL = 3;

    private final CmsCategoryMapper categoryMapper;

    public List<CategoryVO> tree(boolean onlyEnabled) {
        LambdaQueryWrapper<CmsCategory> qw = new LambdaQueryWrapper<CmsCategory>()
                .orderByAsc(CmsCategory::getSort).orderByAsc(CmsCategory::getId);
        if (onlyEnabled) {
            qw.eq(CmsCategory::getStatus, 1);
        }
        List<CmsCategory> all = categoryMapper.selectList(qw);
        return buildTree(all);
    }

    public List<CategoryVO> listFlat() {
        List<CmsCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<CmsCategory>()
                .orderByAsc(CmsCategory::getLevel)
                .orderByAsc(CmsCategory::getSort)
                .orderByAsc(CmsCategory::getId));
        List<CategoryVO> list = new ArrayList<>();
        for (CmsCategory c : all) {
            list.add(toVO(c));
        }
        return list;
    }

    @Transactional
    public Long create(CategorySaveRequest request) {
        long parentId = request.getParentId() == null ? 0L : request.getParentId();
        int level = 1;
        if (parentId != 0) {
            CmsCategory parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "上级分类不存在");
            }
            level = parent.getLevel() + 1;
            if (level > MAX_LEVEL) {
                throw new BizException(ResultCode.CATEGORY_LEVEL_EXCEED);
            }
        }
        checkNameUnique(parentId, request.getName(), null);

        CmsCategory category = new CmsCategory();
        category.setParentId(parentId);
        category.setName(request.getName().trim());
        // level 由服务端推导，绝不信前端传的值
        category.setLevel(level);
        category.setSort(request.getSort() == null ? 100 : request.getSort());
        category.setStatus(request.getStatus());
        category.setDescription(request.getDescription());
        categoryMapper.insert(category);
        return category.getId();
    }

    @Transactional
    public void update(Long id, CategorySaveRequest request) {
        CmsCategory category = requireCategory(id);
        checkNameUnique(category.getParentId(), request.getName(), id);
        checkCanDisable(id, request.getStatus());
        category.setName(request.getName().trim());
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        category.setStatus(request.getStatus());
        category.setDescription(request.getDescription());
        categoryMapper.updateById(category);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        CmsCategory category = requireCategory(id);
        checkCanDisable(id, status);
        category.setStatus(status);
        categoryMapper.updateById(category);
    }

    /**
     * 父分类停用后子分类还启用着，会留下"父不可见、子仍可选"的矛盾状态：
     * 新建文章的下拉里还能选到那个子分类，树却没有它的上级。
     */
    private void checkCanDisable(Long id, Integer status) {
        if (status == null || status == 1) {
            return;
        }
        int enabled = categoryMapper.countEnabledChildren(id);
        if (enabled > 0) {
            throw new BizException(ResultCode.IN_USE,
                    "还有 " + enabled + " 个启用的子分类，请先停用子分类");
        }
    }

    @Transactional
    public void delete(Long id) {
        requireCategory(id);
        if (categoryMapper.countChildren(id) > 0) {
            throw new BizException(ResultCode.IN_USE, "存在子分类，不能删除");
        }
        if (categoryMapper.countArticles(id) > 0) {
            throw new BizException(ResultCode.IN_USE, "该分类下仍有文章，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 某分类及其全部子孙分类的 id。按文章列表的"按分类筛选"语义，
     * 选了父分类应该能看到子分类下的文章，否则用户会觉得列表漏数据。
     */
    public List<Long> descendantIds(Long rootId) {
        List<CmsCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<CmsCategory>()
                .select(CmsCategory::getId, CmsCategory::getParentId));
        Map<Long, List<Long>> childrenOf = new java.util.HashMap<>();
        for (CmsCategory c : all) {
            childrenOf.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
        }
        List<Long> result = new ArrayList<>();
        collect(rootId, childrenOf, result);
        return result;
    }

    private void collect(Long id, Map<Long, List<Long>> childrenOf, List<Long> acc) {
        acc.add(id);
        List<Long> children = childrenOf.get(id);
        if (children == null) {
            return;
        }
        for (Long child : children) {
            if (!acc.contains(child)) {
                collect(child, childrenOf, acc);
            }
        }
    }

    /** 发布文章时校验：分类必须存在且为启用状态 */
    public CmsCategory requireEnabled(Long id) {
        CmsCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "分类不存在");
        }
        if (category.getStatus() == null || category.getStatus() != 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "该分类已停用，不能用于发布");
        }
        return category;
    }

    private void checkNameUnique(long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<CmsCategory> qw = new LambdaQueryWrapper<CmsCategory>()
                .eq(CmsCategory::getParentId, parentId)
                .eq(CmsCategory::getName, name.trim());
        if (excludeId != null) {
            qw.ne(CmsCategory::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DUPLICATE, "同一上级下已存在同名分类");
        }
    }

    private CmsCategory requireCategory(Long id) {
        CmsCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private List<CategoryVO> buildTree(List<CmsCategory> all) {
        Map<Long, CategoryVO> byId = new LinkedHashMap<>();
        for (CmsCategory c : all) {
            byId.put(c.getId(), toVO(c));
        }
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : byId.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            } else {
                CategoryVO parent = byId.get(vo.getParentId());
                // 父节点被停用/过滤掉时，子节点挂到根上而不是凭空消失
                if (parent == null) {
                    roots.add(vo);
                } else {
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }

    private CategoryVO toVO(CmsCategory c) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setLevel(c.getLevel());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        vo.setDescription(c.getDescription());
        return vo;
    }
}
