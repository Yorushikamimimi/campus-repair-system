package com.campusrepair.service;

import com.campusrepair.domain.EvaluationRecord;

public interface EvaluationService {
    EvaluationRecord evaluateService(Long reporterId, Long orderId, Integer score, String comment);
    EvaluationRecord getEvaluation(Long userId, Long orderId, boolean allowAdmin);
}

