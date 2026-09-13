package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.OrderStatusLog;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.OrderStatusRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.OrderStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class OrderStateServiceImpl implements OrderStateService {
    private final RepairOrderRepository repairOrderRepository;
    private final OrderStatusRepository orderStatusRepository;

    public OrderStateServiceImpl(RepairOrderRepository repairOrderRepository,
                                 OrderStatusRepository orderStatusRepository) {
        this.repairOrderRepository = repairOrderRepository;
        this.orderStatusRepository = orderStatusRepository;
    }

    @Override
    @Transactional
    public void submit(Long orderId, Long operatorId) {
        RepairOrder order = requireOrder(orderId);
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.SUBMITTED.code());
            repairOrderRepository.updateOrder(order);
            appendLog(orderId, operatorId, null, OrderStatus.SUBMITTED.code(), "提交报修");
            return;
        }
        if (!OrderStatus.SUBMITTED.code().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前工单不能提交");
        }
        if (orderStatusRepository.findLatest(orderId) == null) {
            appendLog(orderId, operatorId, null, OrderStatus.SUBMITTED.code(), "提交报修");
        }
    }

    @Override
    @Transactional
    public void approve(Long orderId, Long operatorId) {
        transition(orderId, operatorId, OrderStatus.PENDING_PROCESS, Set.of(OrderStatus.SUBMITTED), "审核通过");
    }

    @Override
    @Transactional
    public void dispatch(Long orderId, Long operatorId) {
        RepairOrder order = requireOrder(orderId);
        requireCurrent(order, Set.of(OrderStatus.PENDING_PROCESS));
        appendLog(orderId, operatorId, order.getStatus(), order.getStatus(), "完成派单");
    }

    @Override
    @Transactional
    public void startRepair(Long orderId, Long operatorId) {
        transition(orderId, operatorId, OrderStatus.PROCESSING,
                Set.of(OrderStatus.PENDING_PROCESS, OrderStatus.REWORK), "开始维修");
    }

    @Override
    @Transactional
    public void finishRepair(Long orderId, Long operatorId) {
        transition(orderId, operatorId, OrderStatus.PENDING_ACCEPTANCE,
                Set.of(OrderStatus.PROCESSING), "维修完成，等待验收");
    }

    @Override
    @Transactional
    public void accept(Long orderId, Long operatorId) {
        transition(orderId, operatorId, OrderStatus.COMPLETED,
                Set.of(OrderStatus.PENDING_ACCEPTANCE), "验收通过");
    }

    @Override
    @Transactional
    public void reject(Long orderId, Long operatorId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "拒绝原因不能为空");
        }
        transition(orderId, operatorId, OrderStatus.REWORK,
                Set.of(OrderStatus.PENDING_ACCEPTANCE), reason.trim());
    }

    @Override
    @Transactional
    public void reopen(Long orderId, Long operatorId) {
        transition(orderId, operatorId, OrderStatus.PROCESSING,
                Set.of(OrderStatus.REWORK), "重新开始维修");
    }

    private void transition(Long orderId, Long operatorId, OrderStatus target,
                            Set<OrderStatus> allowed, String reason) {
        RepairOrder order = requireOrder(orderId);
        OrderStatus old = requireCurrent(order, allowed);
        order.setStatus(target.code());
        repairOrderRepository.updateOrder(order);
        appendLog(orderId, operatorId, old.code(), target.code(), reason);
    }

    private OrderStatus requireCurrent(RepairOrder order, Set<OrderStatus> allowed) {
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null || !allowed.contains(current)) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前状态不允许该操作");
        }
        return current;
    }

    private RepairOrder requireOrder(Long orderId) {
        if (orderId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        RepairOrder order = repairOrderRepository.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        return order;
    }

    private void appendLog(Long orderId, Long operatorId, String oldStatus,
                           String newStatus, String reason) {
        if (operatorId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setChangeReason(reason);
        log.setChangeTime(LocalDateTime.now());
        orderStatusRepository.appendLog(log);
    }
}
