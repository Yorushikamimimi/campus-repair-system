package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.AcceptanceRecord;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.AcceptanceRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.AcceptanceService;
import com.campusrepair.service.OrderStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcceptanceServiceImpl implements AcceptanceService {
    private final AcceptanceRepository acceptanceRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final OrderStateService orderStateService;

    public AcceptanceServiceImpl(AcceptanceRepository acceptanceRepository,
                                RepairOrderRepository repairOrderRepository,
                                OrderStateService orderStateService) {
        this.acceptanceRepository = acceptanceRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.orderStateService = orderStateService;
    }

    @Override
    @Transactional
    public AcceptanceRecord acceptRepairResult(Long reporterId, Long orderId, boolean passed, String returnReason) {
        AcceptanceContext context = resolveOrderOwnership(reporterId, orderId);
        requirePendingAcceptance(context.order);

        AcceptanceRecord record = new AcceptanceRecord();
        record.setOrderId(orderId);
        record.setReporterId(reporterId);

        if (passed) {
            record.pass();
            orderStateService.accept(orderId, reporterId);
            acceptanceRepository.saveAcceptance(record);
            return record;
        }
        ensureReturnReason(returnReason);
        record.reject(returnReason);
        orderStateService.reject(orderId, reporterId, returnReason);
        acceptanceRepository.saveAcceptance(record);
        return record;
    }

    @Override
    @Transactional
    public AcceptanceRecord requestRework(Long reporterId, Long orderId, String returnReason) {
        return acceptRepairResult(reporterId, orderId, false, returnReason);
    }

    @Override
    public List<AcceptanceRecord> getAcceptanceHistory(Long userId, Long orderId, boolean allowAdmin) {
        AcceptanceContext context = loadOrder(orderId);
        if (!allowAdmin && !context.order.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看本人验收历史");
        }
        return acceptanceRepository.findByOrder(context.order.getOrderId());
    }

    private AcceptanceContext resolveOrderOwnership(Long reporterId, Long orderId) {
        AcceptanceContext context = loadOrder(orderId);
        if (!context.order.isOwnedBy(reporterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能验收本人报修单");
        }
        return context;
    }

    private AcceptanceContext loadOrder(Long orderId) {
        if (orderId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        RepairOrder order = repairOrderRepository.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        return new AcceptanceContext(order);
    }

    private void requirePendingAcceptance(RepairOrder order) {
        if (!OrderStatus.PENDING_ACCEPTANCE.code().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有待验收工单可执行验收");
        }
    }

    private void ensureReturnReason(String returnReason) {
        if (returnReason == null || returnReason.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验收不通过原因不能为空");
        }
    }

    private static class AcceptanceContext {
        private final RepairOrder order;

        private AcceptanceContext(RepairOrder order) { this.order = order; }
    }
}
