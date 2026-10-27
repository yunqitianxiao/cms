package com.ylf.cms.common.result;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 分页参数基类。pageSize 上限在 DTO 上挡一次、在 MP 插件上再挡一次：
 * DTO 挡住正常请求，插件兜住绕过 DTO 的内部调用。
 */
@Data
public class PageQuery {

    @Min(value = 1, message = "页码从 1 开始")
    private long pageNum = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 200, message = "每页最多 200 条")
    private long pageSize = 10;
}
