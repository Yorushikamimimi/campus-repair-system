package com.campusrepair.common;

public enum ErrorCode {
    BAD_REQUEST(400, "请求参数不合法"),
    UNAUTHORIZED(401, "未认证或认证已失效"),
    FORBIDDEN(403, "没有执行该操作的权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "当前状态不允许该操作"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }
    public String message() { return message; }
}
