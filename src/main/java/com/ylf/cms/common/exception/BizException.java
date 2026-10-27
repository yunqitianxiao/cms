package com.ylf.cms.common.exception;

import com.ylf.cms.common.result.ResultCode;

/**
 * 业务异常。只用于"可预期的失败"（校验不过、状态非法、无权限），
 * 不用于包装系统异常——那样会把真正的 bug 藏起来。
 */
public class BizException extends RuntimeException {

    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
