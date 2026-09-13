package com.campusrepair.service;

import com.campusrepair.domain.AcceptanceRecord;

import java.util.List;

public interface AcceptanceService {
    AcceptanceRecord acceptRepairResult(Long reporterId, Long orderId, boolean passed, String returnReason);
    AcceptanceRecord requestRework(Long reporterId, Long orderId, String returnReason);
    List<AcceptanceRecord> getAcceptanceHistory(Long userId, Long orderId, boolean allowAdmin);
}

