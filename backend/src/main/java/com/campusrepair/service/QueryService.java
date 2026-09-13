package com.campusrepair.service;

import com.campusrepair.domain.RepairOrder;

import java.util.List;

public interface QueryService {
    RepairOrder queryOrderProgress(Long userId, Long orderId);
    List<RepairOrder> queryOrders(Long userId, String status, Long typeId);
}
