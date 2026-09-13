package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("repair_location")
public class RepairLocation {
    @TableId(value = "location_id", type = IdType.AUTO)
    private Long locationId;
    private String buildingName;
    private String areaName;
    private String roomNo;
    private String description;
    private Integer status;

    public void enable() { status = 1; }
    public void disable() { status = 0; }
}
