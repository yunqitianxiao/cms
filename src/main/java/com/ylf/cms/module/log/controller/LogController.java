package com.ylf.cms.module.log.controller;

import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.result.PageResult;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.module.log.dto.LogQuery;
import com.ylf.cms.module.log.entity.SysLog;
import com.ylf.cms.module.log.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    @RequirePermission(Perm.LOG_READ)
    @GuideCard(id = "log.list", title = "操作日志", group = "系统",
            route = "/app/logs", order = 80,
            aliases = {"日志", "操作记录", "谁改的", "审计", "记录", "追踪"})
    public Result<PageResult<SysLog>> page(LogQuery query) {
        return Result.ok(logService.page(query));
    }
}
