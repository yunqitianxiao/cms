package com.ylf.cms.auth;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 登录态。每次请求由 LoginInterceptor 从数据库重新装载，
 * 所以管理员改了角色/权限后，下一个请求就生效，不需要用户重新登录。
 */
@Data
public class LoginUser implements Serializable {

    private Long id;
    private String username;
    private String nickname;
    private Set<String> roleCodes;
    private Set<String> permissions;

    public boolean hasPermission(String code) {
        return permissions != null && permissions.contains(code);
    }

    public boolean hasAnyPermission(String... codes) {
        for (String c : codes) {
            if (hasPermission(c)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }

    public Set<String> getRoleCodes() {
        return roleCodes == null ? Collections.emptySet() : roleCodes;
    }

    public Set<String> getPermissions() {
        return permissions == null ? Collections.emptySet() : permissions;
    }
}
