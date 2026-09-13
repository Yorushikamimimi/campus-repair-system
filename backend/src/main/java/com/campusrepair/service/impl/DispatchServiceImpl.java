package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.DispatchRecord;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.OrderStatusLog;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.SysRole;
import com.campusrepair.domain.SysUser;
import com.campusrepair.repository.DispatchRepository;
import com.campusrepair.repository.OrderStatusRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.repository.UserPermissionRepository;
import com.campusrepair.service.DispatchService;
import com.campusrepair.service.OrderStateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DispatchServiceImpl implements DispatchService {
    private static final String DISPATCH_STATUS_ASSIGNED = "ASSIGNED";
    private static final Set<String> ACTIVE_DISPATCH_STATUS = Set.of("ASSIGNED", "TAKEN");

    private final DispatchRepository dispatchRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final OrderStateService orderStateService;

    public DispatchServiceImpl(DispatchRepository dispatchRepository,
                               OrderStatusRepository orderStatusRepository,
                               RepairOrderRepository repairOrderRepository,
                               UserPermissionRepository userPermissionRepository,
                               OrderStateService orderStateService) {
        this.dispatchRepository = dispatchRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.repairOrderRepository = repairOrderRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.orderStateService = orderStateService;
    }

    @Override
    public void auditOrder(Long adminId, Long orderId, boolean approved, String comment) {
        RepairOrder order = requireOrder(orderId);
        requireAdmin(adminId);
        if (!OrderStatus.SUBMITTED.code().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前工单不能进行审核");
        }
        if (approved) {
            orderStateService.approve(orderId, adminId);
            return;
        }

        String reason = normalizeAuditComment(comment);
        appendLog(orderId, adminId, OrderStatus.SUBMITTED.code(), OrderStatus.SUBMITTED.code(), reason);
    }

    @Override
    public void dispatchOrder(Long adminId, Long orderId, Long maintainerId, String note) {
        RepairOrder order = requireOrder(orderId);
        requireAdmin(adminId);
        if (!OrderStatus.PENDING_PROCESS.code().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "仅可对待处理工单进行派单");
        }
        validateMaintainer(maintainerId);
        validateNoActiveDispatch(orderId);

        DispatchRecord record = new DispatchRecord();
        record.setOrderId(orderId);
        record.setAdminId(adminId);
        record.setMaintainerId(maintainerId);
        record.setDispatchTime(LocalDateTime.now());
        record.setDispatchNote(normalizeNote(note));
        record.changeStatus(DISPATCH_STATUS_ASSIGNED);
        dispatchRepository.save(record);

        orderStateService.dispatch(orderId, adminId);
    }

    @Override
    public List<RepairOrder> listPendingOrders() {
        List<RepairOrder> result = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (RepairOrder order : findPendingByStatus(OrderStatus.SUBMITTED.code())) {
            if (seen.add(order.getOrderId())) result.add(order);
        }
        for (RepairOrder order : findPendingByStatus(OrderStatus.PENDING_PROCESS.code())) {
            if (seen.add(order.getOrderId())) result.add(order);
        }
        return result;
    }

    private RepairOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        }
        RepairOrder order = repairOrderRepository.findById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        }
        return order;
    }

    private void requireAdmin(Long adminId) {
        if (adminId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateMaintainer(Long userId) {
        SysUser user = userPermissionRepository.findUserById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "维修人员不存在");
        }
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "维修人员不可用");
        }
        boolean isMaintainer = userPermissionRepository.findRolesByUserId(userId).stream()
                .map(SysRole::getRoleCode)
                .anyMatch("MAINTAINER"::equals);
        if (!isMaintainer) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "目标用户不是维修人员");
        }
    }

    private void validateNoActiveDispatch(Long orderId) {
        DispatchRecord latest = dispatchRepository.findLatestByOrder(orderId);
        if (latest != null && isActiveDispatchStatus(latest.getDispatchStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该工单存在进行中的派单");
        }
    }

    private void appendLog(Long orderId, Long operatorId, String oldStatus, String newStatus, String reason) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setChangeReason(reason);
        log.setChangeTime(LocalDateTime.now());
        orderStatusRepository.appendLog(log);
    }

    private List<RepairOrder> findPendingByStatus(String status) {
        List<RepairOrder> orders = repairOrderRepository.findByFilters(status, null);
        return orders == null ? List.of() : orders;
    }

    private boolean isActiveDispatchStatus(String status) {
        if (status == null || status.isBlank()) return false;
        return ACTIVE_DISPATCH_STATUS.contains(status);
    }

    private String normalizeAuditComment(String comment) {
        String normalized = comment == null ? "" : comment.trim();
        return normalized.isBlank() ? "退回补充" : normalized;
    }

    private String normalizeNote(String note) {
        return note == null ? "" : note.trim();
    }
}

