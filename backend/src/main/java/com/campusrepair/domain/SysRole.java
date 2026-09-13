package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_role")
public class SysRole {
    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;
    private String roleName;
    private String roleCode;
    private String remark;

    public void rename(String newRoleName) {
        if (newRoleName == null || newRoleName.isBlank()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        this.roleName = newRoleName.trim();
    }

    public void changeRemark(String newRemark) {
        this.remark = newRemark == null ? null : newRemark.trim();
    }
}
