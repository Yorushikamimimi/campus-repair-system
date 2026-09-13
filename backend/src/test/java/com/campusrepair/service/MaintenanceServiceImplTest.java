package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.DispatchRecord;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.RepairRecord;
import com.campusrepair.dto.MaintenanceProcessRequest;
import com.campusrepair.dto.MaintenanceTaskItem;
import com.campusrepair.repository.DispatchRepository;
import com.campusrepair.repository.MaintenanceRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.impl.MaintenanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceImplTest {
    @Mock
    RepairOrderRepository orderRepository;
    @Mock
    DispatchRepository dispatchRepository;
    @Mock
    MaintenanceRepository maintenanceRepository;
    @Mock
    OrderStateService orderStateService;

    private MaintenanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MaintenanceServiceImpl(orderRepository, dispatchRepository, maintenanceRepository, orderStateService);
    }

    @Test
    void listTasksOnlyReturnsLatestVisibleOrdersForMaintainer() {
        when(dispatchRepository.findTasksByMaintainer(8L)).thenReturn(List.of(
                dispatch(101L, 8L, "ASSIGNED", LocalDateTime.of(2026, 9, 12, 10, 0)),
                dispatch(102L, 8L, "ASSIGNED", LocalDateTime.of(2026, 9, 12, 9, 0)),
                dispatch(101L, 8L, "ASSIGNED", LocalDateTime.of(2026, 9, 12, 8, 0))
        ));
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PENDING_PROCESS.code()));
        when(orderRepository.findById(102L)).thenReturn(order(102L, OrderStatus.COMPLETED.code()));

        List<MaintenanceTaskItem> result = service.listTasks(8L);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).orderId());
    }

    @Test
    void acceptTaskRequiresAssignedOrReworkStatusAndAssignments() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PENDING_PROCESS.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 8L, "ASSIGNED", LocalDateTime.now()));

        service.acceptTask(8L, 101L);

        verify(dispatchRepository).updateStatus(1201L, "TAKEN");
        verify(orderStateService).startRepair(101L, 8L);
    }

    @Test
    void acceptTaskForUnrelatedMaintainerIsForbidden() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PENDING_PROCESS.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 9L, "ASSIGNED", LocalDateTime.now()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.acceptTask(8L, 101L));
        assertSame(ErrorCode.FORBIDDEN, ex.errorCode());
        verify(orderStateService, never()).startRepair(101L, 8L);
    }

    @Test
    void acceptTaskIsIdempotentWhenAlreadyTaken() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PROCESSING.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 8L, "TAKEN", LocalDateTime.now()));

        service.acceptTask(8L, 101L);

        verify(orderStateService, never()).startRepair(101L, 8L);
        verify(dispatchRepository, never()).updateStatus(201L, "TAKEN");
    }

    @Test
    void processRepairOrderFinishedMovesToPendingAcceptance() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PROCESSING.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 8L, "TAKEN", LocalDateTime.now()));
        when(maintenanceRepository.saveRepairRecord(org.mockito.ArgumentMatchers.any())).thenReturn(new RepairRecord());

        MaintenanceProcessRequest request = request("更换阀门", true, "修好了", null);
        service.processRepairOrder(8L, 101L, request);

        ArgumentCaptor<RepairRecord> captor = ArgumentCaptor.forClass(RepairRecord.class);
        verify(maintenanceRepository).saveRepairRecord(captor.capture());
        assertEquals("更换阀门", captor.getValue().getProcessDesc());
        assertEquals("修好了", captor.getValue().getRepairResult());
        verify(orderStateService).finishRepair(101L, 8L);
        verify(dispatchRepository).updateStatus(1201L, "COMPLETED");
    }

    @Test
    void processRepairOrderNotFinishedKeepsProcessing() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PROCESSING.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 8L, "TAKEN", LocalDateTime.now()));
        when(maintenanceRepository.saveRepairRecord(org.mockito.ArgumentMatchers.any())).thenReturn(new RepairRecord());

        MaintenanceProcessRequest request = request("暂时停电", false, null, "等待零件");
        service.processRepairOrder(8L, 101L, request);

        verify(orderStateService, never()).finishRepair(101L, 8L);
        verify(dispatchRepository, never()).updateStatus(201L, "COMPLETED");
        verify(maintenanceRepository).saveRepairRecord(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void processRepairOrderPersistsFailureIsWrappedAsInternalError() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PROCESSING.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 8L, "TAKEN", LocalDateTime.now()));
        when(maintenanceRepository.saveRepairRecord(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("db unavailable"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.processRepairOrder(8L, 101L, request("更换滤芯", true, "已修好", null))
        );
        assertSame(ErrorCode.INTERNAL_ERROR, ex.errorCode());
        verify(orderStateService, never()).finishRepair(101L, 8L);
    }

    @Test
    void continueRepairAllowedOnlyWhenInRework() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.REWORK.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 8L, "ASSIGNED", LocalDateTime.now()));

        service.continueRepair(8L, 101L);

        verify(orderStateService).reopen(101L, 8L);
    }

    @Test
    void continueRepairAfterCompletedDispatchWillResetToTaken() {
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.REWORK.code()));
        when(dispatchRepository.findLatestByOrder(101L)).thenReturn(dispatch(201L, 8L, "COMPLETED", LocalDateTime.now()));

        service.continueRepair(8L, 101L);

        verify(dispatchRepository).updateStatus(1201L, "TAKEN");
        verify(orderStateService).reopen(101L, 8L);
    }

    @Test
    void getRepairHistoryReturnsRepairRecords() {
        RepairRecord first = new RepairRecord();
        first.setRepairId(1L);
        when(orderRepository.findById(101L)).thenReturn(order(101L, OrderStatus.PENDING_PROCESS.code()));
        when(maintenanceRepository.findByOrder(101L)).thenReturn(List.of(first));

        List<RepairRecord> history = service.getRepairHistory(101L);

        assertEquals(1, history.size());
        assertEquals(1L, history.get(0).getRepairId());
    }

    private RepairOrder order(Long orderId, String status) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setStatus(status);
        return order;
    }

    private DispatchRecord dispatch(Long orderId, Long maintainerId, String status, LocalDateTime time) {
        DispatchRecord dispatch = new DispatchRecord();
        dispatch.setOrderId(orderId);
        dispatch.setDispatchId(1000L + orderId);
        dispatch.setMaintainerId(maintainerId);
        dispatch.setDispatchStatus(status);
        dispatch.setDispatchTime(time);
        return dispatch;
    }

    private MaintenanceProcessRequest request(String process, boolean completed, String result, String unfinishedReason) {
        MaintenanceProcessRequest request = new MaintenanceProcessRequest();
        request.setProcessDesc(process);
        request.setCompleted(completed);
        request.setRepairResult(result);
        request.setUnfinishedReason(unfinishedReason);
        return request;
    }
}
