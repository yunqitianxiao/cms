package com.ylf.cms.module.user.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class UserSaveRequest {

    /** 新增时必填；修改时后端忽略该字段（用户名不可改，避免审计链断裂） */
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名为 4-20 位字母、数字或下划线")
    private String username;

    /** 新增时必填，修改时留空表示不改密码 */
    @Size(min = 6, max = 32, message = "密码长度 6-32 位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private List<Long> roleIds;
}
