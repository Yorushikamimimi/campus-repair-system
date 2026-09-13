package com.campusrepair.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcceptanceRequest {
    @NotNull(message = "验收结论不能为空")
    private Boolean passed;

    @Size(max = 500, message = "验收不通过原因不能超过500字符")
    private String returnReason;
}

