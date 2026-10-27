package com.ylf.cms.module.article.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cms_article")
public class CmsArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    private Long categoryId;

    private Long authorId;

    /** DRAFT / PENDING / PUBLISHED / REJECTED / OFFLINE，只允许经状态机修改 */
    private String status;

    private Long auditorId;

    private String auditOpinion;

    private LocalDateTime auditTime;

    private LocalDateTime submitTime;

    private LocalDateTime publishTime;

    private String offlineReason;

    private Integer viewCount;

    /** 乐观锁：两个编辑页同时提交时，后提交者拿到 0 行受影响 */
    @Version
    private Integer version;

    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
