package com.campusrepair.service;

public interface OrderStateService {
    void submit(Long orderId, Long operatorId);
    void approve(Long orderId, Long operatorId);
    void dispatch(Long orderId, Long operatorId);
    void startRepair(Long orderId, Long operatorId);
    void finishRepair(Long orderId, Long operatorId);
    void accept(Long orderId, Long operatorId);
    void reject(Long orderId, Long operatorId, String reason);
    void reopen(Long orderId, Long operatorId);
}
