package com.campusrepair.service;

import com.campusrepair.domain.RepairRecord;
import com.campusrepair.dto.MaintenanceTaskItem;
import com.campusrepair.dto.MaintenanceProcessRequest;

import java.util.List;

public interface MaintenanceService {
    List<MaintenanceTaskItem> listTasks(Long maintainerId);
    void acceptTask(Long maintainerId, Long orderId);
    void processRepairOrder(Long maintainerId, Long orderId, MaintenanceProcessRequest request);
    void continueRepair(Long maintainerId, Long orderId);
    List<RepairRecord> getRepairHistory(Long orderId);
}

