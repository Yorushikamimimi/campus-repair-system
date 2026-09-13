package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.EvaluationRecord;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.EvaluationRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.EvaluationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationServiceImpl implements EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final RepairOrderRepository repairOrderRepository;

    public EvaluationServiceImpl(EvaluationRepository evaluationRepository,
                                RepairOrderRepository repairOrderRepository) {
        this.evaluationRepository = evaluationRepository;
        this.repairOrderRepository = repairOrderRepository;
    }

    @Override
    @Transactional
    public EvaluationRecord evaluateService(Long reporterId, Long orderId, Integer score, String comment) {
        RepairOrder order = requireOwnedOrder(reporterId, orderId);
        requireCompleted(order);
        if (orderId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        if (score == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "评分不能为空");
        EvaluationRecord record = new EvaluationRecord();
        record.setOrderId(orderId);
        record.setReporterId(reporterId);
        record.setScore(score);
        record.setComment(comment);
        validateScore(record);
        if (evaluationRepository.existsByOrder(orderId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该工单已评价");
        }
        return evaluationRepository.saveEvaluation(record);
    }

    @Override
    public EvaluationRecord getEvaluation(Long userId, Long orderId, boolean allowAdmin) {
        if (orderId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        RepairOrder order = requireOrder(orderId);
        if (!allowAdmin && !order.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看本人评价");
        }
        var latest = evaluationRepository.findByOrder(orderId);
        if (latest.isEmpty()) return null;
        return latest.get(latest.size() - 1);
    }

    private RepairOrder requireOwnedOrder(Long reporterId, Long orderId) {
        RepairOrder order = requireOrder(orderId);
        if (reporterId == null || !order.isOwnedBy(reporterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能评价本人报修单");
        }
        return order;
    }

    private RepairOrder requireOrder(Long orderId) {
        if (orderId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        RepairOrder order = repairOrderRepository.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        return order;
    }

    private void requireCompleted(RepairOrder order) {
        if (order == null || !"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "仅已完成工单可评价");
        }
    }

    private void validateScore(EvaluationRecord record) {
        if (!record.validateScore()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评分范围应为1到5");
        }
    }
}
