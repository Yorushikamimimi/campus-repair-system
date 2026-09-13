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
@TableName("sys_user")
public class SysUser extends AbstractAuditableEntity {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    public void changeStatus(Integer newStatus) {
        if (newStatus == null || (newStatus != 0 && newStatus != 1)) {
            throw new IllegalArgumentException("用户状态必须为0或1");
        }
        this.status = newStatus;
        touchUpdate();
    }

    public void updateProfile(String newRealName, String newPhone) {
        if (newRealName == null || newRealName.isBlank()) {
            throw new IllegalArgumentException("真实姓名不能为空");
        }
        this.realName = newRealName.trim();
        this.phone = newPhone == null ? null : newPhone.trim();
        touchUpdate();
    }

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }
}
