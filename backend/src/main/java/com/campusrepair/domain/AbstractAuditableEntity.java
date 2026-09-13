package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Common domain audit information. Concrete tables map their own audit columns. */
@Getter
@Setter
public abstract class AbstractAuditableEntity {
    @TableField(exist = false)
    protected LocalDateTime createTime;

    @TableField(exist = false)
    protected LocalDateTime updateTime;

    public void touchUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
