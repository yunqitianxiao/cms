package com.ylf.cms.common.result;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码。code 采用 5 位分段：4xxxx 业务 / 客户端错误，5xxxx 服务端错误。
 * 与 HTTP 状态码分开：HTTP 状态码表达"协议层结果"，code 表达"业务原因"。
 */
public enum ResultCode {

    SUCCESS(0, "成功", HttpStatus.OK),

    BAD_REQUEST(40000, "请求参数不合法", HttpStatus.BAD_REQUEST),
    CATEGORY_LEVEL_EXCEED(40001, "分类层级超出上限（最多 3 级）", HttpStatus.BAD_REQUEST),
    PARAM_MISSING(40002, "缺少必要参数", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED),
    LOGIN_FAILED(40101, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(40102, "账号已被停用", HttpStatus.UNAUTHORIZED),

    FORBIDDEN(40300, "没有操作权限", HttpStatus.FORBIDDEN),
    NOT_OWNER(40301, "只能操作自己的数据", HttpStatus.FORBIDDEN),

    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),

    CONFLICT(40900, "资源状态冲突", HttpStatus.CONFLICT),
    INVALID_STATE(40901, "当前状态不允许该操作", HttpStatus.CONFLICT),
    DUPLICATE(40902, "数据已存在", HttpStatus.CONFLICT),
    IN_USE(40903, "数据被引用，无法删除", HttpStatus.CONFLICT),

    INTERNAL(50000, "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR),
    DB_ERROR(50001, "数据库操作失败", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ResultCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
