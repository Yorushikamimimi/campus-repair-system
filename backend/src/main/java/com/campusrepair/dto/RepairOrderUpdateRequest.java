package com.campusrepair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RepairOrderUpdateRequest {
    @NotBlank(message = "工单标题不能为空")
    @Size(max = 128, message = "工单标题不能超过128个字符")
    private String title;

    @NotBlank(message = "工单描述不能为空")
    private String description;
}
