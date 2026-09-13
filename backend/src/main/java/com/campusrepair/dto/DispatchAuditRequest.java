package com.campusrepair.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispatchAuditRequest {
    @NotNull(message = "审核结论不能为空")
    private Boolean approved;

    @Size(max = 500, message = "审核意见不能超过500字符")
    private String comment;
}

