package com.ylf.cms.module.log.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** LOGIN / ARTICLE / AUDIT / CATEGORY / USER / ROLE / AI_QUERY */
    private String logType;

    private String operation;

    /** 类名#方法名 */
    private String method;

    private String uri;

    private String httpMethod;

    /** 脱敏并截断后的参数 */
    private String params;

    private String ip;

    /** SUCCESS / DENIED / FAIL */
    private String result;

    private String errorMsg;

    private Long costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
