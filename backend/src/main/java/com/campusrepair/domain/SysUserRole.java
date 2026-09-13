package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_user_role")
public class SysUserRole {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime assignTime;

    public boolean matches(Long expectedUserId, Long expectedRoleId) {
        return userId != null && userId.equals(expectedUserId)
                && roleId != null && roleId.equals(expectedRoleId);
    }
}
