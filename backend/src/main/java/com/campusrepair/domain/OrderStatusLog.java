package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("order_status_log")
public class OrderStatusLog {
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;
    private Long orderId;
    private Long operatorId;
    private String oldStatus;
    private String newStatus;
    private String changeReason;
    private LocalDateTime changeTime;

    public String describeTransition() {
        String oldValue = oldStatus == null ? "新建" : oldStatus;
        return oldValue + " -> " + newStatus;
    }
}
