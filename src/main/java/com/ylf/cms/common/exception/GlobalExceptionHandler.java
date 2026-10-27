package com.ylf.cms.common.exception;

import com.ylf.cms.common.result.Result;
import com.ylf.cms.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * 三条原则：
 * 1. 业务异常按声明的错误码返回，HTTP 状态码与 code 各自表达不同层次；
 * 2. 未知异常只回"系统内部错误"，堆栈只进服务端日志——把堆栈回给前端等于免费送攻击面；
 * 3. 每个响应带 traceId，用户报障时凭它能在日志里定位到这一次请求。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        ResultCode rc = e.getResultCode();
        return ResponseEntity.status(rc.getStatus())
                .body(Result.fail(rc, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return badRequest(msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(ResultCode.PARAM_MISSING.getStatus())
                .body(Result.fail(ResultCode.PARAM_MISSING, "缺少参数：" + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return badRequest("参数类型不正确：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return badRequest("请求体格式不正确");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(ResultCode.BAD_REQUEST.getStatus())
                .body(Result.fail(ResultCode.BAD_REQUEST, "不支持的请求方法：" + e.getMethod()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicate(DuplicateKeyException e) {
        // 唯一索引兜底：并发下先查后插仍可能撞车，靠数据库约束给出可读错误
        log.warn("唯一约束冲突: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(ResultCode.DUPLICATE.getStatus())
                .body(Result.fail(ResultCode.DUPLICATE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未捕获异常", e);
        return ResponseEntity.status(ResultCode.INTERNAL.getStatus())
                .body(Result.fail(ResultCode.INTERNAL));
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.status(ResultCode.BAD_REQUEST.getStatus())
                .body(Result.fail(ResultCode.BAD_REQUEST, message));
    }

    private String describe(FieldError fe) {
        return fe.getField() + " " + fe.getDefaultMessage();
    }
}
