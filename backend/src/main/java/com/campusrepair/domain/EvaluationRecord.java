package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("evaluation_record")
public class EvaluationRecord {
    @TableId(value = "evaluation_id", type = IdType.AUTO)
    private Long evaluationId;
    private Long orderId;
    private Long reporterId;
    private Integer score;
    private String comment;
    private LocalDateTime evaluateTime;

    public boolean validateScore() {
        return score != null && score >= 1 && score <= 5;
    }
}
