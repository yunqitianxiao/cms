package com.ylf.cms.module.log.dto;

import com.ylf.cms.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogQuery extends PageQuery {

    private Long userId;

    private String logType;

    private String result;

    private String keyword;

    /** yyyy-MM-dd */
    private String startDate;

    /** yyyy-MM-dd */
    private String endDate;
}
