package com.campusrepair.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispatchOrderRequest {
    @NotNull(message = "维修人员ID不能为空")
    private Long maintainerId;

    @Size(max = 500, message = "派单说明不能超过500字符")
    private String note;
}

