package com.campusrepair.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationRequest {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分需在1到5之间")
    @Max(value = 5, message = "评分需在1到5之间")
    private Integer score;

    @Size(max = 500, message = "评价文字不能超过500字符")
    private String comment;
}

