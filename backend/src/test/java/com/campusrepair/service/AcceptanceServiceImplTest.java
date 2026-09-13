package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.AcceptanceRecord;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.AcceptanceRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.impl.AcceptanceServiceImpl;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptanceServiceImplTest {
    @Mock
    AcceptanceRepository acceptanceRepository;
    @Mock
    RepairOrderRepository repairOrderRepository;
    @Mock
    OrderStateService orderStateService;

    private AcceptanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AcceptanceServiceImpl(acceptanceRepository, repairOrderRepository, orderStateService);
    }

    @Test
    void passAcceptanceCreatesPassedRecordAndAcceptsOrder() {
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PENDING_ACCEPTANCE.code()));

        AcceptanceRecord result = service.acceptRepairResult(9L, 101L, true, null);

        ArgumentCaptor<AcceptanceRecord> captor = ArgumentCaptor.forClass(AcceptanceRecord.class);
        verify(acceptanceRepository).saveAcceptance(captor.capture());
        verify(orderStateService).accept(101L, 9L);

        AcceptanceRecord record = captor.getValue();
        assertEquals("PASSED", record.getAcceptResult());
        assertEquals(null, record.getReturnReason());
        assertEquals(101L, record.getOrderId());
        assertEquals(9L, record.getReporterId());
        assertEquals("PASSED", result.getAcceptResult());
    }

    @Test
    void failAcceptanceRequiresReasonAndRejectsToRework() {
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PENDING_ACCEPTANCE.code()));

        AcceptanceRecord result = service.acceptRepairResult(9L, 101L, false, "零件未修复");

        ArgumentCaptor<AcceptanceRecord> captor = ArgumentCaptor.forClass(AcceptanceRecord.class);
        verify(acceptanceRepository).saveAcceptance(captor.capture());
        verify(orderStateService).reject(101L, 9L, "零件未修复");

        AcceptanceRecord record = captor.getValue();
        assertEquals("REJECTED", record.getAcceptResult());
        assertEquals("零件未修复", record.getReturnReason());
        assertEquals(101L, record.getOrderId());
        assertEquals("REJECTED", result.getAcceptResult());
    }

    @Test
    void failAcceptanceEmptyReasonIsRejected() {
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PENDING_ACCEPTANCE.code()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.acceptRepairResult(9L, 101L, false, " "));

        assertSame(ErrorCode.BAD_REQUEST, ex.errorCode());
        verify(orderStateService, never()).reject(101L, 9L, "");
        verifyNoInteractions(acceptanceRepository);
    }

    @Test
    void nonOwnerCannotAccept() {
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 8L, OrderStatus.PENDING_ACCEPTANCE.code()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.acceptRepairResult(9L, 101L, true, null));

        assertSame(ErrorCode.FORBIDDEN, ex.errorCode());
        verifyNoInteractions(orderStateService, acceptanceRepository);
    }

    @Test
    void acceptOnlyAllowedWhenStatusPendingAcceptance() {
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PROCESSING.code()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.acceptRepairResult(9L, 101L, true, null));

        assertSame(ErrorCode.CONFLICT, ex.errorCode());
        verifyNoInteractions(acceptanceRepository);
        verifyNoInteractions(orderStateService);
    }

    @Test
    void requestReworkReusesAcceptanceFailureFlow() {
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PENDING_ACCEPTANCE.code()));

        service.requestRework(9L, 101L, "返回验收");

        verify(orderStateService).reject(101L, 9L, "返回验收");
        verify(acceptanceRepository).saveAcceptance(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ownerCanGetAcceptanceHistory() {
        List<AcceptanceRecord> records = List.of(record(1L, 101L, "PASSED"), record(2L, 101L, "REJECTED"));
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PENDING_ACCEPTANCE.code()));
        when(acceptanceRepository.findByOrder(101L)).thenReturn(records);

        List<AcceptanceRecord> result = service.getAcceptanceHistory(9L, 101L, false);

        assertEquals(records, result);
        verify(acceptanceRepository).findByOrder(101L);
    }

    @Test
    void adminCanGetAcceptanceHistoryByOrder() {
        List<AcceptanceRecord> records = List.of(record(1L, 101L, "PASSED"), record(2L, 101L, "PASSED"));
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PENDING_ACCEPTANCE.code()));
        when(acceptanceRepository.findByOrder(101L)).thenReturn(records);

        List<AcceptanceRecord> result = service.getAcceptanceHistory(8L, 101L, true);

        assertEquals(records, result);
        verify(acceptanceRepository).findByOrder(101L);
    }

    @Test
    void nonOwnerReporterCannotReadAcceptanceHistoryWithoutAdmin() {
        when(repairOrderRepository.findById(101L)).thenReturn(order(101L, 9L, OrderStatus.PENDING_ACCEPTANCE.code()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAcceptanceHistory(8L, 101L, false));

        assertSame(ErrorCode.FORBIDDEN, ex.errorCode());
        verify(acceptanceRepository, never()).findByOrder(101L);
    }

    private RepairOrder order(Long orderId, Long reporterId, String status) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setReporterId(reporterId);
        order.setStatus(status);
        return order;
    }

    private AcceptanceRecord record(Long acceptanceId, Long orderId, String result) {
        AcceptanceRecord record = new AcceptanceRecord();
        record.setAcceptanceId(acceptanceId);
        record.setOrderId(orderId);
        record.setAcceptResult(result);
        return record;
    }
}

