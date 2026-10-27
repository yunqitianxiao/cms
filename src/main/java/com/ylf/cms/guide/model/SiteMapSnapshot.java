package com.ylf.cms.guide.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集结果。除了卡片本身，还要带上"哪些接口有权限注解却没有卡片"，
 * 这是启动期校验 C2 的输入。
 */
@Data
public class SiteMapSnapshot {

    private Map<String, GuideCardVO> cards = new LinkedHashMap<>();

    /**
     * 有 @RequirePermission 但整类都没有 @GuideCard 的 Controller。
     *
     * 粒度是"类"而不是"方法"：一个页面背后往往有十几个接口（详情、新增、改状态…），
     * 要求每个接口都挂卡片只会逼人写一堆重复卡片。真正需要保证的是
     * "没有哪个受控模块整个是黑箱"，所以按 Controller 类判断。
     */
    private List<String> uncoveredPermittedMethods = new ArrayList<>();

    /** Controller 声明了但没有任何处理方法的卡片 id（用于排查） */
    private List<String> emptyCards = new ArrayList<>();

    /** 重复声明的卡片 id。采集时如果直接 put 进 Map 会被静默覆盖，所以单独记一份 */
    private List<String> duplicateIds = new ArrayList<>();
}
