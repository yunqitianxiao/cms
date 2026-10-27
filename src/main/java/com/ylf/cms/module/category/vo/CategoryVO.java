package com.ylf.cms.module.category.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVO {

    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sort;
    private Integer status;
    private String description;
    private List<CategoryVO> children = new ArrayList<>();
}
