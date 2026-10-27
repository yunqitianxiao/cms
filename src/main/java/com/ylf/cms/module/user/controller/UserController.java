package com.ylf.cms.module.user.controller;

import com.ylf.cms.common.annotation.OperLog;
import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.common.constant.LogType;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.result.PageResult;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.module.user.dto.RoleAssignRequest;
import com.ylf.cms.module.user.dto.UserQuery;
import com.ylf.cms.module.user.dto.UserSaveRequest;
import com.ylf.cms.module.user.service.UserService;
import com.ylf.cms.module.user.vo.UserVO;
import com.ylf.cms.guide.annotation.GuideCard;
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequirePermission(Perm.USER_MANAGE)
    @GuideCard(id = "user.manage", title = "用户管理", group = "账号与权限",
            route = "/app/users", order = 60,
            aliases = {"用户", "账号", "成员", "人员", "加人", "停用用户"})
    public Result<PageResult<UserVO>> page(UserQuery query) {
        return Result.ok(userService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePermission(Perm.USER_MANAGE)
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @PostMapping
    @RequirePermission(Perm.USER_MANAGE)
    @OperLog(type = LogType.USER, value = "新增用户")
    public Result<Long> create(@Valid @RequestBody UserSaveRequest request) {
        return Result.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(Perm.USER_MANAGE)
    @OperLog(type = LogType.USER, value = "修改用户")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        userService.update(id, request);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @RequirePermission(Perm.USER_MANAGE)
    @OperLog(type = LogType.USER, value = "修改用户状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    @RequirePermission(Perm.USER_MANAGE)
    @OperLog(type = LogType.USER, value = "分配用户角色")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody RoleAssignRequest request) {
        userService.assignRoles(id, request.getRoleIds());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Perm.USER_MANAGE)
    @OperLog(type = LogType.USER, value = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
