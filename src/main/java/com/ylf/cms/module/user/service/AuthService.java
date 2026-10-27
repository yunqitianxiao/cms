package com.ylf.cms.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ylf.cms.auth.LoginUser;
import com.ylf.cms.common.constant.RoleCode;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.module.user.dto.RegisterRequest;
import com.ylf.cms.module.user.entity.SysRole;
import com.ylf.cms.module.user.entity.SysUser;
import com.ylf.cms.module.user.mapper.SysRoleMapper;
import com.ylf.cms.module.user.mapper.SysUserMapper;
import com.ylf.cms.module.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 校验账号密码。
     *
     * 账号不存在和密码错误返回同一个错误码：分开提示等于给攻击者一个
     * 用户名枚举接口，能批量判断哪些账号真实存在。
     */
    public LoginUser authenticate(String username, String rawPassword) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BizException(ResultCode.LOGIN_FAILED);
        }
        String encoded = userMapper.selectPasswordByUsername(username);
        if (encoded == null || !passwordEncoder.matches(rawPassword, encoded)) {
            throw new BizException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        return toLoginUser(user);
    }

    /**
     * 每请求重新装载登录用户。返回 null 表示"身份已失效"（用户被删或被停用），
     * 由拦截器统一转 401——这样停用一个账号能立刻踢下线，不用等 Session 过期。
     */
    public LoginUser loadLoginUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }
        return toLoginUser(user);
    }

    @Transactional
    public Long register(RegisterRequest request) {
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
        user.setStatus(1);
        userMapper.insert(user);

        SysRole defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, RoleCode.DEFAULT));
        if (defaultRole != null) {
            userRoleMapper.insert(user.getId(), defaultRole.getId());
        }
        return user.getId();
    }

    private LoginUser toLoginUser(SysUser user) {
        LoginUser lu = new LoginUser();
        lu.setId(user.getId());
        lu.setUsername(user.getUsername());
        lu.setNickname(user.getNickname());
        List<String> roleCodes = userMapper.selectRoleCodes(user.getId());
        List<String> perms = userMapper.selectPermCodes(user.getId());
        lu.setRoleCodes(new HashSet<>(roleCodes));
        lu.setPermissions(new HashSet<>(perms));
        return lu;
    }

    public Set<String> permissionsOf(Long userId) {
        return new HashSet<>(userMapper.selectPermCodes(userId));
    }
}
