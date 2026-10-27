package com.ylf.cms.auth;

import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 接口级权限校验切面。
 *
 * 顺序在 LogAspect 之后（@Order 值更大 = 更内层），
 * 所以被 403 拦下的请求仍然会被 LogAspect 记录一条 DENIED —— 越权尝试必须留痕。
 */
@Slf4j
@Aspect
@Component
@Order(2)
public class PermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object check(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        LoginUser user = UserContext.require();
        String[] codes = requirePermission.value();

        boolean allowed = requirePermission.logic() == RequirePermission.Logic.ALL
                ? Arrays.stream(codes).allMatch(user::hasPermission)
                : user.hasAnyPermission(codes);

        if (!allowed) {
            log.warn("权限不足 user={} required={} path={}", user.getUsername(),
                    Arrays.toString(codes), joinPoint.getSignature().toShortString());
            throw new BizException(ResultCode.FORBIDDEN,
                    "缺少权限：" + String.join(" 或 ", codes));
        }
        return joinPoint.proceed();
    }
}
