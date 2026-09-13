package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("dispatch_record")
public class DispatchRecord {
    @TableId(value = "dispatch_id", type = IdType.AUTO)
    private Long dispatchId;
    private Long orderId;
    private Long adminId;
    private Long maintainerId;
    private LocalDateTime dispatchTime;
    private String dispatchNote;
    private String dispatchStatus;

    public void changeStatus(String status) {
        if (status == null || status.isBlank()) throw new IllegalArgumentException("派单状态不能为空");
        this.dispatchStatus = status.trim();
    }
}
