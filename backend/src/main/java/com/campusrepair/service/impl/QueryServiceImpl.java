package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.QueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryServiceImpl implements QueryService {
    private final RepairOrderRepository repairOrderRepository;

    public QueryServiceImpl(RepairOrderRepository repairOrderRepository) {
        this.repairOrderRepository = repairOrderRepository;
    }

    @Override
    public RepairOrder queryOrderProgress(Long userId, Long orderId) {
        RepairOrder order = requireOrder(orderId);
        checkQueryPermission(userId, order);
        return order;
    }

    @Override
    public List<RepairOrder> queryOrders(Long userId, String status, Long typeId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        String normalized = normalizeStatus(status);
        return repairOrderRepository.findByReporter(userId).stream()
                .filter(order -> normalized.isBlank() || normalized.equals(order.getStatus()))
                .filter(order -> typeId == null || typeId.equals(order.getTypeId()))
                .toList();
    }

    private RepairOrder requireOrder(Long orderId) {
        if (orderId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        RepairOrder order = repairOrderRepository.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        return order;
    }

    private void checkQueryPermission(Long userId, RepairOrder order) {
        if (userId == null || !order.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查询自己的工单");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "";
        return normalizeStatusValue(status.trim());
    }

    private String normalizeStatusValue(String value) {
        OrderStatus code = OrderStatus.fromCode(value);
        if (code != null) return code.code();
        return displayNameToCode(value);
    }

    private String displayNameToCode(String value) {
        for (OrderStatus item : OrderStatus.values()) {
            if (item.displayName().equals(value)) return item.code();
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "工单状态不合法");
    }
}
