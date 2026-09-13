package com.campusrepair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceProcessRequest {
    @NotBlank(message = "维修过程不能为空")
    @Size(max = 5000, message = "维修过程不能超过5000字符")
    private String processDesc;

    private Boolean completed;

    @Size(max = 500, message = "维修结果不能为空且不能超过500字符")
    private String repairResult;

    @Size(max = 500, message = "未完成原因不能超过500字符")
    private String unfinishedReason;
}

