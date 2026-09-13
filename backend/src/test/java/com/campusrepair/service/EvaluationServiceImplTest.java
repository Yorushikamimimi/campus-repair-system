package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.EvaluationRecord;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.EvaluationRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.impl.EvaluationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {
    @Mock
    EvaluationRepository evaluationRepository;
    @Mock
    RepairOrderRepository repairOrderRepository;

    private EvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluationServiceImpl(evaluationRepository, repairOrderRepository);
    }

    @Test
    void evaluateCompletedOrderSuccess() {
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 9L, OrderStatus.COMPLETED.code()));
        when(evaluationRepository.existsByOrder(201L)).thenReturn(false);
        when(evaluationRepository.saveEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));

        EvaluationRecord result = service.evaluateService(9L, 201L, 5, "处理很快");

        ArgumentCaptor<EvaluationRecord> captor = ArgumentCaptor.forClass(EvaluationRecord.class);
        verify(evaluationRepository).saveEvaluation(captor.capture());

        EvaluationRecord saved = captor.getValue();
        assertEquals(201L, saved.getOrderId());
        assertEquals(9L, saved.getReporterId());
        assertEquals(5, saved.getScore());
        assertEquals("处理很快", saved.getComment());
        assertEquals(5, result.getScore());
    }

    @Test
    void evaluateOnlyForCompletedOrder() {
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 9L, OrderStatus.PROCESSING.code()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.evaluateService(9L, 201L, 5, "测试"));

        assertSame(ErrorCode.CONFLICT, ex.errorCode());
        verify(evaluationRepository, never()).saveEvaluation(any());
    }

    @Test
    void nonOwnerCannotEvaluate() {
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 8L, OrderStatus.COMPLETED.code()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.evaluateService(9L, 201L, 5, "测试"));

        assertSame(ErrorCode.FORBIDDEN, ex.errorCode());
        verify(evaluationRepository, never()).saveEvaluation(any());
    }

    @Test
    void scoreLowerBoundValidated() {
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 9L, OrderStatus.COMPLETED.code()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.evaluateService(9L, 201L, 0, "too low"));

        assertSame(ErrorCode.BAD_REQUEST, ex.errorCode());
        verify(evaluationRepository, never()).saveEvaluation(any());
    }

    @Test
    void scoreUpperBoundValidated() {
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 9L, OrderStatus.COMPLETED.code()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.evaluateService(9L, 201L, 6, "too high"));

        assertSame(ErrorCode.BAD_REQUEST, ex.errorCode());
        verify(evaluationRepository, never()).saveEvaluation(any());
    }

    @Test
    void duplicateEvaluationRejected() {
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 9L, OrderStatus.COMPLETED.code()));
        when(evaluationRepository.existsByOrder(201L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.evaluateService(9L, 201L, 4, "repeat"));

        assertSame(ErrorCode.CONFLICT, ex.errorCode());
        verify(evaluationRepository, never()).saveEvaluation(any());
    }

    @Test
    void getEvaluationReturnsLatestRecordForOwner() {
        EvaluationRecord first = evaluation(1L, 201L, 4, "慢");
        EvaluationRecord second = evaluation(2L, 201L, 5, "快");
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 9L, OrderStatus.COMPLETED.code()));
        when(evaluationRepository.findByOrder(201L)).thenReturn(List.of(first, second));

        EvaluationRecord result = service.getEvaluation(9L, 201L, false);

        assertEquals(second, result);
        assertEquals(2L, result.getEvaluationId());
        assertEquals(5, result.getScore());
        assertEquals("快", result.getComment());
    }

    @Test
    void adminCanGetEvaluationOfOthers() {
        EvaluationRecord first = evaluation(1L, 201L, 4, "慢");
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 8L, OrderStatus.COMPLETED.code()));
        when(evaluationRepository.findByOrder(201L)).thenReturn(List.of(first));

        EvaluationRecord result = service.getEvaluation(1L, 201L, true);

        assertEquals(first, result);
        assertEquals(4, result.getScore());
    }

    @Test
    void getEvaluationReturnsNullWhenNoRecord() {
        when(repairOrderRepository.findById(201L)).thenReturn(order(201L, 9L, OrderStatus.COMPLETED.code()));
        when(evaluationRepository.findByOrder(201L)).thenReturn(List.of());

        EvaluationRecord result = service.getEvaluation(9L, 201L, false);

        assertEquals(null, result);
    }

    private RepairOrder order(Long orderId, Long reporterId, String status) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setReporterId(reporterId);
        order.setStatus(status);
        return order;
    }

    private EvaluationRecord evaluation(Long evaluationId, Long orderId, Integer score, String comment) {
        EvaluationRecord record = new EvaluationRecord();
        record.setEvaluationId(evaluationId);
        record.setOrderId(orderId);
        record.setScore(score);
        record.setComment(comment);
        return record;
    }
}
