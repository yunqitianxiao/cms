package com.ylf.cms.module.category.controller;

import com.ylf.cms.common.annotation.OperLog;
import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.common.constant.LogType;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.module.category.dto.CategorySaveRequest;
import com.ylf.cms.module.category.service.CategoryService;
import com.ylf.cms.module.category.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @RequirePermission(Perm.CATEGORY_READ)
    public Result<List<CategoryVO>> tree(@RequestParam(defaultValue = "false") boolean onlyEnabled) {
        return Result.ok(categoryService.tree(onlyEnabled));
    }

    @GetMapping
    @RequirePermission(Perm.CATEGORY_READ)
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listFlat());
    }

    /*
     * 卡片挂在"新增"上而不是"查树"上：卡片代表的是"管理分类"，门槛应对齐
     * category:write；挂在查树上会让只有读权限的游客看到一张"你可以管理分类"的卡片。
     */
    @PostMapping
    @RequirePermission(Perm.CATEGORY_WRITE)
    @GuideCard(id = "category.manage", title = "分类管理", group = "内容",
            route = "/app/categories", order = 30,
            aliases = {"分类", "栏目", "频道", "目录", "建分类", "排序"})
    @OperLog(type = LogType.CATEGORY, value = "新增分类")
    public Result<Long> create(@Valid @RequestBody CategorySaveRequest request) {
        return Result.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(Perm.CATEGORY_WRITE)
    @OperLog(type = LogType.CATEGORY, value = "修改分类")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) {
        categoryService.update(id, request);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @RequirePermission(Perm.CATEGORY_WRITE)
    @OperLog(type = LogType.CATEGORY, value = "修改分类状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Perm.CATEGORY_WRITE)
    @OperLog(type = LogType.CATEGORY, value = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
