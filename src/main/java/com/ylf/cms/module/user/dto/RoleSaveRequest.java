package com.ylf.cms.module.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RoleSaveRequest {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z_]{2,20}$", message = "角色编码为 2-20 位大写字母或下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称最长 50 字")
    private String roleName;

    @Size(max = 200, message = "描述最长 200 字")
    private String description;

    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
