package com.ylf.cms.module.user.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class PermissionAssignRequest {

    @NotNull(message = "权限列表不能为空")
    private List<Long> permIds;
}
