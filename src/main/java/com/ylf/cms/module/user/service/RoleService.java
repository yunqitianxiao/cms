package com.ylf.cms.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.constant.RoleCode;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.module.user.dto.RoleSaveRequest;
import com.ylf.cms.module.user.entity.SysPermission;
import com.ylf.cms.module.user.entity.SysRole;
import com.ylf.cms.module.user.mapper.SysPermissionMapper;
import com.ylf.cms.module.user.mapper.SysRoleMapper;
import com.ylf.cms.module.user.mapper.SysRolePermissionMapper;
import com.ylf.cms.module.user.vo.PermissionVO;
import com.ylf.cms.module.user.vo.RolePermRow;
import com.ylf.cms.module.user.vo.RoleUserCountRow;
import com.ylf.cms.module.user.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    public List<RoleVO> list() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId));
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        Map<Long, List<Long>> permsByRole = new HashMap<>();
        for (RolePermRow row : roleMapper.selectPermRowsByRoleIds(roleIds)) {
            permsByRole.computeIfAbsent(row.getRoleId(), k -> new ArrayList<>()).add(row.getPermId());
        }
        Map<Long, Integer> userCounts = roleMapper.selectUserCounts().stream()
                .collect(Collectors.toMap(RoleUserCountRow::getRoleId, RoleUserCountRow::getUserCount));

        return roles.stream().map(r -> {
            RoleVO vo = new RoleVO();
            vo.setId(r.getId());
            vo.setRoleCode(r.getRoleCode());
            vo.setRoleName(r.getRoleName());
            vo.setDescription(r.getDescription());
            vo.setSort(r.getSort());
            vo.setStatus(r.getStatus());
            vo.setPermIds(permsByRole.getOrDefault(r.getId(), Collections.emptyList()));
            vo.setUserCount(userCounts.getOrDefault(r.getId(), 0));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<PermissionVO> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSort).orderByAsc(SysPermission::getId))
                .stream().map(p -> {
                    PermissionVO vo = new PermissionVO();
                    vo.setId(p.getId());
                    vo.setPermCode(p.getPermCode());
                    vo.setPermName(p.getPermName());
                    vo.setPermGroup(p.getPermGroup());
                    vo.setDescription(p.getDescription());
                    return vo;
                }).collect(Collectors.toList());
    }

    @Transactional
    public Long create(RoleSaveRequest request) {
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, request.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.DUPLICATE, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setSort(request.getSort() == null ? 100 : request.getSort());
        role.setStatus(request.getStatus());
        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional
    public void update(Long id, RoleSaveRequest request) {
        SysRole role = requireRole(id);
        // 角色编码是代码里的常量（RoleCode.ADMIN 等）和前端判断的依据，运行期不改
        if (!role.getRoleCode().equals(request.getRoleCode())) {
            throw new BizException(ResultCode.BAD_REQUEST, "角色编码不允许修改");
        }
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        if (request.getSort() != null) {
            role.setSort(request.getSort());
        }
        role.setStatus(request.getStatus());
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (RoleCode.ADMIN.equals(role.getRoleCode())) {
            throw new BizException(ResultCode.CONFLICT, "管理员角色是系统内置角色，不允许删除");
        }
        if (roleMapper.countUsersOfRole(id) > 0) {
            throw new BizException(ResultCode.IN_USE, "该角色仍被用户使用，请先移除用户角色");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.deleteByRoleId(id);
    }

    @Transactional
    public void assignPermissions(Long roleId, List<Long> permIds) {
        SysRole role = requireRole(roleId);
        // 管理员角色必须保留"角色管理"，否则谁也改不回来，只能改库
        if (RoleCode.ADMIN.equals(role.getRoleCode())) {
            SysPermission roleManage = permissionMapper.selectOne(
                    new LambdaQueryWrapper<SysPermission>()
                            .eq(SysPermission::getPermCode, Perm.ROLE_MANAGE));
            if (roleManage == null
                    || permIds == null
                    || !permIds.contains(roleManage.getId())) {
                throw new BizException(ResultCode.CONFLICT,
                        "管理员角色必须保留「角色管理」权限，否则将无人能再调整权限");
            }
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permIds == null) {
            return;
        }
        for (Long permId : permIds) {
            if (permId == null) {
                continue;
            }
            if (permissionMapper.selectById(permId) == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "权限点不存在：" + permId);
            }
            rolePermissionMapper.insert(roleId, permId);
        }
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }
}
