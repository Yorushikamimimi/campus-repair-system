package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("repair_type")
public class RepairType {
    @TableId(value = "type_id", type = IdType.AUTO)
    private Long typeId;
    private String typeName;
    private String typeCode;
    private String description;
    private Integer status;

    public void enable() { status = 1; }
    public void disable() { status = 0; }
}
