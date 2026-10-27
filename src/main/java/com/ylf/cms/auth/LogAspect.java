package com.ylf.cms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ylf.cms.common.annotation.OperLog;
import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;
import com.ylf.cms.common.util.IpUtil;
import com.ylf.cms.common.util.MaskUtil;
import com.ylf.cms.module.log.entity.SysLog;
import com.ylf.cms.module.log.service.LogService;
import com.ylf.cms.module.user.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志切面。最外层切面（@Order(1)），因此它能看到内层权限切面抛出的异常，
 * 越权尝试会被记成 DENIED 而不是凭空消失。
 *
 * 失败也要记，而且要独立事务记（见 LogService.record）——业务回滚时日志不能跟着没。
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class LogAspect {

    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        String result = "SUCCESS";
        String errorMsg = null;
        try {
            return joinPoint.proceed();
        } catch (BizException e) {
            result = isDenied(e) ? "DENIED" : "FAIL";
            errorMsg = e.getMessage();
            throw e;
        } catch (Throwable t) {
            result = "FAIL";
            errorMsg = t.getClass().getSimpleName() + ": " + t.getMessage();
            throw t;
        } finally {
            writeLog(joinPoint, operLog, result, errorMsg, System.currentTimeMillis() - start);
        }
    }

    private void writeLog(ProceedingJoinPoint joinPoint, OperLog operLog,
                          String result, String errorMsg, long costMs) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            LoginUser user = UserContext.get();

            SysLog entry = new SysLog();
            if (user != null) {
                entry.setUserId(user.getId());
                entry.setUsername(user.getUsername());
            } else {
                // 登录接口在拦截器放行范围内，此时上下文里还没有用户，从参数里取用户名
                for (Object arg : joinPoint.getArgs()) {
                    if (arg instanceof LoginRequest) {
                        entry.setUsername(((LoginRequest) arg).getUsername());
                        break;
                    }
                }
            }
            entry.setLogType(operLog.type());
            entry.setOperation(operLog.value());
            entry.setMethod(signature.getDeclaringType().getSimpleName() + "#" + signature.getName());
            entry.setParams(MaskUtil.mask(joinPoint.getArgs(), objectMapper));
            entry.setResult(result);
            entry.setErrorMsg(truncate(errorMsg, 400));
            entry.setCostMs(costMs);

            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setUri(truncate(request.getRequestURI(), 200));
                entry.setHttpMethod(request.getMethod());
                entry.setIp(IpUtil.getIp(request));
            }
            logService.record(entry);
        } catch (Exception e) {
            log.warn("写操作日志失败: {}", e.getMessage());
        }
    }

    private boolean isDenied(BizException e) {
        return e.getResultCode() == ResultCode.FORBIDDEN
                || e.getResultCode() == ResultCode.NOT_OWNER
                || e.getResultCode() == ResultCode.UNAUTHORIZED;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
