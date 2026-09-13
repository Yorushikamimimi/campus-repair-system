package com.campusrepair.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Setter
@TableName("repair_image")
public class RepairImage {
    @TableId(value = "image_id", type = IdType.AUTO)
    private Long imageId;
    private Long orderId;
    private String fileUrl;
    private String fileName;
    private LocalDateTime uploadTime;

    public String getExtension() {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
