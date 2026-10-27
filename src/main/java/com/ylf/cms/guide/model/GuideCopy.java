package com.ylf.cms.guide.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** site-map.yaml 里单个卡片的文案 */
@Data
public class GuideCopy {

    private String summary;
    private List<String> steps = new ArrayList<>();
    private List<String> tips = new ArrayList<>();
    private String highlight;
}
