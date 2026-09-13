package com.campusrepair.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Database status codes and their Chinese presentation labels. */
public enum OrderStatus {
    SUBMITTED("SUBMITTED", "待审核"),
    PENDING_PROCESS("PENDING_PROCESS", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    PENDING_ACCEPTANCE("PENDING_ACCEPTANCE", "待验收"),
    REWORK("REWORK", "待返修"),
    COMPLETED("COMPLETED", "已完成");

    private static final Map<String, OrderStatus> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(OrderStatus::code, Function.identity()));
    private final String code;
    private final String displayName;

    OrderStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }
    public static OrderStatus fromCode(String code) { return BY_CODE.get(code); }
}
