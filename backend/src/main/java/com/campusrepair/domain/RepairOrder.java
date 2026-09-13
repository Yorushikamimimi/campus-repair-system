package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("repair_order")
public class RepairOrder extends AbstractAuditableEntity {
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;
    private Long reporterId;
    private Long typeId;
    private Long locationId;
    private String title;
    private String description;
    private String status;

    /** The frozen schema calls this column submit_time; it is the domain create time. */
    @TableField("submit_time")
    private LocalDateTime submitTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Override
    public LocalDateTime getCreateTime() {
        return submitTime;
    }

    @Override
    public void setCreateTime(LocalDateTime value) {
        this.submitTime = value;
    }

    @Override
    public void touchUpdate() {
        this.updateTime = LocalDateTime.now();
        super.touchUpdate();
    }

    public void updateContent(String newTitle, String newDescription) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("工单标题不能为空");
        }
        if (newDescription == null || newDescription.isBlank()) {
            throw new IllegalArgumentException("工单描述不能为空");
        }
        this.title = newTitle.trim();
        this.description = newDescription.trim();
        touchUpdate();
    }

    public boolean isOwnedBy(Long userId) {
        return reporterId != null && reporterId.equals(userId);
    }
}
