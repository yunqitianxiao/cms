package com.ylf.cms.module.user.dto;

import com.ylf.cms.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {

    private String keyword;

    private Integer status;

    private String roleCode;
}
