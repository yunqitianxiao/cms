package com.ylf.cms.module.user.vo;

import lombok.Data;

/** 用户-角色扁平行，用于列表页一次查出所有用户的角色，避免逐个用户查一次（N+1）。 */
@Data
public class UserRoleRow {

    private Long userId;
    private Long roleId;
    private String roleCode;
    private String roleName;
}
