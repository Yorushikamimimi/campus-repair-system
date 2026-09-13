package com.campusrepair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReworkRequest {
    @NotBlank(message = "返修原因不能为空")
    @Size(max = 500, message = "返修原因不能超过500字符")
    private String returnReason;
}

