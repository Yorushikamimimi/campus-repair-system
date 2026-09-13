package com.campusrepair.dto;

import java.time.LocalDateTime;

public record MaintenanceTaskItem(
        Long orderId,
        String title,
        Long typeId,
        Long locationId,
        String status,
        LocalDateTime dispatchTime,
        String dispatchNote) { }

