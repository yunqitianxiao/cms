package com.ylf.cms.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylf.cms.auth.UserContext;
import com.ylf.cms.common.constant.RoleCode;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.PageResult;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.module.article.entity.CmsArticle;
import com.ylf.cms.module.article.mapper.CmsArticleMapper;
import com.ylf.cms.module.user.dto.UserQuery;
import com.ylf.cms.module.user.dto.UserSaveRequest;
import com.ylf.cms.module.user.entity.SysRole;
import com.ylf.cms.module.user.entity.SysUser;
import com.ylf.cms.module.user.mapper.SysRoleMapper;
import com.ylf.cms.module.user.mapper.SysUserMapper;
import com.ylf.cms.module.user.mapper.SysUserRoleMapper;
import com.ylf.cms.module.user.vo.UserRoleRow;
import com.ylf.cms.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final CmsArticleMapper articleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(UserQuery query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getNickname, kw)
                    .or().like(SysUser::getEmail, kw));
        }
        if (query.getStatus() != null) {
            qw.eq(SysUser::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getRoleCode())) {
            // apply 的 {0} 是预编译占位符，不是字符串拼接
            qw.apply("id in (select ur.user_id from sys_user_role ur "
                    + "join sys_role r on r.id = ur.role_id where r.role_code = {0})", query.getRoleCode());
        }
        qw.orderByDesc(SysUser::getCreateTime).orderByAsc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(page, qw);

        List<SysUser> users = result.getRecords();
        Map<Long, List<UserRoleRow>> rolesByUser = loadRoles(users.stream()
                .map(SysUser::getId).collect(Collectors.toList()));

        PageResult<UserVO> pr = new PageResult<>();
        pr.setTotal(result.getTotal());
        pr.setPageNum(result.getCurrent());
        pr.setPageSize(result.getSize());
        pr.setPages(result.getPages());
        pr.setRecords(users.stream()
                .map(u -> toVO(u, rolesByUser.getOrDefault(u.getId(), Collections.emptyList())))
                .collect(Collectors.toList()));
        return pr;
    }

    public UserVO detail(Long id) {
        SysUser user = requireUser(id);
        return toVO(user, loadRoles(Collections.singletonList(id)).getOrDefault(id, Collections.emptyList()));
    }

    @Transactional
    public Long create(UserSaveRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "新增用户必须设置初始密码");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.DUPLICATE, "用户名已被占用");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        userMapper.insert(user);

        syncRoles(user.getId(), request.getRoleIds());
        return user.getId();
    }

    @Transactional
    public void update(Long id, UserSaveRequest request) {
        SysUser user = requireUser(id);
        // 用户名参与审计链条，不允许修改；请求里的 username 一律忽略
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        if (request.getStatus() != null && !request.getStatus().equals(user.getStatus())) {
            checkCanDisable(id, request.getStatus());
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(user);

        if (request.getRoleIds() != null) {
            checkCanRemoveAdminRole(id, request.getRoleIds());
            syncRoles(id, request.getRoleIds());
        }
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysUser user = requireUser(id);
        checkCanDisable(id, status);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        checkCanRemoveAdminRole(id, roleIds);
        syncRoles(id, roleIds);
    }

    @Transactional
    public void delete(Long id) {
        requireUser(id);
        if (UserContext.require().getId().equals(id)) {
            throw new BizException(ResultCode.CONFLICT, "不能删除当前登录的账号");
        }
        if (isActiveAdmin(id) && roleMapper.countActiveAdmins() <= 1) {
            throw new BizException(ResultCode.CONFLICT, "系统至少需要保留一名可用管理员");
        }
        // 用户是逻辑删除，外键不会拦；不查一次的话文章会指向一个"已删除"的作者，
        // 列表里显示空白，也没人能再编辑它。
        Long articles = articleMapper.selectCount(new LambdaQueryWrapper<CmsArticle>()
                .eq(CmsArticle::getAuthorId, id));
        if (articles != null && articles > 0) {
            throw new BizException(ResultCode.IN_USE,
                    "该用户名下还有 " + articles + " 篇稿件，请先转移或删除后再删除账号");
        }
        userMapper.deleteById(id);
        // 关联表是物理删除：留着会变成指向已删用户的孤儿行，用户列表的 join 会多出脏数据
        userRoleMapper.deleteByUserId(id);
    }

    private Map<Long, List<UserRoleRow>> loadRoles(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return roleMapper.selectRoleRowsByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleRow::getUserId));
    }

    private void syncRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            if (roleId == null) {
                continue;
            }
            if (roleMapper.selectById(roleId) == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "角色不存在：" + roleId);
            }
            userRoleMapper.insert(userId, roleId);
        }
    }

    private void checkCanDisable(Long userId, Integer status) {
        if (status == null || status == 1) {
            return;
        }
        if (UserContext.require().getId().equals(userId)) {
            throw new BizException(ResultCode.CONFLICT, "不能停用当前登录的账号");
        }
        if (isActiveAdmin(userId) && roleMapper.countActiveAdmins() <= 1) {
            throw new BizException(ResultCode.CONFLICT, "系统至少需要保留一名可用管理员");
        }
    }

    /** 把最后一个管理员从 ADMIN 角色里摘掉，后果和停用他一样 */
    private void checkCanRemoveAdminRole(Long userId, List<Long> newRoleIds) {
        if (!isActiveAdmin(userId)) {
            return;
        }
        SysRole adminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, RoleCode.ADMIN));
        if (adminRole == null) {
            return;
        }
        boolean keepsAdmin = newRoleIds != null && newRoleIds.contains(adminRole.getId());
        if (!keepsAdmin && roleMapper.countActiveAdmins() <= 1) {
            throw new BizException(ResultCode.CONFLICT, "不能移除最后一名管理员的管理员角色");
        }
    }

    private boolean isActiveAdmin(Long userId) {
        return userMapper.selectRoleCodes(userId).contains(RoleCode.ADMIN);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserVO toVO(SysUser user, List<UserRoleRow> roles) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        List<Long> roleIds = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();
        for (UserRoleRow row : roles) {
            roleIds.add(row.getRoleId());
            roleNames.add(row.getRoleName());
        }
        vo.setRoleIds(roleIds);
        vo.setRoleNames(roleNames);
        return vo;
    }
}
