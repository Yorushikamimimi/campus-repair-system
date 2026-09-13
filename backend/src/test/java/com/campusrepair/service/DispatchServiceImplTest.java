package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.DispatchRecord;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.SysRole;
import com.campusrepair.domain.SysUser;
import com.campusrepair.repository.DispatchRepository;
import com.campusrepair.repository.OrderStatusRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.repository.UserPermissionRepository;
import com.campusrepair.service.impl.DispatchServiceImpl;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceImplTest {
    @Mock
    DispatchRepository dispatchRepository;
    @Mock
    OrderStatusRepository orderStatusRepository;
    @Mock
    RepairOrderRepository orderRepository;
    @Mock
    UserPermissionRepository userPermissionRepository;
    @Mock
    OrderStateService orderStateService;

    private DispatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DispatchServiceImpl(dispatchRepository, orderStatusRepository,
                orderRepository, userPermissionRepository, orderStateService);
    }

    @Test
    void auditApprovedOrderMovesToPendingProcess() {
        when(orderRepository.findById(10L)).thenReturn(order(10L, OrderStatus.SUBMITTED.code()));

        service.auditOrder(1L, 10L, true, null);

        verify(orderStateService).approve(10L, 1L);
        verify(orderStatusRepository, never()).appendLog(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void auditRejectedOrderKeepsSubmittedAndWritesLog() {
        when(orderRepository.findById(10L)).thenReturn(order(10L, OrderStatus.SUBMITTED.code()));

        service.auditOrder(1L, 10L, false, "资料不完整");

        verify(orderStatusRepository).appendLog(org.mockito.ArgumentMatchers.argThat(log ->
                OrderStatus.SUBMITTED.code().equals(log.getOldStatus())
                        && OrderStatus.SUBMITTED.code().equals(log.getNewStatus())
                        && "资料不完整".equals(log.getChangeReason())));
        verify(orderStateService, never()).approve(10L, 1L);
    }

    @Test
    void auditOrderRejectsNonSubmittedOrder() {
        when(orderRepository.findById(11L)).thenReturn(order(11L, OrderStatus.PENDING_PROCESS.code()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.auditOrder(1L, 11L, true, null));
        assertSame(ErrorCode.CONFLICT, ex.errorCode());
        verify(orderStateService, never()).approve(11L, 1L);
    }

    @Test
    void dispatchOrderValidatesMaintainerAndCreatesRecord() {
        when(orderRepository.findById(10L)).thenReturn(order(10L, OrderStatus.PENDING_PROCESS.code()));
        when(userPermissionRepository.findUserById(8L)).thenReturn(maintainer(8L, true, "MAINTAINER"));
        when(userPermissionRepository.findRolesByUserId(8L)).thenReturn(List.of(role("MAINTAINER")));
        when(dispatchRepository.findLatestByOrder(10L)).thenReturn(null);

        service.dispatchOrder(1L, 10L, 8L, "  ");

        ArgumentCaptor<DispatchRecord> captor = ArgumentCaptor.forClass(DispatchRecord.class);
        verify(dispatchRepository).save(captor.capture());
        DispatchRecord saved = captor.getValue();
        assertEquals("ASSIGNED", saved.getDispatchStatus());
        assertEquals("", saved.getDispatchNote());
        verify(orderStateService).dispatch(10L, 1L);
    }

    @Test
    void dispatchOrderRejectsNonActiveMaintainer() {
        when(orderRepository.findById(10L)).thenReturn(order(10L, OrderStatus.PENDING_PROCESS.code()));
        when(userPermissionRepository.findUserById(8L)).thenReturn(maintainer(8L, true, "REPORTER"));
        when(userPermissionRepository.findRolesByUserId(8L)).thenReturn(List.of(role("REPORTER")));

        assertThrows(BusinessException.class, () -> service.dispatchOrder(1L, 10L, 8L, "note"));
    }

    @Test
    void dispatchOrderRejectsActiveDispatchConflict() {
        when(orderRepository.findById(10L)).thenReturn(order(10L, OrderStatus.PENDING_PROCESS.code()));
        when(userPermissionRepository.findUserById(8L)).thenReturn(maintainer(8L, true, "MAINTAINER"));
        when(userPermissionRepository.findRolesByUserId(8L)).thenReturn(List.of(role("MAINTAINER")));
        DispatchRecord active = new DispatchRecord();
        active.setDispatchId(5L);
        active.setDispatchStatus("ASSIGNED");
        when(dispatchRepository.findLatestByOrder(10L)).thenReturn(active);

        assertThrows(BusinessException.class, () -> service.dispatchOrder(1L, 10L, 8L, "note"));
        verify(orderStateService, never()).dispatch(10L, 1L);
    }

    @Test
    void listPendingOrdersDeduplicatesByOrder() {
        when(orderRepository.findByFilters(OrderStatus.SUBMITTED.code(), null)).thenReturn(List.of(order(1L, "SUBMITTED"), order(2L, "SUBMITTED")));
        when(orderRepository.findByFilters(OrderStatus.PENDING_PROCESS.code(), null))
                .thenReturn(List.of(order(2L, "PENDING_PROCESS"), order(3L, "PENDING_PROCESS")));

        List<RepairOrder> result = service.listPendingOrders();

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getOrderId());
        assertEquals(2L, result.get(1).getOrderId());
        assertEquals(3L, result.get(2).getOrderId());
    }

    private RepairOrder order(Long orderId, String status) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setStatus(status);
        return order;
    }

    private SysUser maintainer(Long userId, boolean enabled, String role) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(enabled ? 1 : 0);
        return user;
    }

    private SysRole role(String code) {
        SysRole role = new SysRole();
        role.setRoleCode(code);
        return role;
    }
}
