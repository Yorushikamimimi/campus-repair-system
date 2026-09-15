package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.DispatchRecord;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairRecord;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.dto.MaintenanceProcessRequest;
import com.campusrepair.dto.MaintenanceTaskItem;
import com.campusrepair.repository.DispatchRepository;
import com.campusrepair.repository.MaintenanceRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.MaintenanceService;
import com.campusrepair.service.OrderStateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {
    private static final String DISPATCH_STATUS_ASSIGNED = "ASSIGNED";
    private static final String DISPATCH_STATUS_TAKEN = "TAKEN";
    private static final String DISPATCH_STATUS_COMPLETED = "COMPLETED";

    private final RepairOrderRepository repairOrderRepository;
    private final DispatchRepository dispatchRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final OrderStateService orderStateService;

    public MaintenanceServiceImpl(RepairOrderRepository repairOrderRepository,
                                 DispatchRepository dispatchRepository,
                                 MaintenanceRepository maintenanceRepository,
                                 OrderStateService orderStateService) {
        this.repairOrderRepository = repairOrderRepository;
        this.dispatchRepository = dispatchRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.orderStateService = orderStateService;
    }

    @Override
    public List<MaintenanceTaskItem> listTasks(Long maintainerId) {
        requireUserId(maintainerId);
        List<DispatchRecord> records = dispatchRepository.findTasksByMaintainer(maintainerId);
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        Map<Long, DispatchRecord> latestByOrder = new LinkedHashMap<>();
        for (DispatchRecord record : records) {
            latestByOrder.putIfAbsent(record.getOrderId(), record);
        }

        List<MaintenanceTaskItem> result = new ArrayList<>();
        for (DispatchRecord record : latestByOrder.values()) {
            RepairOrder order = repairOrderRepository.findById(record.getOrderId());
            if (order == null) {
                continue;
            }
            if (!isCurrentTaskVisible(order)) {
                continue;
            }
            result.add(new MaintenanceTaskItem(
                    order.getOrderId(),
                    order.getTitle(),
                    order.getTypeId(),
                    order.getLocationId(),
                    order.getStatus(),
                    record.getDispatchTime(),
                    record.getDispatchNote()
            ));
        }
        return result.stream()
                .sorted((left, right) -> {
                    LocalDateTime leftTime = left.dispatchTime();
                    LocalDateTime rightTime = right.dispatchTime();
                    if (leftTime == null && rightTime == null) return 0;
                    if (leftTime == null) return 1;
                    if (rightTime == null) return -1;
                    return rightTime.compareTo(leftTime);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void acceptTask(Long maintainerId, Long orderId) {
        TaskContext context = validateAssignment(maintainerId, orderId);
        OrderStatus current = requireCurrentState(context.order);
        if (current == OrderStatus.PROCESSING && DISPATCH_STATUS_TAKEN.equals(context.dispatch.getDispatchStatus())) {
            return;
        }
        if (current != OrderStatus.PENDING_PROCESS && current != OrderStatus.REWORK) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前状态不允许接单");
        }
        dispatchRepository.updateStatus(context.dispatch.getDispatchId(), DISPATCH_STATUS_TAKEN);
        orderStateService.startRepair(context.order.getOrderId(), maintainerId);
    }

    @Override
    @Transactional
    public void processRepairOrder(Long maintainerId, Long orderId, MaintenanceProcessRequest request) {
        TaskContext context = validateAssignment(maintainerId, orderId);
        validateRepairState(context);
        Boolean completedFlag = request.getCompleted();
        if (completedFlag == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "完成标识不能为空");
        }
        boolean finished = completedFlag;
        ensureRecordComplete(request, finished);

        OrderStatus nextState = resolveNextState(finished);
        handlePersistenceFailure(() -> {
            saveResult(context.order.getOrderId(), maintainerId, request, finished);
            if (nextState == OrderStatus.PENDING_ACCEPTANCE) {
                orderStateService.finishRepair(orderId, maintainerId);
                dispatchRepository.updateStatus(context.dispatch.getDispatchId(), DISPATCH_STATUS_COMPLETED);
            }
        });
    }

    @Override
    public void continueRepair(Long maintainerId, Long orderId) {
        TaskContext context = validateAssignment(maintainerId, orderId);
        OrderStatus current = requireCurrentState(context.order);
        if (current == OrderStatus.PROCESSING) {
            return;
        }
        if (current != OrderStatus.REWORK) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前维修工单无需继续处理");
        }
        if (!DISPATCH_STATUS_TAKEN.equals(context.dispatch.getDispatchStatus())) {
            dispatchRepository.updateStatus(context.dispatch.getDispatchId(), DISPATCH_STATUS_TAKEN);
        }
        orderStateService.reopen(context.order.getOrderId(), maintainerId);
    }

    @Override
    public List<RepairRecord> getRepairHistory(Long orderId) {
        requireOrder(orderId);
        return maintenanceRepository.findByOrder(orderId);
    }

    private TaskContext validateAssignment(Long maintainerId, Long orderId) {
        requireUserId(maintainerId);
        RepairOrder order = requireOrder(orderId);
        DispatchRecord latest = dispatchRepository.findLatestByOrder(orderId);
        if (latest == null || !maintainerId.equals(latest.getMaintainerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能处理本人已派发的工单");
        }
        OrderStatus current = requireCurrentState(order);
        if (!isActiveAssignmentStatusForFlow(latest.getDispatchStatus(), current)) {
            throw new BusinessException(ErrorCode.CONFLICT, "工单派发状态不允许当前操作");
        }
        return new TaskContext(order, latest);
    }

    private boolean isActiveAssignmentStatusForFlow(String dispatchStatus, OrderStatus currentOrderStatus) {
        if (DISPATCH_STATUS_ASSIGNED.equals(dispatchStatus) || DISPATCH_STATUS_TAKEN.equals(dispatchStatus)) {
            return true;
        }
        return OrderStatus.REWORK.equals(currentOrderStatus) && DISPATCH_STATUS_COMPLETED.equals(dispatchStatus);
    }

    private void validateRepairState(TaskContext context) {
        OrderStatus current = requireCurrentState(context.order);
        if (current != OrderStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前状态不允许提交维修结果");
        }
        if (!DISPATCH_STATUS_TAKEN.equals(context.dispatch.getDispatchStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "请先接单后提交处理结果");
        }
    }

    private void ensureRecordComplete(MaintenanceProcessRequest request, boolean finished) {
        validateProcessDescription(request.getProcessDesc());
        if (finished) {
            validateCompletedRecord(request);
        } else {
            validateProcessingRecord(request);
        }
    }

    private void validateProcessDescription(String processDesc) {
        String normalized = normalizeText(processDesc);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "维修过程不能为空");
        }
    }

    private void validateCompletedRecord(MaintenanceProcessRequest request) {
        if (normalizeText(request.getRepairResult()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "维修结果不能为空");
        }
    }

    private void validateProcessingRecord(MaintenanceProcessRequest request) {
        if (normalizeText(request.getUnfinishedReason()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未完成原因不能为空");
        }
        if (request.getRepairResult() != null && !request.getRepairResult().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未完成时不能填写维修结果");
        }
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private OrderStatus resolveNextState(boolean finished) {
        return finished ? OrderStatus.PENDING_ACCEPTANCE : OrderStatus.PROCESSING;
    }

    private void saveResult(Long orderId, Long maintainerId, MaintenanceProcessRequest request, boolean finished) {
        String processDesc = normalizeText(request.getProcessDesc());
        String normalizedResult = normalizeText(request.getRepairResult());
        String normalizedUnfinished = normalizeText(request.getUnfinishedReason());
        if (processDesc == null || processDesc.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "维修过程不能为空");
        }
        RepairRecord record = new RepairRecord();
        record.setOrderId(orderId);
        record.setMaintainerId(maintainerId);
        record.setStartTime(LocalDateTime.now());
        record.setProcessDesc(processDesc);
        if (finished) {
            record.setRepairResult(normalizedResult);
            record.setEndTime(LocalDateTime.now());
            record.recordUnfinished(null);
        } else {
            record.recordUnfinished(normalizedUnfinished);
        }
        maintenanceRepository.saveRepairRecord(record);
    }

    private void handlePersistenceFailure(Runnable action) {
        try {
            action.run();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "维修处理信息保存失败");
        }
    }

    private boolean isCurrentTaskVisible(RepairOrder order) {
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        if (status == null) {
            return false;
        }
        return status == OrderStatus.PENDING_PROCESS
                || status == OrderStatus.PROCESSING
                || status == OrderStatus.REWORK;
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

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private OrderStatus requireCurrentState(RepairOrder order) {
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单状态非法");
        }
        return current;
    }

    private static class TaskContext {
        private final RepairOrder order;
        private final DispatchRecord dispatch;

        private TaskContext(RepairOrder order, DispatchRecord dispatch) {
            this.order = order;
            this.dispatch = dispatch;
        }
    }
}
