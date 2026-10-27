package com.ylf.cms.module.user.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class RoleAssignRequest {

    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
