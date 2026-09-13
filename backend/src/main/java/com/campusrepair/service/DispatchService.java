package com.campusrepair.service;

import com.campusrepair.domain.RepairOrder;

import java.util.List;

public interface DispatchService {
    void auditOrder(Long adminId, Long orderId, boolean approved, String comment);
    void dispatchOrder(Long adminId, Long orderId, Long maintainerId, String note);
    List<RepairOrder> listPendingOrders();
}

