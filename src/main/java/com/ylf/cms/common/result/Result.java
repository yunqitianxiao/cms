package com.ylf.cms.common.result;

import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一返回结构。所有接口（含异常分支）都返回这个壳，前端只需判断 code。
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;
    private String traceId;
    private long timestamp;

    public static <T> Result<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(ResultCode rc) {
        return build(rc.getCode(), rc.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode rc, String message) {
        return build(rc.getCode(), message, null);
    }

    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        r.setTraceId(MDC.get("traceId"));
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }
}
