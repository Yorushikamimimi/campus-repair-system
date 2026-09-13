package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("repair_record")
public class RepairRecord {
    @TableId(value = "repair_id", type = IdType.AUTO)
    private Long repairId;
    private Long orderId;
    private Long maintainerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String processDesc;
    private String repairResult;
    private String unfinishedReason;

    public void finish(String result) {
        this.repairResult = result;
        this.endTime = LocalDateTime.now();
    }

    public void recordUnfinished(String reason) {
        this.unfinishedReason = reason;
    }
}
