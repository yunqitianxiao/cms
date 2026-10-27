package com.ylf.cms.module.user.vo;

import lombok.Data;

@Data
public class PermissionVO {

    private Long id;
    private String permCode;
    private String permName;
    private String permGroup;
    private String description;
}
