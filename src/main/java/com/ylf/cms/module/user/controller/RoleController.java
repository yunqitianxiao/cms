package com.ylf.cms.module.user.controller;

import com.ylf.cms.common.annotation.OperLog;
import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.common.constant.LogType;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.module.user.dto.PermissionAssignRequest;
import com.ylf.cms.module.user.dto.RoleSaveRequest;
import com.ylf.cms.module.user.service.RoleService;
import com.ylf.cms.module.user.vo.PermissionVO;
import com.ylf.cms.module.user.vo.RoleVO;
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
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequirePermission({Perm.ROLE_MANAGE, Perm.USER_MANAGE})
    @GuideCard(id = "role.manage", title = "角色与权限", group = "账号与权限",
            route = "/app/roles", order = 70,
            aliases = {"角色", "权限", "授权", "分配权限", "谁能干什么"})
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.list());
    }

    @GetMapping("/permissions")
    @RequirePermission({Perm.ROLE_MANAGE, Perm.USER_MANAGE})
    public Result<List<PermissionVO>> permissions() {
        return Result.ok(roleService.listPermissions());
    }

    @PostMapping
    @RequirePermission(Perm.ROLE_MANAGE)
    @OperLog(type = LogType.ROLE, value = "新增角色")
    public Result<Long> create(@Valid @RequestBody RoleSaveRequest request) {
        return Result.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(Perm.ROLE_MANAGE)
    @OperLog(type = LogType.ROLE, value = "修改角色")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest request) {
        roleService.update(id, request);
        return Result.ok();
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission(Perm.ROLE_MANAGE)
    @OperLog(type = LogType.ROLE, value = "分配角色权限")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                          @Valid @RequestBody PermissionAssignRequest request) {
        roleService.assignPermissions(id, request.getPermIds());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Perm.ROLE_MANAGE)
    @OperLog(type = LogType.ROLE, value = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
