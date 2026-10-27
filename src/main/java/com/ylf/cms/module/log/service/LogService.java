package com.ylf.cms.module.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.PageResult;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.module.log.dto.LogQuery;
import com.ylf.cms.module.log.entity.SysLog;
import com.ylf.cms.module.log.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final SysLogMapper logMapper;

    /**
     * 独立事务写日志。
     *
     * 业务方法抛异常时外层事务会回滚，如果日志和业务共用事务，
     * 恰好会丢掉"最需要查的那条失败记录"。所以这里必须 REQUIRES_NEW。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SysLog sysLog) {
        try {
            logMapper.insert(sysLog);
        } catch (Exception e) {
            // 日志失败不能反过来影响业务
            log.warn("写操作日志失败: {}", e.getMessage());
        }
    }

    public PageResult<SysLog> page(LogQuery query) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        if (query.getUserId() != null) {
            qw.eq(SysLog::getUserId, query.getUserId());
        }
        if (StringUtils.hasText(query.getLogType())) {
            qw.eq(SysLog::getLogType, query.getLogType());
        }
        if (StringUtils.hasText(query.getResult())) {
            qw.eq(SysLog::getResult, query.getResult());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(SysLog::getOperation, kw).or().like(SysLog::getUsername, kw));
        }
        if (StringUtils.hasText(query.getStartDate())) {
            qw.ge(SysLog::getCreateTime, parseDate(query.getStartDate(), "开始日期").atStartOfDay());
        }
        if (StringUtils.hasText(query.getEndDate())) {
            qw.lt(SysLog::getCreateTime, parseDate(query.getEndDate(), "结束日期").plusDays(1).atStartOfDay());
        }
        qw.orderByDesc(SysLog::getId);

        Page<SysLog> page = logMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), qw);
        return PageResult.of(page);
    }

    private LocalDate parseDate(String text, String label) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new BizException(ResultCode.BAD_REQUEST, label + "格式应为 yyyy-MM-dd");
        }
    }
}
