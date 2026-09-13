package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("acceptance_record")
public class AcceptanceRecord {
    @TableId(value = "acceptance_id", type = IdType.AUTO)
    private Long acceptanceId;
    private Long orderId;
    private Long reporterId;
    private String acceptResult;
    private String returnReason;
    private LocalDateTime acceptTime;

    public void pass() {
        this.acceptResult = "PASSED";
        this.returnReason = null;
    }

    public void reject(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("返修原因不能为空");
        this.acceptResult = "REJECTED";
        this.returnReason = reason.trim();
    }
}
